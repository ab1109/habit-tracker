// Entry point: auth mode (GET /auth/config), landing / sign-in, local identity setup (DEV_HEADER),
// app shell (sidebar + mobile bar), profile menu and hash router.
import { h, icon, avatar, famClass, famFor, openModal, toast, toastError, copyText, loadingState, inlineError, field } from './ui.js';
import { api } from './api.js';
import { tz } from './dates.js';
import { applyTheme } from './theme.js';
import {
  identities, currentIdentity, createIdentity, switchIdentity, clearCurrentIdentity,
  setAuthConfig, isGoogleMode, getLoginUrl, setSessionUser, savePendingJoin, takePendingJoin,
} from './identity.js';
import { openNewCircle, pageHead } from './components.js';
import { renderToday } from './views/today.js';
import { renderHabit } from './views/habit.js';
import { renderCircle } from './views/circle.js';
import { renderJoint } from './views/joint.js';
import { renderNotifications } from './views/notifications.js';
import { renderJoin } from './views/join.js';
import { renderLanding } from './views/landing.js';

const root = document.getElementById('app');
let shell = null;
let groupsCache = [];
let renderToken = 0;
let screen = 'boot'; // 'boot' | 'landing' | 'setup' | 'app'

const JOIN_RE = /^#\/join\/([^/?]+)$/;
function joinTokenFromHash() {
  const m = (location.hash || '').match(JOIN_RE);
  if (!m) return null;
  try { return decodeURIComponent(m[1]); } catch { return m[1]; }
}

// ---------- landing (GOOGLE signed-out = sign-in screen; DEV_HEADER before any identity) ----------
function showLanding() {
  shell = null;
  screen = 'landing';
  const google = isGoogleMode();
  const token = joinTokenFromHash();
  // Google sign-in comes back to '/', so remember the invite across the round trip.
  if (token && google) savePendingJoin(token);
  root.className = '';
  document.title = 'Cohabit · Habit tracking, together';
  root.replaceChildren(renderLanding({
    google,
    loginUrl: getLoginUrl(),
    invited: !!token,
    onStart: () => showSetup({ fromLanding: true }),
  }));
  window.scrollTo(0, 0);
}

function showBootError(err) {
  shell = null;
  screen = 'boot';
  root.className = '';
  root.replaceChildren(h('div', { class: 'setup' },
    h('div', { class: 'setup-card' },
      brand(),
      h('h1', { class: 'title' }, 'Can’t reach Cohabit'),
      h('div', { class: 'mt-16' }, inlineError(err && err.message ? err.message : String(err))),
      h('div', { class: 'row mt-20' }, h('button', { type: 'button', class: 'btn btn-primary', onclick: boot }, 'Try again')))));
}

// ---------- identity setup ----------
function brand(small) {
  return h('a', { class: 'brand', href: '#/', 'aria-label': 'Cohabit, go to Today' },
    h('span', { class: 'brand-mark', style: small ? 'width:28px;height:28px;border-radius:10px' : null }, icon('logo', small ? 15 : 17, 1.9)),
    h('span', { class: 'brand-name', style: small ? 'font-size:18px' : null }, 'Cohabit'));
}

