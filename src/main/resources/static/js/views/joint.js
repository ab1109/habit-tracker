// Joint habit (#/groups/{gid}/habits/{hid}): one row for the whole circle, filled with
// whoever covered each day.
import { h, avatar, famClass, famFor, initials, bar, chip, icon, toast, toastError, confirmDialog } from '../ui.js';
import { api } from '../api.js';
import { currentIdentity } from '../identity.js';
import {
  todayISO, mondayOf, addDays, dateRange, dayIndex, monthStart, monthEnd, monthName, dayMonth, weekdayName,
  timeOf, relative, SHORT_DAYS, LETTERS,
} from '../dates.js';
import { isScheduled, scheduleLong, streakText, periodState, numberWord, plural, trackedFrom } from '../schedule.js';
import { pageHead } from '../components.js';

// Result of the last check-in tap per habit, so the explanation survives the re-render.
const lastTap = new Map();

export async function renderJoint(ctx, gid, hid) {
  const me = currentIdentity();
  const today = todayISO();
  const mon = mondayOf(today);
  const sun = addDays(mon, 6);
  const ms = monthStart(today);
  const me2 = monthEnd(today);
  const from = mon < ms ? mon : ms;
  const to = sun > me2 ? sun : me2;

  const [group, habit, progress, checkins] = await Promise.all([
    api.getGroup(gid),
    api.getHabit(hid),
    api.progress(hid),
    api.listCheckins(hid, from, to),
  ]);
  const trackedStart = trackedFrom(habit);
  const fam = famFor(gid);
  document.title = `${habit.name} · ${group.name} · Cohabit`;

  const names = new Map(group.members.map((m) => [m.userId, m.displayName]));
  const nameOf = (uid) => names.get(uid) || 'Former member';
  const whoLabel = (uid) => (uid === me.id ? 'You' : nameOf(uid));
  const byDate = new Map();
  for (const c of checkins) if (!byDate.has(c.localDate)) byDate.set(c.localDate, c);

  // ----- check in -----
  async function checkIn(btn) {
    btn.disabled = true;
    try {
      const r = await api.checkIn(hid);
      lastTap.set(hid, { created: r.created, at: Date.now(), checkIn: r.checkIn });
      if (r.created) toast('Covered for the group', 'success');
      else toast('Already covered — changed nothing');
      ctx.rerender();
    } catch (e) {
      toastError(e);
      btn.disabled = false;
    }
  }

  async function undo(btn) {
    const ok = await confirmDialog({
      title: 'Undo your check-in?',
      text: 'Today goes back to uncovered, for the whole circle, until someone checks in.',
      confirmLabel: 'Undo',
    });
    if (!ok) return;
    btn.disabled = true;
    try {
      await api.undoCheckIn(hid);
      lastTap.delete(hid);
      toast('Check-in undone. Today is uncovered again.');
      ctx.rerender();
    } catch (e) {
      toastError(e);
      btn.disabled = false;
    }
  }

  const todayC = byDate.get(today);
  const scheduledToday = isScheduled(habit, today);
  const n = group.members.length;
  let heading;
  if (todayC) heading = `${whoLabel(todayC.performedByUserId)} covered today at ${timeOf(todayC.recordedAt)}`;
  else if (scheduledToday) heading = 'Nobody has covered today yet';
  else heading = 'Today isn’t one of the chosen days';

  const tap = lastTap.get(hid);
  let noticeText;
  if (tap && !tap.created) {
    const c = tap.checkIn || todayC;
    noticeText = [
      'You tapped check in again. It returned ',
      h('strong', null, 'already covered'),
      ` and changed nothing, so the day still holds one record${c ? `, credited to ${c.performedByUserId === me.id ? 'you' : nameOf(c.performedByUserId)} at ${timeOf(c.recordedAt)}` : ''}.`,
    ];
  } else if (tap && tap.created) {
    noticeText = ['Covered. The day now holds one record, credited to you. Anyone who checks in after this gets ', h('strong', null, 'already covered'), ' and nothing changes.'];
  } else {
    noticeText = ['If someone already covered today, checking in again returns ', h('strong', null, 'already covered'), ' and changes nothing, so a day never holds more than one record.'];
  }

  const btn = h('button', { type: 'button', class: 'btn btn-primary btn-lg', onclick: () => checkIn(btn) }, 'Check in for the group');
  // Only the member who covered today can take it back.
  const undoBtn = todayC && todayC.performedByUserId === me.id
    ? h('button', { type: 'button', class: 'btn-link danger', onclick: () => undoBtn && undo(undoBtn) }, 'Undo my check-in')
    : null;
  const hero = h('section', { class: `joint-hero ${famClass(fam)}` },
    h('div', { class: 'top' },
      h('div', null,
        h('p', { class: 'eyebrow' }, `Today, ${dayMonth(today)}`),
        h('h2', null, heading),
        h('p', { class: 'desc' }, n > 1
          ? `One check-in from any of the ${numberWord(n)} of you closes the day for the whole circle.`
          : 'You’re the only member so far. Invite someone to share the load.')),
      h('div', { class: 'stack', style: 'align-items:flex-end;gap:8px' }, btn, undoBtn)),
    h('div', { class: 'notice' }, icon('info', 18), h('p', null, noticeText)));

  // ----- this week -----
  const week = dateRange(mon, sun);
  const weekCard = h('section', { class: `card ${famClass(fam)}` },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'This week'),
      h('span', { class: 'hint' }, 'One row for the circle, not one per person')),
    h('div', { class: 'week7' }, week.map((d, i) => {
      const c = byDate.get(d);
      const sched = isScheduled(habit, d);
      let cls = 'day-box';
      let mark;
      let text;
      if (c) {
        cls += ' done';
        mark = avatar(nameOf(c.performedByUserId), c.performedByUserId, 'xl');
        text = timeOf(c.recordedAt);
      } else if (trackedStart && d < trackedStart) {
        cls += ' future';
        mark = h('span', { class: 'ph' });
        text = 'Not yet';
      } else if (!sched) {
        cls += ' off';
        mark = h('span', { class: 'ph' });
        text = 'Day off';
      } else if (d === today) {
        cls += ' today';
        mark = h('span', { class: 'ph' }, '+');
        text = 'Open';
      } else if (d < today) {
        mark = h('span', { class: 'ph' }, '·');
        text = 'Missed';
      } else {
        cls += ' future';
        mark = h('span', { class: 'ph' });
        text = '';
      }
      const label = `${weekdayName(d)}: ${c ? `covered by ${nameOf(c.performedByUserId)} at ${timeOf(c.recordedAt)}` : text || 'upcoming'}`;
      return h('div', { class: cls, title: label, 'aria-label': label },
        h('span', { class: 'dl' }, SHORT_DAYS[i]), mark, h('span', { class: 'dt' }, text));
    })));

  // ----- month -----
  const monthDays = dateRange(ms, me2);
  const elapsed = monthDays.filter((d) => d <= today && (!trackedStart || d >= trackedStart) && isScheduled(habit, d));
  const coveredDays = elapsed.filter((d) => byDate.has(d)).length;
  const cells = [];
  for (let i = 0; i < dayIndex(ms); i++) cells.push(h('div', { class: 'mday blank', 'aria-hidden': 'true' }));
  for (const d of monthDays) {
    const c = byDate.get(d);
    const dayNum = String(Number(d.slice(8)));
    let cls = 'mday';
    let content = '';
    let label;
    if (c) {
      cls += ` cov ${famClass(famFor(c.performedByUserId))}`;
      content = initials(nameOf(c.performedByUserId));
      label = `covered by ${nameOf(c.performedByUserId)}`;
    } else if (d > today) { cls += ' future'; label = 'upcoming'; }
    else if (!isScheduled(habit, d)) { cls += ' off'; label = 'day off'; }
    else { content = '—'; label = 'uncovered'; }
    if (d === today) cls += ' today';
    cells.push(h('div', { class: cls, title: `${dayMonth(d)} · ${label}` }, h('span', { class: 'n' }, dayNum), content));
  }
  const monthCard = h('section', { class: 'card' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, monthName(today)),
      h('span', { class: 'hint' }, `Covered on ${coveredDays} of ${elapsed.length} ${plural(elapsed.length, 'day')} so far`)),
    h('div', { class: 'month', role: 'grid', 'aria-label': `${monthName(today)} coverage` },
      LETTERS.map((l) => h('span', { class: 'ml' }, l)),
      cells));

  // ----- who covered it (month, by performer) -----
  const counts = new Map();
  for (const c of checkins) {
    if (c.localDate < ms || c.localDate > me2) continue;
    counts.set(c.performedByUserId, (counts.get(c.performedByUserId) || 0) + 1);
  }
  const split = [...counts.entries()].sort((a, b) => b[1] - a[1]);
  const total = split.reduce((a, [, v]) => a + v, 0);
  const ps = periodState(progress);
  const splitCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Who covered it'),
    h('p', { class: 'hint' }, `${monthName(today)}, by performer`),
    split.length
      ? split.map(([uid, count]) => h('div', { class: `metric-row ${famClass(famFor(uid))}` },
        h('div', { class: 'top' },
          h('span', { class: 'name' }, whoLabel(uid)),
          h('span', { class: 'val' }, `${count} ${plural(count, 'day')}`)),
        bar(total ? count / total : 0, 'thin')))
      : h('p', { class: 'hint mt-12' }, 'Nobody has covered it this month yet.'),
    h('p', { class: 'hint', style: 'margin-top:18px;padding:14px 16px;border-radius:16px;background:var(--row);color:var(--text-2)' },
      `The group keeps one streak: ${streakText(progress, habit).toLowerCase()}${ps ? `, ${ps.text.toLowerCase()}` : ''}. This split only says who did the work.`));

  const tw = progress.thisWeek;
  const statsCard = h('section', { class: `card ${famClass(fam)}` },
    h('div', { class: 'stats', style: 'margin-top:0;grid-template-columns:repeat(2,minmax(0,1fr))' },
      h('div', { class: 'stat' },
        h('div', { class: 'stat-label' }, 'This week'),
        h('div', { class: 'stat-value' }, String(tw.done), h('small', null, ` / ${tw.target}`)),
        h('div', { class: 'stat-note' }, tw.done >= tw.target ? 'Target met' : `${tw.target - tw.done} to go`)),
      h('div', { class: 'stat' },
        h('div', { class: 'stat-label' }, 'Best run'),
        h('div', { class: 'stat-value' }, String(progress.longestStreak || 0)),
        h('div', { class: 'stat-note' }, progress.streakUnit === 'WEEKS' ? 'weeks' : 'days'))));

  const recent = [...checkins].sort((a, b) => String(b.recordedAt).localeCompare(String(a.recordedAt))).slice(0, 6);
  const activityCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Activity'),
    recent.length
      ? recent.map((c) => h('div', { class: 'feed-item' },
        avatar(nameOf(c.performedByUserId), c.performedByUserId),
        h('div', null,
          h('div', { class: 'feed-text' }, `${whoLabel(c.performedByUserId)} covered ${c.localDate === today ? 'today' : weekdayName(c.localDate)} at ${timeOf(c.recordedAt)}.`),
          h('div', { class: 'feed-when' }, relative(c.recordedAt)))))
      : h('p', { class: 'hint mt-12' }, 'No check-ins this month yet.'));

  const stack = h('div', { class: 'avatar-stack' },
    group.members.slice(0, 5).map((m) => avatar(m.displayName, m.userId, 'lg')));

  return h('div', { class: famClass(fam) },
    pageHead({
      crumb: [{ label: 'Circles' }, { label: group.name, href: `#/groups/${gid}` }],
      title: habit.name,
      chips: [
        h('span', { class: 'chip tinted' }, 'Joint habit'),
        chip(`Owned by ${group.name}`),
        chip(scheduleLong(habit)),
        habit.archivedAt ? chip('Archived', 'bad') : null,
      ],
      actions: [stack, h('a', { class: 'btn btn-card', style: 'height:48px', href: `#/groups/${gid}` }, 'Back to circle')],
    }),
    h('div', { class: 'grid-3', style: 'margin-top:20px' },
      h('div', { class: 'span-2 stack' }, hero, weekCard, monthCard),
      h('div', { class: 'stack' }, statsCard, splitCard, activityCard)));
}
