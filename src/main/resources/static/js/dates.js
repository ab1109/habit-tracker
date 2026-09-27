// Date helpers. Calendar dates are plain 'YYYY-MM-DD' strings; arithmetic is done in UTC
// so a date never shifts. "Today" is resolved in the browser's IANA timezone, which is the
// same zone we send to the API as X-Timezone.

export const WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
export const SHORT_DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
export const LETTERS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

export function tz() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

const partFormatters = new Map();
function partsIn(date, zone) {
  let f = partFormatters.get(zone);
  if (!f) {
    f = new Intl.DateTimeFormat('en-US', {
      timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    });
    partFormatters.set(zone, f);
  }
  const o = {};
  for (const p of f.formatToParts(date)) o[p.type] = p.value;
  if (o.hour === '24') o.hour = '00';
  return o;
}

export function localDateOf(instant) {
  const p = partsIn(new Date(instant), tz());
  return `${p.year}-${p.month}-${p.day}`;
}

export function todayISO() {
  return localDateOf(Date.now());
}

export function hourNow() {
  return Number(partsIn(new Date(), tz()).hour);
}

export function timeOf(instant) {
  const p = partsIn(new Date(instant), tz());
  return `${p.hour}:${p.minute}`;
}

export function parseISO(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

export function toISO(date) {
  return date.toISOString().slice(0, 10);
}

export function addDays(iso, n) {
  const d = parseISO(iso);
  d.setUTCDate(d.getUTCDate() + n);
  return toISO(d);
}

/** 0 = Monday … 6 = Sunday */
export function dayIndex(iso) {
  return (parseISO(iso).getUTCDay() + 6) % 7;
}

export function mondayOf(iso) {
  return addDays(iso, -dayIndex(iso));
}

export function daysBetween(a, b) {
  return Math.round((parseISO(b) - parseISO(a)) / 86400000);
}

export function dateRange(from, to) {
  const out = [];
  for (let d = from; d <= to; d = addDays(d, 1)) out.push(d);
  return out;
}

export function weekdayOf(iso) {
  return WEEKDAYS[dayIndex(iso)];
}

export function monthStart(iso) {
  return iso.slice(0, 8) + '01';
}

export function monthEnd(iso) {
  const d = parseISO(monthStart(iso));
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(0);
  return toISO(d);
}

function fmtUTC(iso, opts) {
  return new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', ...opts }).format(parseISO(iso));
}

/** "Sunday, 20 September" */
export function longDate(iso) {
  return `${weekdayName(iso)}, ${dayMonth(iso)}`;
}
/** "Sunday" */
export function weekdayName(iso) {
  return fmtUTC(iso, { weekday: 'long' });
}
/** "20 September" */
export function dayMonth(iso) {
  return fmtUTC(iso, { day: 'numeric', month: 'long' });
}
/** "20 Sep" */
export function shortDayMonth(iso) {
  return fmtUTC(iso, { day: 'numeric', month: 'short' });
}
/** "September" */
export function monthName(iso) {
  return fmtUTC(iso, { month: 'long' });
}

/** "18 minutes ago", "Yesterday, 9:12", "Friday", "2 September" */
export function relative(instant) {
  if (!instant) return '';
  const t = new Date(instant).getTime();
  const secs = (Date.now() - t) / 1000;
  if (secs < 60) return 'Just now';
  if (secs < 3600) {
    const m = Math.floor(secs / 60);
    return `${m} minute${m === 1 ? '' : 's'} ago`;
  }
  if (secs < 6 * 3600) {
    const h = Math.floor(secs / 3600);
    return `${h} hour${h === 1 ? '' : 's'} ago`;
  }
  const d = localDateOf(instant);
  const today = todayISO();
  if (d === today) return `Today, ${timeOf(instant)}`;
  if (d === addDays(today, -1)) return `Yesterday, ${timeOf(instant)}`;
  if (daysBetween(d, today) < 7 && daysBetween(d, today) > 0) return weekdayName(d);
  return dayMonth(d);
}

/** "20 Sep, 18:00" for instants */
export function dateTime(instant) {
  if (!instant) return '';
  return `${shortDayMonth(localDateOf(instant))}, ${timeOf(instant)}`;
}
