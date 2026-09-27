// Habit detail (#/habits/{id}): stat cards, 40-week heatmap, 12 weeks on target, schedule, sharing.
import { h, famClass, famFor, chip, emptyState, toast, toastError, confirmDialog, inlineError } from '../ui.js';
import { api } from '../api.js';
import { currentIdentity } from '../identity.js';
import { todayISO, mondayOf, addDays, dayIndex, dayMonth, shortDayMonth, localDateOf, LETTERS } from '../dates.js';
import {
  isScheduled, isQuota, weekTarget, scheduleLong, scheduleOptions, streakText, periodState, streakUnitWord,
  weeksMet, numberWord, plural, cap,
} from '../schedule.js';
import { pageHead, openShare } from '../components.js';

const WEEKS = 40;

export async function renderHabit(ctx, id) {
  const habit = await api.getHabit(id);
  if (habit.ownerType === 'GROUP') {
    // Joint habits live under their circle.
    location.replace(`#/groups/${habit.ownerId}/habits/${habit.id}`);
    return h('div');
  }
  const me = currentIdentity();
  const isOwner = habit.ownerId === me.id;
  const today = todayISO();
  const curMon = mondayOf(today);
  const start = addDays(curMon, -(WEEKS - 1) * 7);

  const [progress, checkins, groups] = await Promise.all([
    api.progress(id),
    api.listCheckins(id, start, today),
    api.listGroups().catch(() => []),
  ]);
  const groupProgress = await Promise.all(groups.map((g) => api.groupProgress(g.id).catch(() => null)));
  const sharedWith = groups.filter((g, i) => groupProgress[i]
    && (groupProgress[i].sharedHabits || []).some((s) => s.habit.id === id));
  const sharedIds = new Set(sharedWith.map((g) => g.id));

  const fam = sharedWith.length ? famFor(sharedWith[0].id) : 'lilac';
  const dates = new Set(checkins.map((c) => c.localDate));
  const tw = progress.thisWeek || { done: 0, target: weekTarget(habit) };
  const left = Math.max(0, tw.target - tw.done);
  const ps = periodState(progress);
  const archived = !!habit.archivedAt;
  document.title = `${habit.name} · Cohabit`;

  // ----- actions -----
  async function doCheckIn(btn) {
    btn.disabled = true;
    try {
      const r = await api.checkIn(habit.id);
      if (r.created) toast('Checked in for today', 'success');
      else toast('Already checked in today. Checking in again changed nothing.');
      ctx.rerender();
    } catch (e) {
      toastError(e);
      btn.disabled = false;
    }
  }

  async function doUndo(btn) {
    const ok = await confirmDialog({
      title: 'Uncheck today?',
      text: 'This removes today’s check-in. You can check in again any time today.',
      confirmLabel: 'Uncheck',
    });
    if (!ok) return;
    btn.disabled = true;
    try {
      await api.undoCheckIn(habit.id);
      toast('Today’s check-in removed');
      ctx.rerender();
    } catch (e) {
      toastError(e);
      btn.disabled = false;
    }
  }

  async function doArchive() {
    const ok = await confirmDialog({
      title: `Archive “${habit.name}”?`,
      text: 'It leaves Today and stops asking for check-ins. Its history is kept.',
      confirmLabel: 'Archive',
    });
    if (!ok) return;
    try {
      await api.archiveHabit(habit.id);
      toast('Archived. Its history stays.', 'success');
      location.hash = '#/';
    } catch (e) {
      toastError(e);
    }
  }

  async function stopSharing(g) {
    const ok = await confirmDialog({
      title: `Stop sharing with ${g.name}?`,
      text: 'The circle will no longer see this habit. Your check-ins and streak are untouched.',
      confirmLabel: 'Stop sharing',
    });
    if (!ok) return;
    try {
      await api.unshareHabit(g.id, habit.id);
      toast(`No longer shared with ${g.name}`);
      ctx.rerender();
    } catch (e) {
      toastError(e);
    }
  }

  const share = () => openShare({ habit, groups, alreadyShared: sharedIds, onDone: () => ctx.rerender() });

  const checkBtn = h('button', {
    type: 'button', class: 'btn btn-card', style: 'height:48px',
    onclick: () => (dates.has(today) ? doUndo(checkBtn) : doCheckIn(checkBtn)),
    title: dates.has(today) ? 'Click to uncheck today' : undefined,
  }, dates.has(today) ? 'Checked in today · Uncheck' : 'Check in today');

  const actions = isOwner && !archived ? [
    checkBtn,
    h('button', { type: 'button', class: 'btn btn-card', style: 'height:48px', onclick: doArchive }, 'Archive'),
    h('button', { type: 'button', class: 'btn btn-primary', style: 'height:48px', onclick: share }, 'Share with a circle'),
  ] : null;

  // ----- chips -----
  const chips = [
    sharedWith.length
      ? h('span', { class: `chip tinted ${famClass(fam)}` }, `Shared with ${sharedWith.map((g) => g.name).join(', ')}`)
      : chip('Private · not shared with anyone', 'priv'),
    chip(scheduleLong(habit)),
    archived ? chip('Archived', 'bad')
      : tw.done >= tw.target && tw.target > 0 ? chip('On target this week', 'good')
        : chip(`${cap(numberWord(left))} left this week`, 'open'),
  ];

  // ----- stat cards -----
  const n = progress.currentStreak || 0;
  const daysToGo = 6 - dayIndex(today);
  const stat = (famName, label, value, note) => h('div', { class: `stat ${famClass(famName)}` },
    h('div', { class: 'stat-label' }, label),
    h('div', { class: 'stat-value' }, value),
    h('div', { class: 'stat-note' }, note));
  const met12 = weeksMet(progress);
  const stats = h('div', { class: 'stats' },
    stat(fam, progress.streakUnit === 'WEEKS' ? 'Weeks on target' : 'Current streak', String(n),
      [progress.streakUnit === 'WEEKS' ? 'in a row' : streakUnitWord(progress, n), ps ? ps.text.toLowerCase() : null].filter(Boolean).join(' · ')),
    stat(fam === 'sky' ? 'plum' : 'sky', 'Best run', String(progress.longestStreak || 0),
      `${streakUnitWord(progress, progress.longestStreak || 0)}${progress.streakUnit === 'WEEKS' ? ' on target' : ''}`),
    stat(left === 0 ? 'mint' : 'coral', 'This week', h('span', null, String(tw.done), h('small', null, ` / ${tw.target}`)),
      left === 0 ? 'Target met' : `${cap(numberWord(left))} left, ${daysToGo === 0 ? 'last day' : `${numberWord(daysToGo)} ${plural(daysToGo, 'day')} to go`}`),
    stat(fam === 'mint' ? 'lilac' : 'mint', 'Last twelve weeks', h('span', null, String(met12), h('small', null, ` / ${(progress.weeks || []).length}`)),
      'weeks on target'));

  return h('div', { class: famClass(fam) },
    pageHead({
      crumb: [{ label: 'Today', href: '#/' }, { label: 'Habits' }],
      title: habit.name,
      chips,
      actions,
    }),
    !isOwner ? h('div', { class: 'mt-16' }, inlineError('You are viewing someone else’s shared habit. Only its owner can check in.')) : null,
    stats,
    heatmapCard(habit, dates, today, start, curMon, fam),
    h('div', { class: 'grid-3', style: 'margin-top:18px' },
      h('div', { class: 'span-2 stack' }, weeksCard(progress, habit, fam)),
      h('div', { class: 'stack' },
        scheduleCard(habit, fam),
        visibilityCard({ isOwner, sharedWith, stopSharing, share, archived }))));
}

