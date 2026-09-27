// Small DOM toolkit + shared visual components. Text is always inserted as text nodes;
// only the trusted icon SVG strings below go through innerHTML.

export function h(tag, attrs, ...children) {
  const el = document.createElement(tag);
  if (attrs) {
    for (const [k, v] of Object.entries(attrs)) {
      if (v == null || v === false) continue;
      if (k === 'class') el.className = v;
      else if (k === 'style') {
        if (typeof v === 'string') el.setAttribute('style', v);
        else for (const [sk, sv] of Object.entries(v)) {
          if (sv == null) continue;
          if (sk.startsWith('--')) el.style.setProperty(sk, sv);
          else el.style[sk] = sv;
        }
      } else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
      else if (k === 'dataset') Object.assign(el.dataset, v);
      else if (k === 'svg') el.innerHTML = v;
      else if (k === 'value' && 'value' in el) el.value = v;
      else if (v === true) el.setAttribute(k, '');
      else el.setAttribute(k, String(v));
    }
  }
  appendChildren(el, children);
  return el;
}

function appendChildren(el, children) {
  for (const c of children.flat(Infinity)) {
    if (c == null || c === false || c === true) continue;
    el.append(c instanceof Node ? c : String(c));
  }
}

// ---------- icons (paths from the Cohabit mock) ----------
const ICONS = {
  logo: '<circle cx="7.4" cy="10" r="4.6"/><circle cx="12.6" cy="10" r="4.6"/>',
  home: '<path d="M3.5 9.5 10 4.2l6.5 5.3" stroke-linecap="round" stroke-linejoin="round"/><path d="M5.6 9v7h8.8V9" stroke-linecap="round" stroke-linejoin="round"/>',
  list: '<path d="M4 6h12M4 10h12M4 14h7" stroke-linecap="round"/>',
  circles: '<circle cx="7.5" cy="10" r="4.5"/><circle cx="12.5" cy="10" r="4.5"/>',
  moon: '<path d="M15.5 11.6A6.2 6.2 0 0 1 8.4 4.5a6.2 6.2 0 1 0 7.1 7.1z" stroke-linejoin="round"/>',
  bell: '<path d="M6 8.5a4 4 0 0 1 8 0c0 3.2 1.2 4.5 1.2 4.5H4.8S6 11.7 6 8.5z" stroke-linejoin="round"/><path d="M8.6 15.5a1.6 1.6 0 0 0 2.8 0" stroke-linecap="round"/>',
  plus: '<path d="M10 4.5v11M4.5 10h11" stroke-linecap="round"/>',
  check: '<path d="M4.8 10.4 8.3 13.8 15.2 6.4" stroke-linecap="round" stroke-linejoin="round"/>',
  copy: '<rect x="7" y="7" width="9" height="9" rx="2.2"/><path d="M13 7V5.2A1.2 1.2 0 0 0 11.8 4H5.2A1.2 1.2 0 0 0 4 5.2v6.6A1.2 1.2 0 0 0 5.2 13H7" stroke-linecap="round"/>',
  close: '<path d="M5.5 5.5l9 9M14.5 5.5l-9 9" stroke-linecap="round"/>',
  menu: '<path d="M3.5 6h13M3.5 10h13M3.5 14h13" stroke-linecap="round"/>',
  info: '<circle cx="10" cy="10" r="7"/><path d="M10 9v4.2M10 6.6v.2" stroke-linecap="round"/>',
  user: '<circle cx="10" cy="7.2" r="3.2"/><path d="M4 16.2c.9-2.9 3.2-4.3 6-4.3s5.1 1.4 6 4.3" stroke-linecap="round"/>',
};

export function icon(name, size = 18, strokeWidth = 1.8) {
  return h('span', {
    class: 'ico', 'aria-hidden': 'true', style: 'display:inline-flex;flex-shrink:0',
    svg: `<svg width="${size}" height="${size}" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="${strokeWidth}">${ICONS[name] || ''}</svg>`,
  });
}

// ---------- colour families ----------
export const FAMILIES = ['sky', 'mint', 'lilac', 'plum', 'coral'];

function hash(str) {
  let x = 2166136261;
  for (let i = 0; i < str.length; i++) {
    x ^= str.charCodeAt(i);
    x = Math.imul(x, 16777619);
  }
  return x >>> 0;
}

/** Stable colour family for an id (circle, member). */
export function famFor(id) {
  return FAMILIES[hash(String(id || '')) % FAMILIES.length];
}

export function famClass(fam) {
  return `fam-${fam}`;
}