function showSetup({ cancelable = false, fromLanding = false } = {}) {
  shell = null;
  screen = 'setup';
  const known = identities();
  const current = currentIdentity();
  const nameInput = h('input', { class: 'input', placeholder: 'Maya Rao', maxlength: '60', autocomplete: 'name' });
  const idInput = h('input', { class: 'input mono', placeholder: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx', autocomplete: 'off', spellcheck: 'false' });
  const errorEl = h('div', { class: 'inline-error form-error', role: 'alert', hidden: true });

  const form = h('form', { class: 'setup-card', novalidate: true },
    brand(),
    h('h1', { class: 'title' }, cancelable ? 'Add someone new' : 'Who are you?'),
    h('p', { class: 'sub' }, 'Cohabit has no accounts yet. Pick the name your circles will see and we’ll make you a private user id, kept in this browser.'),
    h('div', { class: 'mt-20' },
      field('Your display name', nameInput),
      h('details', null,
        h('summary', null, 'I already have a user id'),
        h('div', { class: 'mt-12' }, field('User id', idInput, 'Use the same id you have elsewhere, e.g. on another browser.')))),
    errorEl,
    h('div', { class: 'row mt-20' },
      h('button', { type: 'submit', class: 'btn btn-primary' }, 'Continue'),
      cancelable && current
        ? h('button', { type: 'button', class: 'btn btn-secondary', onclick: () => { buildShell(); route(true); } }, `Back to ${current.name}`)
        : fromLanding ? h('button', { type: 'button', class: 'btn btn-secondary', onclick: showLanding }, 'Back') : null),
    known.length && !cancelable
      ? h('div', { class: 'mt-20' },
        h('p', { class: 'side-label' }, 'Or continue as'),
        known.map((i) => h('button', {
          type: 'button', class: 'ident-row',
          onclick: () => { switchIdentity(i.id); enterApp(); },
        }, avatar(i.name, i.id, 'md'), h('div', { class: 'grow' }, h('div', { class: 't' }, i.name), h('div', { class: 's mono' }, i.id)))))
      : null);

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    errorEl.hidden = true;
    const name = nameInput.value.trim();
    if (!name) {
      errorEl.textContent = 'Add a display name first.';
      errorEl.hidden = false;
      nameInput.focus();
      return;
    }
    try {
      createIdentity(name, idInput.value);
    } catch (err) {
      errorEl.textContent = err.message;
      errorEl.hidden = false;
      return;
    }
    enterApp();
  });

  root.className = '';
  document.title = 'Welcome · Cohabit';
  root.replaceChildren(h('div', { class: 'setup' }, form));
  nameInput.focus();
}

function enterApp() {
  buildShell();
  // Keep an invite link the visitor arrived with; otherwise start on Today.
  if (joinTokenFromHash() || location.hash === '' || location.hash === '#' || location.hash === '#/') route(true);
  else location.hash = '#/';
}

// Log out. GOOGLE: POST /logout (CSRF header added by api.js) and drop the session user.
// DEV_HEADER: clear the active identity but keep the saved list for the setup screen.
async function logOut(modal) {
  if (isGoogleMode()) {
    try {
      await api.logout();
    } catch (e) {
      if (e.status !== 401) { toastError(e); return; }
    }
    setSessionUser(null);
  } else {
    clearCurrentIdentity();
  }
  if (modal) modal.close();
  groupsCache = [];
  history.replaceState(null, '', location.pathname);
  toast('Logged out');
  showLanding();
}

// ---------- profile menu ----------
function logoutButton(getModal) {
  return h('div', { class: 'profile-logout' },
    h('button', { type: 'button', class: 'btn btn-danger', style: 'width:100%', onclick: () => logOut(getModal()) }, 'Log out'));
}

function openProfile() {
  const me = currentIdentity();
  if (!me) return;
  let modal;
  if (isGoogleMode()) {
    modal = openModal({
      title: 'Profile',
      content: h('div', null,
        h('div', { class: 'row', style: 'flex-wrap:nowrap' },
          avatar(me.name, me.id, 'lg'),
          h('div', { style: 'min-width:0' },
            h('div', { class: 'member-name' }, me.name),
            me.email ? h('div', { class: 'member-sub', style: 'overflow-wrap:anywhere' }, me.email) : null,
            h('div', { class: 'member-sub' }, `Timezone ${tz()}`))),
        logoutButton(() => modal)),
    });
    return;
  }
  const copyBtn = h('button', {
    type: 'button', class: 'btn btn-secondary btn-sm',
    onclick: async () => {
      const ok = await copyText(me.id);
      toast(ok ? 'User id copied. Send it to whoever is inviting you.' : 'Could not copy. Select the id and copy it by hand.', ok ? 'success' : 'error');
    },
  }, icon('copy', 15), 'Copy');

  const identRow = (i) => h('button', {
    type: 'button', class: 'ident-row', disabled: i.id === me.id,
    onclick: () => {
      switchIdentity(i.id);
      modal.close();
      toast(`You are now ${i.name}`);
      enterApp();
    },
  }, avatar(i.name, i.id, 'md'),
  h('div', { class: 'grow' }, h('div', { class: 't' }, i.name), h('div', { class: 's mono' }, i.id)),
  i.id === me.id ? h('span', { class: 'chip good chip-sm' }, 'Current') : null);

  const content = h('div', null,
    h('div', { class: 'row' },
      avatar(me.name, me.id, 'lg'),
      h('div', null, h('div', { class: 'member-name' }, me.name), h('div', { class: 'member-sub' }, `Timezone ${tz()}`))),
    h('div', { class: 'field mt-20' },
      h('span', { class: 'label' }, 'Your user id'),
      h('div', { class: 'id-box' }, h('code', { class: 'mono' }, me.id), copyBtn),
      h('div', { class: 'field-hint' }, 'Anyone in a circle can invite you with this id.')),
    h('div', { class: 'field mt-20' },
      h('span', { class: 'label' }, 'Switch user'),
      identities().map(identRow),
      h('button', {
        type: 'button', class: 'ident-row',
        onclick: () => { modal.close(); showSetup({ cancelable: true }); },
      }, h('span', { class: 'avatar md' }, icon('plus', 15)),
      h('div', { class: 'grow' }, h('div', { class: 't' }, 'New identity'), h('div', { class: 's' }, 'Try a circle from both sides in one browser')))),
    h('p', { class: 'hint mt-16' }, 'Identities live only in this browser. There are no passwords yet.'),
    logoutButton(() => modal));

  modal = openModal({ title: 'Profile', content });
}

