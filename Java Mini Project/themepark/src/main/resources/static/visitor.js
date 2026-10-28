let me = null;                  // logged-in visitor name or null
let currentTab = 'avail';
let ticketTypes = [];
let avail = [];                 // availability for the booking wizard
let pick = { slot: null, ride: null };

const SECTIONS = ['avail', 'book', 'bookings', 'tickets', 'feedback'];
const NEEDS_LOGIN = ['book', 'bookings', 'tickets', 'feedback'];

function show(tab) {
  currentTab = tab;
  const needLogin = NEEDS_LOGIN.includes(tab) && !me;
  SECTIONS.forEach(t => $('#tab-' + t).classList.toggle('hidden', t !== tab || needLogin));
  $('#tab-auth').classList.toggle('hidden', !needLogin);
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  if (needLogin) return;
  if (tab === 'avail') loadAvail();
  if (tab === 'book') initBooking();
  if (tab === 'bookings') loadBookings();
  if (tab === 'tickets') loadTickets();
  if (tab === 'feedback') loadFeedbackRides();
}

// Any 401 (session timeout) discards in-progress work and sends the visitor to login.
function handle(e) {
  if (e.status === 401) {
    me = null; renderWho();
    resetBooking();
    toast('Your session expired. Please log in again.', 'err');
    show('book');
  } else toast(e.message, 'err');
}

function renderWho() {
  $('#who').textContent = me ? 'Hi, ' + me : '';
  $('#logoutBtn').classList.toggle('hidden', !me);
}

// ------------------------------------------------------------------ auth
$('#showLogin').onclick = () => { $('#loginForm').classList.remove('hidden'); $('#registerForm').classList.add('hidden'); };
$('#showRegister').onclick = () => { $('#registerForm').classList.remove('hidden'); $('#loginForm').classList.add('hidden'); };

async function submitAuth(form, url) {
  const body = Object.fromEntries(new FormData(form).entries());
  if (!body.dob) delete body.dob;
  try {
    const v = await api(url, 'POST', body);
    me = v.name; renderWho(); toast('Welcome, ' + v.name + '!'); form.reset(); show(currentTab);
  } catch (e) { toast(e.message, 'err'); }
}
$('#loginForm').onsubmit = (ev) => { ev.preventDefault(); submitAuth(ev.target, '/api/auth/login'); };
$('#registerForm').onsubmit = (ev) => { ev.preventDefault(); submitAuth(ev.target, '/api/auth/register'); };
$('#logoutBtn').onclick = async () => { await api('/api/auth/logout', 'POST'); me = null; renderWho(); resetBooking(); show('avail'); };
document.querySelectorAll('#nav button').forEach(b => b.onclick = () => show(b.dataset.tab));

// ---------------------------------------------------------- availability
$('#availDate').value = todayStr(); $('#availDate').min = todayStr();
$('#availDate').onchange = loadAvail;

function slotCard(ride, s, selectable, qty) {
  const pct = s.capacity ? Math.min(100, Math.round(100 * s.booked / s.capacity)) : 0;
  const open = ride.status === 'OPEN';
  const ok = open && s.available >= (qty || 1);
  const label = !open ? ride.status : (s.available === 0 ? 'FULL' : s.available + ' of ' + s.capacity + ' seats left');
  const attrs = selectable ? (ok ? '' : 'disabled') : 'disabled';
  return '<button class="slot" ' + attrs + ' data-slot="' + s.slotId + '" data-ride="' + ride.rideId + '"><b>' + esc(s.startTime) +
    ' – ' + esc(s.endTime) + '</b><small>' + esc(label) + '</small><div class="bar"><i style="width:' + pct + '%"></i></div></button>';
}

async function loadAvail() {
  try {
    const list = await api('/api/availability?date=' + $('#availDate').value);
    $('#availList').innerHTML = list.map(r => '<div class="card"><div class="row" style="align-items:center"><div><h2 style="margin:0">' +
      esc(r.rideName) + '</h2><span class="muted">' + esc(r.type || '') + ' · ' + esc(r.location || '') + ' · ' + money(r.price) +
      ' per seat</span></div><div style="text-align:right">' + pill(r.status) + '</div></div><div class="slots">' +
      (r.slots.length ? r.slots.map(s => slotCard(r, s, false)).join('') : '<span class="muted">No slots on this date.</span>') +
      '</div></div>').join('') || '<div class="empty">No rides.</div>';
  } catch (e) { handle(e); }
}

// --------------------------------------------------------- booking wizard
function resetBooking() { pick = { slot: null, ride: null }; avail = []; $('#bkRides').innerHTML = ''; $('#bkPay').classList.add('hidden'); }