function heatmapCard(habit, dates, today, start, curMon, fam) {
  const created = habit.createdAt ? localDateOf(habit.createdAt) : null;
  const target = weekTarget(habit);
  const cols = [];
  let metWeeks = 0;
  for (let w = 0; w < WEEKS; w++) {
    const mon = addDays(start, w * 7);
    let count = 0;
    const cells = [];
    for (let i = 0; i < 7; i++) {
      const d = addDays(mon, i);
      const scheduled = isScheduled(habit, d);
      const done = dates.has(d);
      if (done && scheduled) count++;
      let cls = 'hm';
      let label;
      if (d > today) { cls += ' future'; label = 'upcoming'; }
      else if (!scheduled) { cls += done ? ' extra' : ' off'; label = done ? 'checked in, not a scheduled day' : 'not scheduled'; }
      else if (done) { cls += ' done'; label = 'checked in'; }
      else label = 'no check-in';
      if (created && d < created && !done) cls += ' pre';
      cells.push(h('span', { class: cls, title: `${dayMonth(d)} · ${label}` }));
    }
    const met = target > 0 && count >= target;
    if (met) metWeeks++;
    const markerCls = met ? 'met' : mon === curMon ? 'open' : '';
    cols.push(h('div', { class: 'heat-col' },
      h('span', { class: `hm-marker ${markerCls}`, title: `Week of ${dayMonth(mon)}: ${Math.min(count, target)} / ${target}${met ? ', on target' : ''}` }),
      cells));
  }

  let hint;
  if (isQuota(habit)) {
    hint = `The strip on top marks weeks that met the target of ${numberWord(target)}. Because this habit is counted per week, one empty square is not a miss.`;
  } else if (habit.scheduleType === 'SPECIFIC_WEEKDAYS') {
    hint = 'The strip on top marks weeks where every chosen day was done. Outlined squares are days off, and they never break the streak.';
  } else {
    hint = 'The strip on top marks full weeks. Every day counts, so an empty square ends the streak.';
  }

  const lg = (cls, text) => h('span', { class: 'lg' }, h('span', { class: `hm ${cls}` }), text);
  return h('section', { class: `card ${famClass(fam)}`, style: 'margin-top:18px' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Last forty weeks'),
      h('div', { class: 'heat-legend' },
        lg('', 'None'), lg('done', 'Checked in'),
        habit.scheduleType === 'SPECIFIC_WEEKDAYS' ? lg('off', 'Day off') : null)),
    h('p', { class: 'hint', style: 'margin-top:10px' }, `${hint} ${metWeeks} of ${WEEKS} weeks on target.`),
    h('div', { class: 'heat-scroll' },
      h('div', { class: 'heat', role: 'img', 'aria-label': `Check-in heatmap for the last ${WEEKS} weeks, ${metWeeks} weeks on target` },
        h('div', { class: 'heat-labels' }, LETTERS.map((l, i) => h('span', null, i % 2 === 0 ? l : ''))),
        h('div', { class: 'heat-cols' }, cols))));
}