// ---------- shell ----------
function navItem(href, iconName, label, key) {
  return h('a', { class: 'nav-item', href, dataset: { key } }, icon(iconName), label);
}

function buildShell() {
  const me = currentIdentity();
  const circlesList = h('div', { class: 'side-circles' }, h('p', { class: 'side-empty' }, 'Loading…'));
  const main = h('main', { class: 'main', id: 'main', tabindex: '-1' });

  const google = isGoogleMode();
  const profileBtn = h('button', { type: 'button', class: 'profile-btn', onclick: openProfile, 'aria-label': google ? 'Profile and log out' : 'Profile and switch user' },
    avatar(me.name, me.id, 'md'),
    h('div', { style: 'min-width:0' },
      h('div', { class: 'profile-name' }, me.name),
      h('div', { class: 'profile-sub', style: 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap' },
        google ? (me.email || 'Profile · log out') : 'Profile · switch user')));

  const sidebar = h('aside', { class: 'sidebar', 'aria-label': 'Navigation' },
    brand(),
    h('nav', { class: 'nav' },
      navItem('#/', 'home', 'Today', 'today'),
      navItem('#/notifications', 'bell', 'Notifications', 'notifications')),
    h('div', { class: 'side-section' },
      h('p', { class: 'side-label' }, 'Your circles'),
      circlesList,
      h('button', { type: 'button', class: 'side-new', onclick: () => { closeDrawer(); openNewCircle(); } }, icon('plus', 16), 'New circle')),
    profileBtn);

  const app = h('div', { class: 'app' },
    sidebar,
    h('div', { class: 'scrim', onclick: closeDrawer }),
    h('div', { style: 'flex:1 1 auto;min-width:0' },
      h('div', { class: 'mobile-bar' },
        h('button', { type: 'button', class: 'icon-btn icon-btn-sm', 'aria-label': 'Open menu', onclick: openDrawer }, icon('menu')),
        brand(true),
        h('button', { type: 'button', class: 'icon-btn icon-btn-sm', 'aria-label': 'Profile', onclick: openProfile, style: 'padding:0;background:transparent;box-shadow:none' }, avatar(me.name, me.id, 'md'))),
      main));

  root.className = '';
  root.replaceChildren(app);
  shell = { app, main, sidebar, circlesList };
  screen = 'app';
  loadSidebarGroups();
}

function openDrawer() { if (shell) shell.app.classList.add('drawer-open'); }
function closeDrawer() { if (shell) shell.app.classList.remove('drawer-open'); }

async function loadSidebarGroups() {
  if (!shell) return;
  const list = shell.circlesList;
  try {
    groupsCache = await api.listGroups();
  } catch (e) {
    list.replaceChildren(h('p', { class: 'side-empty' }, `Couldn’t load circles. ${e.message}`));
    return;
  }
  if (!groupsCache.length) {
    list.replaceChildren(h('p', { class: 'side-empty' }, 'No circles yet. Start one and invite a friend.'));
  } else {
    list.replaceChildren(...groupsCache.map((g) =>
      h('a', { class: `side-link ${famClass(famFor(g.id))}`, href: `#/groups/${g.id}`, dataset: { gid: g.id } },
        h('span', { class: 'dot' }), h('span', { class: 'txt' }, g.name))));
  }
  markActive();
}

let activeRoute = null;
function markActive() {
  if (!shell) return;
  shell.sidebar.querySelectorAll('.nav-item').forEach((a) =>
    a.classList.toggle('active', !!activeRoute && a.dataset.key === activeRoute.key));
  shell.sidebar.querySelectorAll('.side-link').forEach((a) =>
    a.classList.toggle('active', !!activeRoute && a.dataset.gid === activeRoute.gid));
}

// ---------- router ----------
const routes = [
  [/^(#\/?)?$/, () => ({ key: 'today', view: (ctx) => renderToday(ctx) })],
  [/^#\/habits\/([^/?]+)$/, (m) => ({ key: 'habit', view: (ctx) => renderHabit(ctx, m[1]) })],
  [/^#\/groups\/([^/?]+)$/, (m) => ({ key: 'group', gid: m[1], view: (ctx) => renderCircle(ctx, m[1]) })],
  [/^#\/groups\/([^/?]+)\/habits\/([^/?]+)$/, (m) => ({ key: 'joint', gid: m[1], view: (ctx) => renderJoint(ctx, m[1], m[2]) })],
  [/^#\/notifications$/, () => ({ key: 'notifications', view: (ctx) => renderNotifications(ctx) })],
  [JOIN_RE, (m) => ({ key: 'join', view: (ctx) => renderJoin(ctx, m[1]) })],
];

function matchRoute(hash) {
  for (const [re, make] of routes) {
    const m = hash.match(re);
    if (m) return make(m.map((p) => (p ? decodeURIComponent(p) : p)));
  }
  return null;
}

function errorPage(err, retry) {
  const notFound = err && (err.status === 404 || err.status === 403);
  return h('div', null,
    pageHead({ title: notFound ? 'Not here' : 'Something went wrong' }),
    h('div', { class: 'card mt-20', style: 'max-width:620px' },
      inlineError(err && err.message ? err.message : String(err)),
      h('div', { class: 'row mt-16' },
        h('a', { class: 'btn btn-secondary', href: '#/' }, 'Back to Today'),
        !notFound ? h('button', { type: 'button', class: 'btn btn-primary', onclick: retry }, 'Try again') : null)));
}

async function route(navigated) {
  if (!shell) return;
  const hash = location.hash || '';
  const match = matchRoute(hash);
  const token = ++renderToken;
  activeRoute = match;
  markActive();
  closeDrawer();
  if (navigated) {
    shell.main.replaceChildren(loadingState());
    window.scrollTo(0, 0);
  }
  const ctx = {
    rerender: () => route(false),
    refreshGroups: () => loadSidebarGroups(),
    groups: () => groupsCache,
  };
  try {
    const node = match
      ? await match.view(ctx)
      : errorPage({ status: 404, message: 'There is no page at this address.' });
    if (token !== renderToken || !shell) return;
    shell.main.replaceChildren(node);
  } catch (err) {
    if (token !== renderToken || !shell) return;
    if (err && err.status === 401 && isGoogleMode()) return; // the sign-in screen takes over
    console.error(err);
    shell.main.replaceChildren(errorPage(err, () => route(true)));
  }
}

// ---------- boot ----------
window.addEventListener('hashchange', () => {
  if (shell) { route(true); return; }
  // Signed out / no identity: an invite link opened now should still be remembered.
  if (screen === 'landing' && joinTokenFromHash()) showLanding();
});
window.addEventListener('cohabit:groups-changed', () => loadSidebarGroups());
window.addEventListener('cohabit:unauthorized', () => {
  if (!isGoogleMode()) {
    toast('The server says you are not signed in.', 'error');
    return;
  }
  if (screen === 'landing') return;
  setSessionUser(null);
  showLanding();
});
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeDrawer(); });

async function boot() {
  applyTheme();
  screen = 'boot';
  let cfg;
  try {
    cfg = await api.authConfig();
  } catch (e) {
    if (e.status === 0) { showBootError(e); return; }
    cfg = { mode: 'DEV_HEADER', loginUrl: null }; // older backend without /auth/config
  }
  setAuthConfig(cfg);

  if (isGoogleMode()) {
    try {
      setSessionUser(await api.me());
    } catch (e) {
      if (e.status === 401) { showLanding(); return; }
      showBootError(e);
      return;
    }
    if (!currentIdentity()) { showLanding(); return; }
    // Back from Google sign-in (lands on '/'): resume a saved invite link.
    const pending = takePendingJoin();
    if (pending && !joinTokenFromHash()) {
      history.replaceState(null, '', `${location.pathname}#/join/${encodeURIComponent(pending)}`);
    }
    buildShell();
    route(true);
    return;
  }

  if (currentIdentity()) {
    buildShell();
    route(true);
  } else {
    showLanding();
  }
}

boot();