async function initBooking() {
  $('#bkDone').classList.add('hidden');
  $('#bkDate').min = todayStr(); $('#bkDate').max = todayStr(90);
  if (!$('#bkDate').value) $('#bkDate').value = todayStr();
  if (!ticketTypes.length) {
    ticketTypes = await api('/api/ticket-types');
    $('#bkType').innerHTML = ticketTypes.map(t => '<option value="' + t.typeId + '">' + esc(t.typeName) +
      (t.description ? ' (' + esc(t.description) + ')' : '') +
      ' · entry ' + money(t.price) + ' · ' + Math.round(Number(t.ridePriceMultiplier) * 100) + '% ride fee</option>').join('');
  }
}

$('#bkShow').onclick = async () => {
  const qty = parseInt($('#bkQty').value, 10);
  if (!(qty >= 1 && qty <= 10)) return toast('Quantity must be between 1 and 10.', 'err');
  if (!$('#bkDate').value || $('#bkDate').value < todayStr()) return toast('Choose today or a future date.', 'err');
  pick = { slot: null, ride: null };
  try {
    avail = await api('/api/availability?date=' + $('#bkDate').value);
    renderWizardRides(qty);
    $('#bkPay').classList.remove('hidden'); updateSummary();
  } catch (e) { handle(e); }
};

function renderWizardRides(qty) {
  const type = selectedTicketType();
  $('#bkRides').innerHTML = '<div class="card"><h2>Choose an optional ride and time slot</h2>' +
    '<p class="muted">Combined price includes the selected ticket and the ticket-type-adjusted ride fee.</p>' +
    '<button class="btn ghost small" id="noRide">Park entry only</button>' +
    avail.map(r => {
      const rideFee = ridePriceForTicket(r, type);
      return '<div style="margin-top:14px"><b>' + esc(r.rideName) + '</b> ' + pill(r.status) +
        ' <span class="muted">' + esc(r.type || '') + ' · ride fee ' + money(rideFee) +
        ' · combined price ' + money(Number(type.price) + rideFee) + ' per person</span><div class="slots">' +
        (r.slots.length ? r.slots.map(s => slotCard(r, s, true, qty)).join('') : '<span class="muted">No slots</span>') +
      '</div></div>';
    }).join('');
  document.querySelectorAll('#bkRides .slot').forEach(b => b.onclick = () => {
    document.querySelectorAll('#bkRides .slot').forEach(x => x.classList.remove('sel'));
    b.classList.add('sel');
    pick.slot = parseInt(b.dataset.slot, 10);
    pick.ride = avail.find(r => r.rideId === parseInt(b.dataset.ride, 10));
    updateSummary();
  });
  $('#noRide').onclick = () => { pick = { slot: null, ride: null }; document.querySelectorAll('#bkRides .slot').forEach(x => x.classList.remove('sel')); updateSummary(); };
}

function selectedTicketType() {
  return ticketTypes.find(x => x.typeId === parseInt($('#bkType').value, 10));
}
function ridePriceForTicket(ride, type) {
  return Math.round(Number(ride.price || 0) * Number(type?.ridePriceMultiplier || 1) * 100) / 100;
}
function wizardTotal() {
  const t = selectedTicketType();
  const qty = parseInt($('#bkQty').value, 10) || 0;
  return t ? (Number(t.price) + (pick.ride ? ridePriceForTicket(pick.ride, t) : 0)) * qty : 0;
}
function updateSummary() {
  const type = selectedTicketType();
  const qty = parseInt($('#bkQty').value, 10) || 0;
  const entryPrice = type ? Number(type.price) : 0;
  const rideFee = pick.ride && type ? ridePriceForTicket(pick.ride, type) : 0;
  $('#bkSummary').innerHTML = qty + ' × <b>' + esc(type ? type.typeName : 'ticket') + '</b> at ' + money(entryPrice) +
    ' entry each' + (pick.ride ? ' + <b>' + esc(pick.ride.rideName) + '</b> ride fee ' + money(rideFee) +
      ' each (' + Math.round(Number(type ? type.ridePriceMultiplier : 1) * 100) + '% of the ride price)' : ' · park entry only') +
    ' · <b>Total ' + money(wizardTotal()) + '</b>';
}
['#bkType', '#bkQty', '#bkDate'].forEach(s => $(s).addEventListener('change', () => { $('#bkPay').classList.add('hidden'); $('#bkRides').innerHTML = ''; pick = { slot: null, ride: null }; }));