function weeksCard(progress, habit, fam) {
  const weeks = progress.weeks || [];
  const met = weeksMet(progress);
  return h('section', { class: `card ${famClass(fam)}` },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Weeks on target'),
      h('span', { class: 'hint' }, `${met} of ${weeks.length} · last twelve weeks`)),
    h('p', { class: 'hint', style: 'margin-top:6px' }, 'Counted against this habit’s own schedule, not against seven days. The outlined week is this one.'),
    weeks.length
      ? h('div', { class: 'weeks12' }, weeks.map((w, i) => {
        const frac = w.target ? Math.min(1, w.done / w.target) : 0;
        return h('div', { class: `wk ${w.met ? 'met' : ''} ${i === weeks.length - 1 ? 'current' : ''}`, title: `Week of ${dayMonth(w.weekStart)}: ${w.done} / ${w.target}${w.met ? ', on target' : ''}` },
          h('div', { class: 'wk-bar' }, h('span', { style: { height: `${(frac * 100).toFixed(0)}%` } })),
          h('span', { class: 'wk-num' }, `${w.done}/${w.target}`),
          h('span', { class: 'wk-date' }, shortDayMonth(w.weekStart)));
      }))
      : h('p', { class: 'hint mt-16' }, 'No weeks yet.'),
    h('p', { class: 'hint mt-16' }, streakText(progress, habit)));
}

function scheduleCard(habit, fam) {
  return h('section', { class: `card ${famClass(fam)}` },
    h('h2', { class: 'h2' }, 'Schedule'),
    h('p', { class: 'hint' }, 'How a week is scored'),
    scheduleOptions(habit).map((o) => h('div', { class: `opt ${o.type === habit.scheduleType ? 'on' : ''}`, 'aria-current': o.type === habit.scheduleType ? 'true' : null },
      h('span', { class: 'opt-mark', 'aria-hidden': 'true' }),
      h('div', null,
        h('div', { class: 'opt-title' }, o.label),
        h('span', { class: 'opt-note' }, o.note)))),
    h('p', { class: 'hint mt-12' }, 'Schedules can’t be changed after creation yet. Archive and recreate to switch.'));
}

function visibilityCard({ isOwner, sharedWith, stopSharing, share, archived }) {
  return h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Who can see it'),
    h('p', { class: 'hint' }, 'Sharing is visibility only. Nobody’s check-in counts for anybody else.'),
    sharedWith.length
      ? sharedWith.map((g) => h('div', { class: `list-row ${famClass(famFor(g.id))}` },
        h('span', { class: 'side-link', style: 'margin:0;padding:0;min-height:0' }, h('span', { class: 'dot' })),
        h('div', { class: 'grow' },
          h('a', { class: 't', href: `#/groups/${g.id}`, style: 'color:var(--ink)' }, g.name),
          h('div', { class: 's' }, `${g.members.length} ${plural(g.members.length, 'member')}`)),
        isOwner ? h('button', { type: 'button', class: 'btn-link danger', onclick: () => stopSharing(g) }, 'Stop sharing') : null))
      : h('div', { class: 'mt-12' }, emptyState({ small: true, text: 'Private · only you can see this.' })),
    isOwner && !archived
      ? h('button', { type: 'button', class: 'btn btn-secondary mt-16', style: 'width:100%', onclick: share }, 'Share with a circle')
      : null);
}
