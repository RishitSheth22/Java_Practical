// Shared helpers for both pages
const $ = (s, r = document) => r.querySelector(s);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = (n) => '₹' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const todayStr = (add = 0) => { const d = new Date(); d.setDate(d.getDate() + add);
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); };

let toastTimer;
function toast(msg, kind = 'ok') {
  const t = $('#toast');
  t.textContent = msg; t.className = kind; t.style.display = 'block';
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.style.display = 'none', 5000);
}

// api(): throws Error(message) on failure. A 401 means the session expired.
async function api(path, method = 'GET', body) {
  const res = await fetch(path, {
    method, headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (!res.ok) {
    const err = new Error((data && data.error) || ('Request failed (' + res.status + ')'));
    err.status = res.status; throw err;
  }
  return data;
}

function table(cols, rows, empty = 'Nothing here yet.') {
  if (!rows || !rows.length) return '<div class="empty">' + esc(empty) + '</div>';
  return '<div class="table-wrap"><table><thead><tr>' + cols.map(c => '<th>' + esc(c.h) + '</th>').join('') +
    '</tr></thead><tbody>' + rows.map(r => '<tr>' + cols.map(c => '<td>' + c.f(r) + '</td>').join('') + '</tr>').join('') +
    '</tbody></table></div>';
}
const pill = (s) => '<span class="pill ' + esc(s) + '">' + esc(s) + '</span>';
