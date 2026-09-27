// Light / Dark / System theme, persisted in localStorage. The resolved theme is written to
// <html data-theme>, which the CSS tokens key off.
import { h } from './ui.js';

const KEY = 'cohabit.theme';
const media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
const listeners = new Set();

export function themePref() {
  try {
    const v = localStorage.getItem(KEY);
    return v === 'light' || v === 'dark' ? v : 'system';
  } catch {
    return 'system';
  }
}

export function applyTheme() {
  const pref = themePref();
  const dark = pref === 'dark' || (pref === 'system' && media && media.matches);
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  listeners.forEach((fn) => fn(pref));
}

export function setTheme(pref) {
  try {
    if (pref === 'system') localStorage.removeItem(KEY);
    else localStorage.setItem(KEY, pref);
  } catch { /* per-viewer convenience only */ }
  applyTheme();
}

if (media) {
  const onChange = () => { if (themePref() === 'system') applyTheme(); };
  if (media.addEventListener) media.addEventListener('change', onChange);
  else if (media.addListener) media.addListener(onChange);
}

/** Segmented Light / Dark / System control (from the Dark mode board). */
export function themeSwitch() {
  const opts = [['light', 'Light'], ['dark', 'Dark'], ['system', 'System']];
  const buttons = opts.map(([value, label]) =>
    h('button', { type: 'button', 'aria-pressed': String(themePref() === value), onclick: () => setTheme(value) }, label));
  const el = h('div', { class: 'seg', role: 'group', 'aria-label': 'Appearance' }, buttons);
  const sync = (pref) => {
    // Drop the listener once the control has left the DOM (views re-render often).
    if (!el.isConnected) { listeners.delete(sync); return; }
    buttons.forEach((b, i) => b.setAttribute('aria-pressed', String(opts[i][0] === pref)));
  };
  listeners.add(sync);
  return el;
}
