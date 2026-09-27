// Thin fetch wrapper for the habit tracker REST API (same origin).
// Every request carries X-Timezone; X-User-Id only in DEV_HEADER mode (GOOGLE mode uses the
// session cookie). Mutating requests carry X-XSRF-TOKEN from the XSRF-TOKEN cookie.
// A 401 anywhere fires 'cohabit:unauthorized' so the app can show the sign-in screen.
// Errors come back as {"message": "..."}.
import { currentIdentity, isGoogleMode } from './identity.js';
import { tz } from './dates.js';

export class ApiError extends Error {
  constructor(status, message, body) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function csrfToken() {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  if (!m) return null;
  try { return decodeURIComponent(m[1]); } catch { return m[1]; }
}

async function request(method, path, body, { quiet401 = false } = {}) {
  const headers = { Accept: 'application/json', 'X-Timezone': tz() };
  if (!isGoogleMode()) {
    const me = currentIdentity();
    if (me) headers['X-User-Id'] = me.id;
  }
  if (method !== 'GET' && method !== 'HEAD') {
    const token = csrfToken(); // read at request time; it changes after sign-in
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  const init = { method, headers, credentials: 'same-origin' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(path, init);
  } catch {
    throw new ApiError(0, 'Could not reach the server. Is it running?');
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  if (!res.ok) {
    if (res.status === 401 && !quiet401) {
      window.dispatchEvent(new CustomEvent('cohabit:unauthorized'));
    }
    const message = (data && typeof data === 'object' && data.message)
      || defaultMessage(res.status);
    throw new ApiError(res.status, message, data);
  }
  return { status: res.status, data };
}

function defaultMessage(status) {
  if (status === 400) return 'That request was not valid.';
  if (status === 401) return 'Sign in required.';
  if (status === 403) return "You're not allowed to do that.";
  if (status === 404) return 'Not found.';
  return `Something went wrong (${status}).`;
}

const enc = encodeURIComponent;
const get = async (p) => (await request('GET', p)).data;
const post = async (p, b) => (await request('POST', p, b)).data;

export const api = {
  // auth
  authConfig: async () => (await request('GET', '/auth/config', undefined, { quiet401: true })).data,
  me: async () => (await request('GET', '/me', undefined, { quiet401: true })).data,
  logout: async () => (await request('POST', '/logout', undefined, { quiet401: true })).data,

  // invite links
  createInvite: (groupId) => post(`/groups/${enc(groupId)}/invites`),
  getInvite: (token) => get(`/invites/${enc(token)}`),
  acceptInvite: (token, displayName) => post(`/invites/${enc(token)}/accept`, { displayName }),

  // habits
  listHabits: () => get('/habits'),
  getHabit: (id) => get(`/habits/${enc(id)}`),
  createHabit: (body) => post('/habits', body),
  archiveHabit: async (id) => (await request('PATCH', `/habits/${enc(id)}/archive`)).data,

  // check-ins: 201 = new, 200 = already existed that local day (changed nothing)
  checkIn: async (habitId) => {
    const r = await request('POST', `/habits/${enc(habitId)}/checkins`);
    return { created: r.status === 201, checkIn: r.data };
  },
  // Removes today's check-in (made by mistake); 204 even if there was none.
  undoCheckIn: async (habitId) => (await request('DELETE', `/habits/${enc(habitId)}/checkins/today`)).data,
  listCheckins: (habitId, from, to) =>
    get(`/habits/${enc(habitId)}/checkins?from=${enc(from)}&to=${enc(to)}`),
  progress: (habitId) => get(`/habits/${enc(habitId)}/progress`),

  // circles
  listGroups: () => get('/groups'),
  getGroup: (id) => get(`/groups/${enc(id)}`),
  createGroup: (name, displayName) => post('/groups', { name, displayName }),
  invite: (groupId, userId, displayName) => post(`/groups/${enc(groupId)}/members`, { userId, displayName }),
  removeMember: async (groupId, userId) =>
    (await request('DELETE', `/groups/${enc(groupId)}/members/${enc(userId)}`)).data,
  shareHabit: (groupId, habitId) => post(`/groups/${enc(groupId)}/shared-habits`, { habitId }),
  unshareHabit: async (groupId, habitId) =>
    (await request('DELETE', `/groups/${enc(groupId)}/shared-habits/${enc(habitId)}`)).data,
  createJointHabit: (groupId, body) => post(`/groups/${enc(groupId)}/habits`, body),
  groupProgress: (groupId) => get(`/groups/${enc(groupId)}/progress`),
  digest: (groupId) => get(`/groups/${enc(groupId)}/digest`),

  // notifications
  getPrefs: () => get('/me/notification-preferences'),
  putPrefs: async (prefs) => (await request('PUT', '/me/notification-preferences', prefs)).data,
  listNotifications: () => get('/me/notifications'),
};
