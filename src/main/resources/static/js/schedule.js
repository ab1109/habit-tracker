// Schedule + streak wording. Everything is derived from HabitResponse / HabitProgress.
import { WEEKDAYS, SHORT_DAYS, dayIndex, localDateOf, mondayOf, todayISO } from './dates.js';

const WORDS = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
  'eleven', 'twelve'];

export function numberWord(n) {
  return n >= 0 && n < WORDS.length ? WORDS[n] : String(n);
}

export function cap(s) {
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
}

export function plural(n, word, pluralWord) {
  return n === 1 ? word : (pluralWord || word + 's');
}

function sortedDays(habit) {
  return (habit.weekdays || [])
    .map((d) => WEEKDAYS.indexOf(d))
    .filter((i) => i >= 0)
    .sort((a, b) => a - b);
}

export function isScheduled(habit, iso) {
  if (habit.scheduleType === 'SPECIFIC_WEEKDAYS') {
    return (habit.weekdays || []).includes(WEEKDAYS[dayIndex(iso)]);
  }
  return true;
}

export function weekTarget(habit) {
  if (habit.scheduleType === 'DAILY') return 7;
  if (habit.scheduleType === 'N_TIMES_PER_WEEK') return habit.timesPerWeek || 0;
  return (habit.weekdays || []).length;
}

export function isQuota(habit) {
  return habit.scheduleType === 'N_TIMES_PER_WEEK';
}

function dayRun(idx, names, sep) {
  if (idx.length >= 3 && idx[idx.length - 1] - idx[0] === idx.length - 1) {
    return `${names[idx[0]]}–${names[idx[idx.length - 1]]}`;
  }
  return idx.map((i) => names[i]).join(sep);
}

/** "EVERY DAY", "4× A WEEK", "MON–FRI", "MON · WED · FRI" */
export function scheduleChip(habit) {
  switch (habit.scheduleType) {
    case 'DAILY': return 'EVERY DAY';
    case 'N_TIMES_PER_WEEK': return `${habit.timesPerWeek}× A WEEK`;
    case 'SPECIFIC_WEEKDAYS': return dayRun(sortedDays(habit), SHORT_DAYS, ' · ');
    default: return '';
  }
}

/** "Every day", "Four times a week", "Mon–Fri", "Mon, Wed, Fri" */
export function scheduleLong(habit) {
  switch (habit.scheduleType) {
    case 'DAILY': return 'Every day';
    case 'N_TIMES_PER_WEEK': return timesAWeek(habit.timesPerWeek);
    case 'SPECIFIC_WEEKDAYS': {
      const names = SHORT_DAYS.map((d) => d.charAt(0) + d.slice(1).toLowerCase());
      return dayRun(sortedDays(habit), names, ', ');
    }
    default: return '';
  }
}

export function timesAWeek(n) {
  if (n === 1) return 'Once a week';
  if (n === 2) return 'Twice a week';
  return `${cap(numberWord(n))} times a week`;
}

/** Longer explanation of how a week is scored, adapted from the mock's Schedule panel. */
export function scheduleOptions(habit) {
  const n = habit.timesPerWeek;
  return [
    { type: 'DAILY', label: 'Every day', note: 'Seven squares. A missing day ends the streak.' },
    {
      type: 'N_TIMES_PER_WEEK',
      label: habit.scheduleType === 'N_TIMES_PER_WEEK' ? timesAWeek(n) : 'A number of times a week',
      note: habit.scheduleType === 'N_TIMES_PER_WEEK'
        ? `${cap(numberWord(n))} ${plural(n, 'capsule')}. Any ${numberWord(n)} ${plural(n, 'day')} count, in any order.`
        : 'Capsules. Any N days count, in any order.',
    },
    {
      type: 'SPECIFIC_WEEKDAYS',
      label: habit.scheduleType === 'SPECIFIC_WEEKDAYS' ? `Chosen weekdays · ${scheduleLong(habit)}` : 'Chosen weekdays',
      note: 'Only the days you pick are scored. The rest show as a dash.',
    },
  ];
}

/** The first local date a habit could have been done on; days before it are never "missed". */
export function trackedFrom(habit) {
  return habit && habit.createdAt ? localDateOf(habit.createdAt) : null;
}

/** Last week fell short of its target — only counts if the habit already existed before this week. */
export function missedLastWeek(progress, habit) {
  const w = (progress && progress.weeks) || [];
  if (w.length < 2 || w[w.length - 2].met) return false;
  const from = trackedFrom(habit);
  return !from || from < mondayOf(todayISO());
}

/** "6 day streak", "6 weeks on target", "4 scheduled days in a row", "Target missed last week" */
export function streakText(progress, habit) {
  if (!progress) return '';
  const n = progress.currentStreak || 0;
  switch (progress.streakUnit) {
    case 'WEEKS': {
      if (n > 0) return `${n} ${plural(n, 'week')} on target`;
      if (missedLastWeek(progress, habit)) return 'Target missed last week';
      return 'No weeks on target yet';
    }
    case 'SCHEDULED_DAYS':
      return n > 0 ? `${n} scheduled ${plural(n, 'day')} in a row` : 'No streak yet';
    default:
      return n > 0 ? `${n} day streak` : 'No streak yet';
  }
}

/** Short state of the current day/week: "Today open", "This week open", "Done today", "This week met". */
export function periodState(progress) {
  if (!progress) return null;
  const weekly = progress.streakUnit === 'WEEKS';
  if (progress.currentPeriodMet) return { kind: 'met', text: weekly ? 'This week met' : 'Done today' };
  if (progress.currentPeriodOpen) return { kind: 'open', text: weekly ? 'This week open' : 'Today open' };
  return null;
}

export function streakUnitWord(progress, n) {
  switch (progress.streakUnit) {
    case 'WEEKS': return plural(n, 'week');
    case 'SCHEDULED_DAYS': return `scheduled ${plural(n, 'day')}`;
    default: return plural(n, 'day');
  }
}

export function weeksMet(progress) {
  return (progress && progress.weeks ? progress.weeks : []).filter((w) => w.met).length;
}