export function initials(name) {
  const parts = String(name || '?').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function avatar(name, id, size = '') {
  return h('span', { class: `avatar ${size} ${famClass(famFor(id || name))}`, title: name }, initials(name));
}

// ---------- schedule marks ----------
/** states: 'full' | 'none' | 'soft' | 'off' | 'future' */
export function daySquares(states, labels) {
  return h('div', { class: 'marks', role: 'img', 'aria-label': labels },
    states.map((s) => h('span', { class: `sq ${s === 'none' ? '' : s}` })));
}

export function capsules(done, target, label) {
  const caps = [];
  for (let i = 0; i < target; i++) caps.push(h('span', { class: `cap ${i < done ? 'full' : ''}` }));
  return h('div', { class: 'marks marks-caps', role: 'img', 'aria-label': label || `${done} of ${target} this week` }, caps);
}

export function pips(done, target, small) {
  const out = [];
  for (let i = 0; i < Math.max(target, 0); i++) out.push(h('span', { class: `pip ${i < done ? 'full' : ''}` }));
  return h('div', { class: `pips ${small ? 'small' : ''}`, role: 'img', 'aria-label': `${done} of ${target}` }, out);
}

export function bar(fraction, extraClass = '', fillClass = '') {
  const pct = Math.max(0, Math.min(1, fraction || 0)) * 100;
  return h('div', { class: `bar ${extraClass}` }, h('span', { class: fillClass, style: { width: `${pct.toFixed(1)}%` } }));
}

export function chip(text, cls = '') {
  return h('span', { class: `chip ${cls}` }, text);
}

export function emptyState({ title, text, action, small }) {
  return h('div', { class: `empty ${small ? 'small' : ''}` },
    title && h('h3', { class: 'h3' }, title),
    text && h('p', null, text),
    action);
}

export function loadingState(text = 'Loading…') {
  return h('div', { class: 'loading', role: 'status' }, text);
}

export function inlineError(message) {
  return h('div', { class: 'inline-error', role: 'alert' }, message);
}

// ---------- toasts ----------
export function toast(message, kind = 'info') {
  const host = document.getElementById('toasts');
  if (!host) return;
  const el = h('div', { class: `toast ${kind}`, role: kind === 'error' ? 'alert' : 'status' }, message);
  host.append(el);
  setTimeout(() => {
    el.style.transition = 'opacity .25s';
    el.style.opacity = '0';
    setTimeout(() => el.remove(), 260);
  }, kind === 'error' ? 5200 : 3400);
}

export function toastError(err) {
  toast(err && err.message ? err.message : 'Something went wrong.', 'error');
}

// ---------- modal ----------
/**
 * openModal({ title, sub, content, submitLabel, onSubmit, danger, cancelLabel, fam })
 * onSubmit(form) may throw (message shown inline) or return false to keep the dialog open.
 */
export function openModal({ title, sub, content, submitLabel, onSubmit, danger, cancelLabel = 'Cancel', fam, onClose }) {
  const errorEl = h('div', { class: 'inline-error form-error', role: 'alert', hidden: true });
  const submitBtn = submitLabel
    ? h('button', { type: 'submit', class: `btn ${danger ? 'btn-danger' : 'btn-primary'}` }, submitLabel)
    : null;
  let dlg;
  const close = () => dlg.close();
  const form = h('form', { class: 'modal-form', novalidate: true },
    h('div', { class: 'modal-head' },
      h('h2', { class: 'h2' }, title),
      h('button', { type: 'button', class: 'icon-btn icon-btn-sm', 'aria-label': 'Close', onclick: close }, icon('close', 16))),
    sub && h('p', { class: 'modal-sub' }, sub),
    content && h('div', { class: 'modal-content' }, content),
    errorEl,
    h('div', { class: 'modal-actions' },
      h('button', { type: 'button', class: 'btn btn-secondary', onclick: close }, submitBtn ? cancelLabel : 'Close'),
      submitBtn));
  dlg = h('dialog', { class: `modal ${fam ? famClass(fam) : ''}`, 'aria-label': title }, form);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!onSubmit || !submitBtn) return close();
    errorEl.hidden = true;
    submitBtn.disabled = true;
    try {
      const result = await onSubmit(form);
      if (result !== false) close();
    } catch (err) {
      errorEl.textContent = err && err.message ? err.message : 'Something went wrong.';
      errorEl.hidden = false;
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });
  dlg.addEventListener('close', () => {
    dlg.remove();
    if (onClose) onClose();
  });
  document.body.append(dlg);
  dlg.showModal();
  const first = form.querySelector('.modal-content input:not([type=radio]):not([type=hidden]), .modal-content select');
  if (first) first.focus();
  return { close, dialog: dlg };
}

export function confirmDialog({ title, text, confirmLabel = 'Confirm', danger = true }) {
  return new Promise((resolve) => {
    let ok = false;
    openModal({
      title, sub: text, submitLabel: confirmLabel, danger,
      onSubmit: () => { ok = true; },
      onClose: () => resolve(ok),
    });
  });
}

export function field(labelText, control, hint) {
  const id = control.id || `f-${Math.random().toString(36).slice(2, 9)}`;
  control.id = id;
  return h('div', { class: 'field' },
    h('label', { for: id }, labelText),
    control,
    hint && h('div', { class: 'field-hint' }, hint));
}

export async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const ta = h('textarea', { style: 'position:fixed;opacity:0' });
    ta.value = text;
    document.body.append(ta);
    ta.select();
    let ok = false;
    try { ok = document.execCommand('copy'); } catch { ok = false; }
    ta.remove();
    return ok;
  }
}
