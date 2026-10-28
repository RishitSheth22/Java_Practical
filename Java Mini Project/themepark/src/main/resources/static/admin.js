let staff = null;
let rides = [];
let editingRide = null;      // ride id being edited, or null when adding
let slotRide = null;         // ride whose slots are being managed
const TABS = ['dash', 'rides', 'log', 'bookings'];

function show(tab) {
  TABS.forEach(t => $('#tab-' + t).classList.toggle('hidden', t !== tab || !staff));
  $('#tab-auth').classList.toggle('hidden', !!staff);
  $('#nav').classList.toggle('hidden', !staff);
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  if (!staff) return;
  if (tab === 'dash') loadDash();
  if (tab === 'rides') loadRides();
  if (tab === 'log') loadLog();
  if (tab === 'bookings') loadBookings();
}
function handle(e) {
  if (e.status === 401) { staff = null; renderWho(); toast('Your session expired. Please log in again.', 'err'); show('dash'); }
  else toast(e.message, 'err');
}
function renderWho() {
  $('#who').textContent = staff ? staff : '';
  $('#logoutBtn').classList.toggle('hidden', !staff);
}
document.querySelectorAll('#nav button').forEach(b => b.onclick = () => show(b.dataset.tab));

$('#loginForm').onsubmit = async (ev) => {
  ev.preventDefault();
  try {
    const s = await api('/api/auth/staff-login', 'POST', Object.fromEntries(new FormData(ev.target).entries()));
    staff = s.name + ' (' + s.role + ')'; renderWho(); ev.target.reset(); show('dash');
  } catch (e) { toast(e.message, 'err'); }
};
$('#logoutBtn').onclick = async () => { await api('/api/auth/logout', 'POST'); staff = null; renderWho(); show('dash'); };

// ------------------------------------------------------------- dashboard
async function loadDash() {
  try {
    const d = await api('/api/admin/dashboard');
    $('#stats').innerHTML = [
      [money(d.revenueToday), "Today's net revenue"], [d.upcomingBookings, 'Upcoming bookings'],
      [d.bookingsNeedingReview, 'Bookings needing review'],
      [d.underMaintenance.length, 'Rides in maintenance'], [d.closedRides.length, 'Rides closed']
    ].map(x => '<div class="stat"><b>' + x[0] + '</b><span>' + x[1] + '</span></div>').join('');
    $('#popular').innerHTML = table([{ h: 'Ride', f: r => esc(r.rideName) }, { h: 'Bookings', f: r => r.bookings }, { h: 'Seats', f: r => r.seats }], d.popularRides, 'No bookings yet.');
    $('#ratings').innerHTML = table([{ h: 'Ride', f: r => esc(r.rideName) }, { h: 'Avg', f: r => r.avgRating }, { h: 'Reviews', f: r => r.reviews }], d.averageRatings, 'No ratings yet.');
    $('#overbooked').innerHTML = table([{ h: 'Ride', f: r => esc(r.rideName) }, { h: 'Date', f: r => esc(r.slotDate) }, { h: 'Time', f: r => esc(r.startTime) },
      { h: 'Capacity', f: r => r.capacity }, { h: 'Booked', f: r => r.booked }], d.overbookedSlots, 'No overbooked slots.');
  } catch (e) { handle(e); }
}

// ----------------------------------------------------------------- rides
async function loadRides() {
  try {
    rides = await api('/api/admin/rides');
    $('#rideTable').innerHTML = table([
      { h: 'Ride', f: r => '<b>' + esc(r.rideName) + '</b><br><span class="muted">' + esc(r.location || '') + '</span>' },
      { h: 'Type', f: r => esc(r.type || '') }, { h: 'Capacity', f: r => r.capacity }, { h: 'Price', f: r => money(r.price) },
      { h: 'Status', f: r => pill(r.status) },
      { h: 'Set status', f: r => ['OPEN', 'CLOSED', 'MAINTENANCE'].filter(s => s !== r.status)
          .map(s => '<button class="btn ghost small" data-status="' + s + '" data-id="' + r.rideId + '">' + s.charAt(0) + s.slice(1).toLowerCase() + '</button>').join(' ') },
      { h: '', f: r => '<button class="btn ghost small" data-edit="' + r.rideId + '">Edit</button> <button class="btn ghost small" data-slots="' + r.rideId +
          '">Slots</button> <button class="btn danger small" data-archive="' + r.rideId + '">Archive</button>' }
    ], rides, 'No rides yet.');
    document.querySelectorAll('[data-status]').forEach(b => b.onclick = () => setStatus(b, parseInt(b.dataset.id, 10), b.dataset.status));
    document.querySelectorAll('[data-edit]').forEach(b => b.onclick = () => openRideForm(parseInt(b.dataset.edit, 10)));
    document.querySelectorAll('[data-slots]').forEach(b => b.onclick = () => openSlots(parseInt(b.dataset.slots, 10)));
    document.querySelectorAll('[data-archive]').forEach(b => b.onclick = () => archiveRide(parseInt(b.dataset.archive, 10)));
    if (slotRide) loadSlots();
  } catch (e) { handle(e); }
}

