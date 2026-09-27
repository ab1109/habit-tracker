// Today (#/): personal habits with this week's marks, check-in, weekly ring, circles and activity.
import { h, icon, avatar, famClass, famFor, daySquares, capsules, bar, emptyState, toast, toastError } from '../ui.js';
import { api } from '../api.js';
import { currentIdentity } from '../identity.js';
import { todayISO, mondayOf, addDays, dateRange, longDate, weekdayName, relative, tz, hourNow, SHORT_DAYS } from '../dates.js';
import { isScheduled, isQuota, weekTarget, scheduleChip, streakText, periodState, numberWord } from '../schedule.js';
import { pageHead, openNewHabit, openNewCircle } from '../components.js';

/** Fallback for thisWeek if progress failed to load. */
function localWeek(habit, dates, week) {
  const target = weekTarget(habit);
  const done = week.filter((d) => dates.has(d) && isScheduled(habit, d)).length;
  return { done: Math.min(done, target), target };
}

export async function renderToday(ctx) {
  const me = currentIdentity();
  const today = todayISO();
  const monday = mondayOf(today);
  const week = dateRange(monday, addDays(monday, 6));

  const [habits, groups] = await Promise.all([api.listHabits(), api.listGroups()]);

  const [items, groupProgress] = await Promise.all([
    Promise.all(habits.map(async (habit) => {
      const [progress, checkins] = await Promise.all([
        api.progress(habit.id).catch(() => null),
        api.listCheckins(habit.id, week[0], week[6]).catch(() => []),
      ]);
      const dates = new Set(checkins.map((c) => c.localDate));
      const tw = progress && progress.thisWeek ? progress.thisWeek : localWeek(habit, dates, week);
      return { habit, progress, dates, tw };
    })),
    Promise.all(groups.map((g) => api.groupProgress(g.id).catch(() => null))),
  ]);

  // Which of my habits are shared, and with which circles.
  const sharedIn = new Map();
  groupProgress.forEach((gp, i) => {
    if (!gp) return;
    for (const s of gp.sharedHabits || []) {
      if (s.ownerUserId !== me.id) continue;
      if (!sharedIn.has(s.habit.id)) sharedIn.set(s.habit.id, []);
      sharedIn.get(s.habit.id).push(groups[i]);
    }
  });

  // "N of M done" today: habits that ask something of today.
  const due = items.filter((it) => {
    if (it.habit.scheduleType === 'SPECIFIC_WEEKDAYS') return isScheduled(it.habit, today);
    if (isQuota(it.habit)) return it.dates.has(today) || it.tw.done < it.tw.target;
    return true;
  });
  const doneToday = due.filter((it) => it.dates.has(today)).length;

  const hour = hourNow();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const firstName = me.name.split(/\s+/)[0];
  document.title = 'Today · Cohabit';

  async function checkIn(habit, btn) {
    btn.disabled = true;
    try {
      const r = await api.checkIn(habit.id);
      if (r.created) toast(`Checked in: ${habit.name}`, 'success');
      else toast('Already checked in today. Checking in again changed nothing.');
      ctx.rerender();
    } catch (e) {
      toastError(e);
      btn.disabled = false;
    }
  }

  function habitRow(it) {
    const { habit, progress, dates, tw } = it;
    const shared = sharedIn.get(habit.id) || [];
    const fam = shared.length ? famFor(shared[0].id) : 'lilac';
    const checked = dates.has(today);
    const scheduledToday = isScheduled(habit, today);

    let marks;
    if (isQuota(habit)) {
      marks = capsules(tw.done, tw.target);
    } else {
      const states = week.map((d) => {
        if (!isScheduled(habit, d)) return 'off';
        if (dates.has(d)) return 'full';
        if (d === today) return 'soft';
        return d > today ? 'future' : 'none';
      });
      const label = week.map((d, i) => `${SHORT_DAYS[i]} ${!isScheduled(habit, d) ? 'not scheduled' : dates.has(d) ? 'done' : 'not done'}`).join(', ');
      marks = daySquares(states, label);
    }

    const ps = periodState(progress);
    const vis = shared.length
      ? `${shared.map((g) => g.name).join(', ')} · shared for visibility`
      : 'Private · only you can see this';
    const btnLabel = checked
      ? `${habit.name}: already checked in today`
      : scheduledToday ? `Check in ${habit.name} for today` : `Check in ${habit.name} (today is not a scheduled day)`;
    const btn = h('button', {
      type: 'button', class: `check-btn ${checked ? 'done' : ''}`, 'aria-label': btnLabel, title: btnLabel,
      onclick: () => checkIn(habit, btn),
    }, icon('check', 16, 2.4));

    return h('div', { class: `habit-row ${famClass(fam)}` },
      btn,
      h('div', { class: 'habit-main' },
        h('a', { class: 'habit-name', href: `#/habits/${habit.id}` }, habit.name),
        h('span', { class: 'habit-meta' },
          h('span', { class: 'vis' }, vis),
          progress ? ` · ${streakText(progress, habit)}` : '',
          ps ? ` · ${ps.text}` : '')),
      h('span', { class: 'sched-chip' }, scheduleChip(habit)),
      marks,
      h('span', { class: 'count-chip', title: 'This week, against its own target' }, `${tw.done} / ${tw.target}`));
  }

  // ----- Today card -----
  const todayCard = h('section', { class: 'card' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Today'),
      h('span', { class: 'hint' }, 'Squares are days · capsules are a weekly quota')),
    items.length
      ? h('div', { class: 'habit-list' }, items.map(habitRow))
      : h('div', { class: 'mt-16' }, emptyState({
        title: 'No habits yet',
        text: 'Every day, a number of times a week, or a fixed set of weekdays. Each one is scored on its own terms instead of pretending it’s daily.',
        action: h('button', { type: 'button', class: 'btn btn-primary', onclick: () => openNewHabit(() => ctx.rerender()) }, 'New habit'),
      })));

  // ----- This week ring -----
  const sumDone = items.reduce((a, it) => a + it.tw.done, 0);
  const sumTarget = items.reduce((a, it) => a + it.tw.target, 0);
  const pct = sumTarget ? Math.round((sumDone / sumTarget) * 100) : 0;
  const C = 2 * Math.PI * 66;
  const ring = h('div', {
    class: 'ring',
    svg: `<svg width="150" height="150" viewBox="0 0 150 150" fill="none" aria-hidden="true">
      <circle class="track" cx="75" cy="75" r="66" stroke-width="12"/>
      <circle class="value" cx="75" cy="75" r="66" stroke-width="12" stroke-linecap="round"
        stroke-dasharray="${C.toFixed(1)}" stroke-dashoffset="${(C * (1 - pct / 100)).toFixed(1)}"
        transform="rotate(-90 75 75)" ${pct === 0 ? 'stroke-opacity="0"' : ''}/></svg>`,
  });
  ring.append(h('div', { class: 'ring-label' },
    h('span', { class: 'ring-num' }, sumTarget ? `${pct}%` : '—'),
    h('span', { class: 'ring-sub' }, 'of your targets')));
  const weekCard = h('section', { class: 'card ring-card fam-mint', 'aria-label': `This week: ${pct}% of your targets` },
    h('h2', { class: 'h2' }, 'This week'),
    h('div', { class: 'ring-wrap' }, ring),
    h('p', { class: 'note' }, sumTarget
      ? `${sumDone} of ${sumTarget} this week. Counted against each habit’s own schedule, not against seven days.`
      : 'Add a habit and this fills in, counted against each habit’s own schedule.'));

  // ----- Circles (shared goals) -----
  const goals = groups.map((g, i) => ({ g, gp: groupProgress[i] }));
  const goalsCard = h('section', { class: 'card' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Shared goals'),
      groups.length ? h('button', { type: 'button', class: 'btn-link', onclick: openNewCircle }, '+ New circle') : null),
    groups.length
      ? goals.map(({ g, gp }) => {
        const fam = famFor(g.id);
        if (!gp) {
          return h('div', { class: `metric-row ${famClass(fam)}` },
            h('div', { class: 'top' }, h('a', { class: 'name', href: `#/groups/${g.id}` }, g.name), h('span', { class: 'val' }, '—')),
            h('div', { class: 'note' }, 'Couldn’t load this week’s progress.'));
        }
        const left = Math.max(0, gp.totalTarget - gp.totalDone);
        const ns = (gp.sharedHabits || []).length;
        const nj = (gp.jointHabits || []).length;
        const parts = [`${ns} shared`, `${nj} joint`];
        if (gp.totalTarget === 0) parts.push('nothing to count yet');
        else if (left === 0) parts.push('everyone is on target');
        else parts.push(`${cap1(numberWord(left))} to go`);
        return h('div', { class: `metric-row ${famClass(fam)}`, style: 'padding-top:18px' },
          h('div', { class: 'top' },
            h('a', { class: 'name', href: `#/groups/${g.id}`, style: 'font-size:15px;color:var(--ink)' }, g.name),
            h('span', { class: 'val' }, `${gp.totalDone} of ${gp.totalTarget}`)),
          bar(gp.totalTarget ? gp.totalDone / gp.totalTarget : 0),
          h('div', { class: 'note' }, parts.join(' · ')));
      })
      : h('div', { class: 'mt-16' }, emptyState({
        title: 'No circles yet',
        text: 'Share one habit with a circle for the company, or hand it to the whole group so any single check-in covers the day for everyone.',
        action: h('button', { type: 'button', class: 'btn btn-primary', onclick: openNewCircle }, 'New circle'),
      })));

  // ----- Activity across circles -----
  const seen = new Set();
  const activity = [];
  groupProgress.forEach((gp, i) => {
    if (!gp) return;
    for (const a of gp.recentActivity || []) {
      if (seen.has(a.checkInId)) continue;
      seen.add(a.checkInId);
      activity.push({ ...a, groupName: groups[i].name });
    }
  });
  activity.sort((a, b) => String(b.recordedAt).localeCompare(String(a.recordedAt)));

  const activityCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Activity'),
    h('p', { class: 'hint', style: 'margin-top:6px' }, 'Only habits shared with your circles, and joint habits, appear here.'),
    activity.length
      ? activity.slice(0, 8).map((a) => feedItem(a, me, today))
      : h('p', { class: 'hint mt-16' }, 'Nothing yet. Check-ins from your circles will show up here.'));

  return h('div', null,
    pageHead({
      title: `${greeting}, ${firstName}`,
      sub: `${longDate(today)} · ${tz()}${due.length ? ` · ${numberWord(doneToday)} of ${numberWord(due.length)} done` : ''}`,
      actions: h('button', { type: 'button', class: 'btn btn-primary', style: 'height:48px', onclick: () => openNewHabit(() => ctx.rerender()) }, 'New habit'),
    }),
    h('div', { class: 'grid-3' },
      h('div', { class: 'span-2 stack' }, todayCard, goalsCard),
      h('div', { class: 'stack' }, weekCard, activityCard)));
}

function cap1(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function feedItem(a, me, today) {
  const who = a.performedByUserId === me.id ? 'You' : a.displayName || 'Someone';
  const day = a.localDate === today ? 'today' : weekdayName(a.localDate);
  const text = a.joint
    ? `${who} covered ${a.habitName} for ${day}.`
    : `${who} checked in on ${a.habitName}${a.localDate === today ? '' : ` for ${day}`}.`;
  return h('div', { class: 'feed-item' },
    avatar(a.displayName || '?', a.performedByUserId),
    h('div', null,
      h('div', { class: 'feed-text' }, text),
      h('div', { class: 'feed-when' }, [relative(a.recordedAt), a.groupName].filter(Boolean).join(' · '))));
}

