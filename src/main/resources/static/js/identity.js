// Local, auth-free identity. Each browser can hold several identities so one person can demo
// a circle by switching between users. Stored in localStorage; falls back to memory.

const LIST_KEY = 'cohabit.identities';
const CURRENT_KEY = 'cohabit.currentUserId';

const memory = new Map();

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return memory.has(key) ? memory.get(key) : null;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    memory.set(key, value);
  }
}

export function identities() {
  try {
    const list = JSON.parse(read(LIST_KEY) || '[]');
    return Array.isArray(list) ? list.filter((i) => i && i.id && i.name) : [];
  } catch {
    return [];
  }
}

function saveList(list) {
  write(LIST_KEY, JSON.stringify(list));
}

export function currentIdentity() {
  const id = read(CURRENT_KEY);
  if (!id) return null;
  return identities().find((i) => i.id === id) || null;
}

export const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function newUuid() {
  if (crypto && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  // Fallback for non-secure contexts (e.g. plain http on a LAN address).
  const b = crypto.getRandomValues(new Uint8Array(16));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

export function createIdentity(name, existingId) {
  const id = (existingId || '').trim().toLowerCase() || newUuid();
  if (!UUID_RE.test(id)) throw new Error('That user id is not a valid UUID.');
  const list = identities().filter((i) => i.id !== id);
  const ident = {
    id,
    name: name.trim(),
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
    createdAt: new Date().toISOString(),
  };
  list.push(ident);
  saveList(list);
  write(CURRENT_KEY, id);
  return ident;
}

export function switchIdentity(id) {
  if (identities().some((i) => i.id === id)) write(CURRENT_KEY, id);
}

export function forgetIdentity(id) {
  saveList(identities().filter((i) => i.id !== id));
  if (read(CURRENT_KEY) === id) {
    const next = identities()[0];
    write(CURRENT_KEY, next ? next.id : '');
  }
}