async function setStatus(button, id, status) {
  button.disabled = true;
  try {
    const r = await api('/api/admin/rides/' + id + '/status', 'POST', { status });
    toast('Ride is now ' + status + (r.bookingsFlagged ? ' – ' + r.bookingsFlagged + ' booking(s) flagged for review/refund.' : '.'));
    await loadRides();
  } catch (e) { handle(e); }
  finally { button.disabled = false; }
}

async function archiveRide(id) {
  if (!confirm('Archive this ride? It disappears from the park (history is kept).')) return;
  try { await api('/api/admin/rides/' + id, 'DELETE'); toast('Ride archived.'); if (slotRide === id) { slotRide = null; $('#slotCard').classList.add('hidden'); } loadRides(); }
  catch (e) { handle(e); }
}

async function openRideForm(id) {
  editingRide = id;
  const locs = await api('/api/admin/locations');
  $('#rfLoc').innerHTML = '<option value="">–</option>' + locs.map(l => '<option value="' + l.locationId + '">' + esc(l.locationName) + '</option>').join('');
  const r = id ? rides.find(x => x.rideId === id) : { rideName: '', type: '', capacity: 20, price: 0, locationId: '' };
  $('#rideFormTitle').textContent = id ? 'Edit ride' : 'Add ride (starts CLOSED – open it when ready)';
  $('#rfName').value = r.rideName; $('#rfType').value = r.type || ''; $('#rfCap').value = r.capacity;
  $('#rfPrice').value = r.price; $('#rfLoc').value = r.locationId || '';
  $('#rideForm').classList.remove('hidden');
  $('#rideForm').scrollIntoView({ behavior: 'smooth' });
}
$('#newRideBtn').onclick = () => openRideForm(null);
$('#rfCancel').onclick = () => $('#rideForm').classList.add('hidden');
$('#rfSave').onclick = async () => {
  const body = { rideName: $('#rfName').value, type: $('#rfType').value, capacity: parseInt($('#rfCap').value, 10),
    price: parseFloat($('#rfPrice').value || '0'), locationId: $('#rfLoc').value ? parseInt($('#rfLoc').value, 10) : null };
  if (isNaN(body.capacity)) return toast('Enter a capacity.', 'err');
  try {
    if (editingRide) await api('/api/admin/rides/' + editingRide, 'PUT', body); else await api('/api/admin/rides', 'POST', body);
    toast('Ride saved.'); $('#rideForm').classList.add('hidden'); loadRides();
  } catch (e) { handle(e); }
};

