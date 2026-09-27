// Notifications (#/notifications): weekly digest preference, timezone, delivery log.
import { h, chip, emptyState, toast, field } from '../ui.js';
import { api } from '../api.js';
import { tz, dayMonth, dateTime } from '../dates.js';
import { pageHead } from '../components.js';

function timezones(extra) {
  let list = [];
  try {
    if (typeof Intl.supportedValuesOf === 'function') list = Intl.supportedValuesOf('timeZone');
  } catch { list = []; }
  const set = new Set(list);
  for (const z of extra) if (z) set.add(z);
  return [...set].sort((a, b) => a.localeCompare(b));
}

export async function renderNotifications() {
  const [prefs, log, groups] = await Promise.all([
    api.getPrefs(),
    api.listNotifications(),
    api.listGroups().catch(() => []),
  ]);
  document.title = 'Notifications · Cohabit';
  const groupName = new Map(groups.map((g) => [g.id, g.name]));
  const browserTz = tz();

  const toggle = h('input', { type: 'checkbox', role: 'switch', checked: !!prefs.weeklyDigestEnabled, 'aria-label': 'Weekly digest' });
  const select = h('select', { class: 'select' },
    timezones([prefs.timezone, browserTz, 'UTC']).map((z) =>
      h('option', { value: z, selected: z === prefs.timezone }, z)));
  const errorEl = h('div', { class: 'inline-error form-error', role: 'alert', hidden: true });
  const saveBtn = h('button', { type: 'submit', class: 'btn btn-primary' }, 'Save preferences');

  const form = h('form', { class: 'card', novalidate: true },
    h('h2', { class: 'h2' }, 'Preferences'),
    h('p', { class: 'hint' }, 'One digest a week. The only notification anyone asks for.'),
    h('label', { class: 'pref-row' },
      h('div', null,
        h('div', { class: 'opt-title' }, 'Weekly digest'),
        h('span', { class: 'opt-note' }, 'Sunday evening, every circle gets a single summary: who held their streak, who quietly dropped off, and which joint habits went uncovered.')),
      h('span', { class: 'switch' }, toggle, h('span', { class: 'track', 'aria-hidden': 'true' }))),
    h('div', { class: 'mt-16' },
      field('Deliver at 18:00 on Sunday in', select,
        browserTz !== prefs.timezone
          ? h('span', null, `This browser is on ${browserTz}. `,
            h('button', { type: 'button', class: 'btn-link', onclick: () => { select.value = browserTz; } }, 'Use it'))
          : 'Matches this browser’s timezone.')),
    errorEl,
    h('div', { class: 'row mt-20' }, saveBtn));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorEl.hidden = true;
    saveBtn.disabled = true;
    try {
      const saved = await api.putPrefs({ weeklyDigestEnabled: toggle.checked, timezone: select.value });
      toggle.checked = !!saved.weeklyDigestEnabled;
      select.value = saved.timezone;
      toast(saved.weeklyDigestEnabled ? `Saved. Digests arrive Sunday 18:00, ${saved.timezone}.` : 'Saved. Weekly digests are off.', 'success');
    } catch (err) {
      errorEl.textContent = err.message;
      errorEl.hidden = false;
    } finally {
      saveBtn.disabled = false;
    }
  });

  const logCard = h('section', { class: 'card' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Delivery log'),
      h('span', { class: 'hint' }, `${log.length} ${log.length === 1 ? 'delivery' : 'deliveries'}`)),
    h('p', { class: 'hint' }, 'Digests sent to you, newest first.'),
    log.length
      ? log.map((n) => h('article', { class: 'delivery' },
        h('div', { class: 'top' },
          chip(n.status === 'SENT' ? 'Sent' : n.status === 'FAILED' ? 'Failed' : n.status, `${n.status === 'SENT' ? 'good' : 'bad'} chip-sm`),
          h('div', { class: 'subj' }, n.subject || '(no subject)')),
        h('div', { class: 'meta' }, [
          n.kind === 'WEEKLY_DIGEST' ? 'Weekly digest' : n.kind,
          n.periodStart ? `week of ${dayMonth(n.periodStart)}` : null,
          groupName.get(n.groupId) || (n.groupId ? 'a circle you left' : null),
          n.attemptedAt ? `attempted ${dateTime(n.attemptedAt)}` : null,
        ].filter(Boolean).join(' · ')),
        n.body ? h('details', { class: 'body-text' }, h('summary', null, 'Show message'), h('pre', null, n.body)) : null))
      : h('div', { class: 'mt-16' }, emptyState({
        small: true,
        title: 'No digests yet',
        text: 'The first one goes out on Sunday at 18:00 in your chosen timezone, one per circle.',
      })));

  return h('div', null,
    pageHead({ title: 'Notifications', sub: 'Weekly digest settings and everything we’ve sent you' }),
    h('div', { class: 'grid-3' },
      h('div', { class: 'stack' }, form),
      h('div', { class: 'span-2 stack' }, logCard)));
}