$('#bkConfirm').onclick = async () => {
  const btn = $('#bkConfirm'); btn.disabled = true;
  try {
    const r = await api('/api/visitor/purchase', 'POST', {
      ticketTypeId: parseInt($('#bkType').value, 10), quantity: parseInt($('#bkQty').value, 10),
      visitDate: $('#bkDate').value, slotId: pick.slot, paymentMethod: $('#bkMode').value,
      simulateFailure: $('#bkFail').checked
    });
    $('#bkRides').innerHTML = ''; $('#bkPay').classList.add('hidden');
    $('#bkDone').classList.remove('hidden');
    $('#bkDone').innerHTML = '<h2>✅ Booking confirmed</h2><p>Transaction <b>#' + r.transactionId + '</b> · Purchase <b>#' + r.purchaseId +
      '</b> · Tickets: ' + r.ticketIds.map(i => '#' + i).join(', ') + '<br>Valid on <b>' + esc(r.visitDate) + '</b> · Total paid <b>' + money(r.totalAmount) + '</b></p>' +
      (r.bookingId ? '<p>Ride: <b>' + esc(r.rideName) + '</b> at ' + esc(r.slot) + ' (booking #' + r.bookingId + '). Seats left in that slot: <b>' + r.remainingSeats + '</b></p>' : '');
    pick = { slot: null, ride: null };
    toast('Booking confirmed!');
  } catch (e) { handle(e); if (e.status === 409) $('#bkShow').click(); } finally { btn.disabled = false; }
};

// -------------------------------------------------------------- my pages
async function loadBookings() {
  try {
    const rows = await api('/api/visitor/bookings');
    $('#myBookings').innerHTML = table([
      { h: '#', f: r => r.bookingId }, { h: 'Ride', f: r => esc(r.rideName) },
      { h: 'Date', f: r => esc(r.slotDate) }, { h: 'Time', f: r => esc(r.startTime) + '–' + esc(r.endTime) },
      { h: 'Seats', f: r => r.seats }, { h: 'Paid', f: r => money(r.amount) }, { h: 'Status', f: r => pill(r.status) },
      { h: '', f: r => r.status !== 'CANCELLED' ? '<button class="btn danger small" data-cancel="' + r.bookingId + '">Cancel</button>' : '' }
    ], rows, 'No ride bookings yet.');
    document.querySelectorAll('[data-cancel]').forEach(b => b.onclick = async () => {
      if (!confirm('Cancel this booking? A refund will be recorded.')) return;
      try { const r = await api('/api/visitor/bookings/' + b.dataset.cancel + '/cancel', 'POST'); toast('Cancelled. Refund of ' + money(r.refundAmount) + ' recorded.'); loadBookings(); }
      catch (e) { handle(e); }
    });
    const ps = await api('/api/visitor/purchases');
    $('#myPurchases').innerHTML = table([
      { h: 'Purchase', f: r => '#' + r.purchaseId }, { h: 'Date', f: r => esc(r.purchaseDate) },
      { h: 'Tickets', f: r => r.tickets }, { h: 'Payment', f: r => esc(r.paymentMode) },
      { h: 'Total', f: r => money(r.totalAmount) }, { h: 'Transaction', f: r => '#' + r.transactionId }
    ], ps, 'No purchases yet.');
  } catch (e) { handle(e); }
}

async function loadTickets() {
  try {
    const rows = await api('/api/visitor/tickets');
    $('#myTickets').innerHTML = table([
      { h: 'Ticket', f: r => '#' + r.ticketId }, { h: 'Type', f: r => esc(r.ticketType) },
      { h: 'Entry price', f: r => money(r.price) }, { h: 'Issued', f: r => esc(r.issueDate) },
      { h: 'Valid until', f: r => esc(r.validTo) }, { h: 'Status', f: r => pill(r.validity) }
    ], rows, 'You have no tickets yet.');
  } catch (e) { handle(e); }
}

async function loadFeedbackRides() {
  try {
    const rides = await api('/api/rides');
    $('#fbRide').innerHTML = '<option value="">General park feedback</option>' + rides.map(r => '<option value="' + r.rideId + '">' + esc(r.rideName) + '</option>').join('');
  } catch (e) { handle(e); }
}
$('#fbForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const f = Object.fromEntries(new FormData(ev.target).entries());
  try {
    await api('/api/visitor/feedback', 'POST', { rideId: f.rideId ? parseInt(f.rideId, 10) : null, rating: parseInt(f.rating, 10), text: f.text });
    toast('Thanks for your feedback!'); ev.target.reset();
  } catch (e) { handle(e); }
};

// ------------------------------------------------------------------ init
(async function init() {
  try { const m = await api('/api/auth/me'); me = m.visitor; } catch (e) { /* ignore */ }
  renderWho(); show('avail');
})();