// ----------------------------------------------------------------- slots
function openSlots(id) {
  slotRide = id; const r = rides.find(x => x.rideId === id);
  $('#slotTitle').textContent = 'Slots – ' + r.rideName;
  $('#slDate').value = todayStr(); $('#slDate').min = todayStr();
  $('#slotCard').classList.remove('hidden'); loadSlots();
  $('#slotCard').scrollIntoView({ behavior: 'smooth' });
}
async function loadSlots() {
  try {
    const rows = await api('/api/admin/rides/' + slotRide + '/slots');
    $('#slotTable').innerHTML = table([
      { h: 'Date', f: r => esc(r.slotDate) }, { h: 'Time', f: r => esc(r.startTime) + '–' + esc(r.endTime) },
      { h: 'Capacity', f: r => r.capacity }, { h: 'Booked', f: r => r.booked }, { h: 'Left', f: r => r.available },
      { h: '', f: r => '<button class="btn ghost small" data-slcap="' + r.slotId + '" data-start="' + r.startTime + '" data-end="' + r.endTime + '" data-cap="' + r.capacity +
          '">Change capacity</button> <button class="btn danger small" data-sldel="' + r.slotId + '">Remove</button>' }
    ], rows, 'No upcoming slots.');
    document.querySelectorAll('[data-slcap]').forEach(b => b.onclick = async () => {
      const v = prompt('New capacity for this slot:', b.dataset.cap); if (v === null) return;
      try { await api('/api/admin/slots/' + b.dataset.slcap, 'PUT', { startTime: b.dataset.start, endTime: b.dataset.end, capacity: parseInt(v, 10) }); toast('Slot updated.'); loadSlots(); }
      catch (e) { handle(e); }
    });
    document.querySelectorAll('[data-sldel]').forEach(b => b.onclick = async () => {
      if (!confirm('Remove this slot?')) return;
      try { await api('/api/admin/slots/' + b.dataset.sldel, 'DELETE'); toast('Slot removed.'); loadSlots(); } catch (e) { handle(e); }
    });
  } catch (e) { handle(e); }
}
$('#slAdd').onclick = async () => {
  try {
    await api('/api/admin/rides/' + slotRide + '/slots', 'POST', { slotDate: $('#slDate').value, startTime: $('#slStart').value, endTime: $('#slEnd').value,
      capacity: $('#slCap').value ? parseInt($('#slCap').value, 10) : null });
    toast('Slot added.'); loadSlots();
  } catch (e) { handle(e); }
};

// -------------------------------------------------------------- ride log
async function loadLog() {
  try {
    if (!rides.length) rides = await api('/api/admin/rides');
    const cur = $('#logRide').value;
    $('#logRide').innerHTML = '<option value="">All rides</option>' + rides.map(r => '<option value="' + r.rideId + '">' + esc(r.rideName) + '</option>').join('');
    $('#logRide').value = cur;
    const rows = await api('/api/admin/log' + ($('#logRide').value ? '?rideId=' + $('#logRide').value : ''));
    $('#logTable').innerHTML = table([
      { h: 'Time', f: r => esc(r.logTime) }, { h: 'Operation', f: r => pill(r.operation).replace('class="pill ', 'class="pill ' + (['OPENED', 'RESUMED'].includes(r.operation) ? 'OPEN ' : '')) },
      { h: 'Ride', f: r => esc(r.rideName) }, { h: 'Staff', f: r => esc(r.staffName) }, { h: 'Details', f: r => '<span class="muted">' + esc(r.details || '') + '</span>' }
    ], rows, 'No log entries yet.');
  } catch (e) { handle(e); }
}
$('#logRide').onchange = loadLog;
$('#logAdd').onclick = async () => {
  const id = $('#logRide').value;
  if (!id) return toast('Select a ride first.', 'err');
  try {
    await api('/api/admin/rides/' + id + '/log', 'POST', { operation: $('#logOp').value, details: $('#logDetails').value });
    $('#logOp').value = ''; $('#logDetails').value = ''; toast('Log entry added.'); loadLog();
  } catch (e) { handle(e); }
};

// -------------------------------------------------------------- bookings
async function loadBookings() {
  try {
    const rows = await api('/api/admin/bookings');
    $('#bookingTable').innerHTML = table([
      { h: '#', f: r => r.bookingId }, { h: 'Visitor', f: r => esc(r.visitorName) }, { h: 'Ride', f: r => esc(r.rideName) },
      { h: 'Date', f: r => esc(r.slotDate) }, { h: 'Time', f: r => esc(r.startTime) }, { h: 'Seats', f: r => r.seats },
      { h: 'Amount', f: r => money(r.amount) }, { h: 'Status', f: r => pill(r.status) },
      { h: '', f: r => r.status !== 'CANCELLED' ? '<button class="btn danger small" data-cancel="' + r.bookingId + '">Cancel &amp; refund</button>' : '' }
    ], rows, 'No bookings yet.');
    document.querySelectorAll('[data-cancel]').forEach(b => b.onclick = async () => {
      if (!confirm('Cancel this booking and record a refund?')) return;
      try { await api('/api/admin/bookings/' + b.dataset.cancel + '/cancel', 'POST'); toast('Booking cancelled and refund recorded.'); loadBookings(); }
      catch (e) { handle(e); }
    });
  } catch (e) { handle(e); }
}

// ------------------------------------------------------------------ init
(async function init() {
  try { const m = await api('/api/auth/me'); staff = m.staff; } catch (e) { /* ignore */ }
  renderWho(); show('dash');
})();
