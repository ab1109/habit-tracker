// Circle (#/groups/{id}): members, shared habits per member, joint habits, consistency,
// activity feed and a preview of the weekly digest.
import { h, avatar, famClass, famFor, pips, bar, chip, emptyState, toast, toastError, confirmDialog, inlineError } from '../ui.js';
import { api } from '../api.js';
import { currentIdentity } from '../identity.js';
import { todayISO, shortDayMonth, dayMonth, localDateOf } from '../dates.js';
import { scheduleChip, streakText, missedLastWeek, periodState, weeksMet, numberWord, plural, cap } from '../schedule.js';
import { pageHead, openInvite, openNewJointHabit, openShare, groupsChanged } from '../components.js';
import { feedItem } from './today.js';

export async function renderCircle(ctx, gid) {
  const me = currentIdentity();
  const [group, progress, digest] = await Promise.all([
    api.getGroup(gid),
    api.groupProgress(gid),
    api.digest(gid).then((d) => ({ d }), (e) => ({ e })),
  ]);
  const fam = famFor(gid);
  const today = todayISO();
  const members = [...group.members].sort((a, b) =>
    (a.userId === me.id ? -1 : b.userId === me.id ? 1 : String(a.joinedAt).localeCompare(String(b.joinedAt))));
  const shared = progress.sharedHabits || [];
  const joint = progress.jointHabits || [];
  const isCreator = group.createdBy === me.id;
  document.title = `${group.name} · Cohabit`;

  const invite = () => openInvite(group, () => { ctx.rerender(); groupsChanged(); });
  const newJoint = () => openNewJointHabit(group, (habit) => { location.hash = `#/groups/${gid}/habits/${habit.id}`; });
  const shareMine = async () => {
    try {
      const habits = await api.listHabits();
      const mineHere = new Set(shared.filter((s) => s.ownerUserId === me.id).map((s) => s.habit.id));
      openShare({ group, habits, alreadyShared: mineHere, onDone: () => ctx.rerender() });
    } catch (e) {
      toastError(e);
    }
  };

  async function stopSharing(habit) {
    const ok = await confirmDialog({
      title: `Stop sharing “${habit.name}”?`,
      text: `${group.name} will no longer see it. Your check-ins and streak are untouched.`,
      confirmLabel: 'Stop sharing',
    });
    if (!ok) return;
    try {
      await api.unshareHabit(gid, habit.id);
      toast('No longer shared here');
      ctx.rerender();
    } catch (e) { toastError(e); }
  }

  async function leave() {
    const ok = await confirmDialog({
      title: `Leave ${group.name}?`,
      text: 'Your habits stay yours; they just stop being visible here. Someone can invite you back with your user id.',
      confirmLabel: 'Leave circle',
    });
    if (!ok) return;
    try {
      await api.removeMember(gid, me.id);
      toast(`You left ${group.name}`);
      groupsChanged();
      location.hash = '#/';
    } catch (e) { toastError(e); }
  }

  async function remove(m) {
    const ok = await confirmDialog({
      title: `Remove ${m.displayName}?`,
      text: `They will stop seeing ${group.name}. Their own habits and check-ins are not touched.`,
      confirmLabel: 'Remove',
    });
    if (!ok) return;
    try {
      await api.removeMember(gid, m.userId);
      toast(`${m.displayName} was removed`);
      ctx.rerender();
    } catch (e) { toastError(e); }
  }

  // ----- header -----
  const n = members.length;
  const stack = h('div', { class: 'avatar-stack', 'aria-label': members.map((m) => m.displayName).join(', ') },
    members.slice(0, 5).map((m) => avatar(m.displayName, m.userId, 'lg')),
    n > 5 ? h('span', { class: 'avatar lg' }, `+${n - 5}`) : null);

  // ----- hero -----
  const left = Math.max(0, progress.totalTarget - progress.totalDone);
  const hero = h('section', { class: `hero ${famClass(fam)}` },
    h('div', { class: 'hero-top' },
      h('div', null,
        h('span', { class: 'chip' }, `This week · ${shortDayMonth(progress.weekStart)} – ${shortDayMonth(progress.weekEnd)}`),
        h('h2', { class: 'hero-title' }, 'Across the circle'),
        h('p', { class: 'hero-text' }, progress.totalTarget === 0
          ? 'Nothing to count yet. Share one of your habits here, or give the circle a joint habit that anyone can cover.'
          : left === 0
            ? 'Every shared and joint habit is on target this week.'
            : `${cap(numberWord(left))} to go. Shared habits keep their own check-ins and streaks; a joint habit counts once for everyone.`)),
      h('div', null,
        h('div', { class: 'hero-num' }, String(progress.totalDone), h('span', null, ` / ${progress.totalTarget}`)),
        h('div', { class: 'hero-numsub' }, `${progress.totalDone} of ${progress.totalTarget} across the circle this week`))),
    bar(progress.totalTarget ? progress.totalDone / progress.totalTarget : 0, 'thick on-tint'));

  // ----- shared habits, grouped per member -----
  const byOwner = new Map();
  for (const s of shared) {
    if (!byOwner.has(s.ownerUserId)) byOwner.set(s.ownerUserId, []);
    byOwner.get(s.ownerUserId).push(s);
  }
  const memberCards = members.map((m) => {
    const mine = m.userId === me.id;
    const list = byOwner.get(m.userId) || [];
    const mfam = famFor(m.userId);
    let summary;
    if (list.length === 0) summary = mine ? 'You haven’t shared anything here' : 'Nothing shared yet';
    else if (list.length === 1) summary = streakText(list[0].progress, list[0].habit);
    else summary = `${list.length} shared habits`;
    return h('div', { class: `member-card ${famClass(mfam)}` },
      h('div', { class: 'member-top' },
        avatar(m.displayName, m.userId, 'lg'),
        h('div', { style: 'min-width:0' },
          h('div', { class: 'member-name' }, m.displayName, mine ? h('span', { class: 'muted', style: 'font-weight:500' }, ' (you)') : null),
          h('div', { class: 'member-sub' }, summary))),
      list.map((s) => {
        const p = s.progress;
        const tw = p.thisWeek;
        const ps = periodState(p);
        const behind = !p.currentPeriodMet && missedLastWeek(p, s.habit);
        return h('div', { class: 'shared-item' },
          h('div', { class: 'top' },
            mine ? h('a', { class: 'hn', href: `#/habits/${s.habit.id}`, style: 'color:var(--ink)' }, s.habit.name) : h('span', { class: 'hn' }, s.habit.name),
            h('span', { class: 'sched-chip', style: 'background:var(--fill)' }, scheduleChip(s.habit))),
          pips(tw.done, tw.target),
          h('div', { class: 'bottom' },
            h('span', null, `${tw.done} of ${tw.target} this week`),
            behind ? chip('Behind', 'bad chip-sm') : ps ? chip(ps.text, `${ps.kind === 'met' ? 'good' : 'open'} chip-sm`) : null),
          list.length > 1 ? h('div', { class: 'member-sub', style: 'margin-top:6px' }, streakText(p, s.habit)) : null,
          mine ? h('button', { type: 'button', class: 'btn-link danger', style: 'margin-top:8px', onclick: () => stopSharing(s.habit) }, 'Stop sharing') : null);
      }));
  });

  const sharedSection = h('section', null,
    h('div', { class: 'section-head' },
      h('div', null,
        h('h2', { class: 'h2' }, 'Shared for visibility'),
        h('p', { class: 'hint', style: 'margin-top:4px' }, 'Each person keeps their own check-ins and their own streak. Capsules are this week’s target.')),
      h('button', { type: 'button', class: 'btn btn-card btn-sm', onclick: shareMine }, 'Share one of yours')),
    h('div', { class: 'member-grid' },
      memberCards,
      h('button', { type: 'button', class: 'invite-card', onclick: invite }, '+ Invite someone')));

  // ----- joint habits -----
  const jointSection = h('section', null,
    h('div', { class: 'section-head' },
      h('div', null,
        h('h2', { class: 'h2' }, 'Joint habits'),
        h('p', { class: 'hint', style: 'margin-top:4px' }, 'Owned by the circle. One check-in from anyone closes the day for everybody.')),
      h('button', { type: 'button', class: 'btn btn-card btn-sm', onclick: newJoint }, 'New joint habit')),
    joint.length
      ? h('div', { class: 'joint-grid' }, joint.map((j) => {
        const tw = j.progress.thisWeek;
        const ps = periodState(j.progress);
        return h('a', { class: `joint-card ${famClass(fam)}`, href: `#/groups/${gid}/habits/${j.habit.id}` },
          h('span', { class: 'chip', style: 'background:var(--card);color:var(--deep)' }, `Joint habit · ${scheduleChip(j.habit)}`),
          h('div', { class: 'jn' }, j.habit.name),
          h('div', { class: 'js' }, `${tw.done} of ${tw.target} this week · ${streakText(j.progress, j.habit)}${ps ? ` · ${ps.text}` : ''}`),
          pips(tw.done, tw.target, true),
          (j.coverage || []).length
            ? h('div', { class: 'cov' }, j.coverage.map((c) => h('span', { class: 'chip chip-sm' }, `${c.displayName} × ${c.count}`)))
            : h('div', { class: 'js', style: 'margin-top:10px' }, 'Nobody has covered it this week yet.'));
      }))
      : emptyState({
        small: true,
        title: 'No joint habits yet',
        text: 'Hand a habit to the whole group so any single check-in covers the day for everyone.',
        action: h('button', { type: 'button', class: 'btn btn-primary', onclick: newJoint }, 'New joint habit'),
      }));

  // ----- right column -----
  const membersCard = h('section', { class: 'card' },
    h('div', { class: 'card-head' },
      h('h2', { class: 'h2' }, 'Members'),
      h('span', { class: 'hint' }, `${n} ${plural(n, 'member')}`)),
    members.map((m) => h('div', { class: 'member-list-row' },
      avatar(m.displayName, m.userId),
      h('div', { class: 'grow' },
        h('div', { style: 'font-size:14px;font-weight:500' }, m.displayName, m.userId === me.id ? ' (you)' : ''),
        h('div', { class: 'member-sub' }, [
          m.userId === group.createdBy ? 'Started the circle' : null,
          m.joinedAt ? `joined ${dayMonth(localDateOf(m.joinedAt))}` : null,
        ].filter(Boolean).join(' · '))),
      isCreator && m.userId !== me.id
        ? h('button', { type: 'button', class: 'btn-link danger', onclick: () => remove(m) }, 'Remove')
        : null)),
    h('div', { class: 'row mt-16', style: 'justify-content:space-between' },
      h('button', { type: 'button', class: 'btn-link', onclick: invite }, '+ Invite someone'),
      h('button', { type: 'button', class: 'btn-link danger', onclick: leave }, 'Leave circle')));

  const rows = [
    ...shared.map((s) => ({ name: s.ownerDisplayName, sub: s.habit.name, p: s.progress, id: s.ownerUserId })),
    ...joint.map((j) => ({ name: j.habit.name, sub: 'Joint habit', p: j.progress, id: gid })),
  ].map((r) => ({ ...r, met: weeksMet(r.p), total: (r.p.weeks || []).length }))
    .sort((a, b) => b.met - a.met);
  const consistencyCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Consistency'),
    h('p', { class: 'hint' }, 'Weeks the target was met, last twelve'),
    rows.length
      ? rows.map((r) => {
        const frac = r.total ? r.met / r.total : 0;
        return h('div', { class: `metric-row ${famClass(fam)}` },
          h('div', { class: 'top' },
            h('span', { class: 'name' }, r.name, h('small', null, r.sub)),
            h('span', { class: 'val' }, `${r.met} of ${r.total}`)),
          bar(frac, 'thin', frac >= 0.75 ? '' : frac >= 0.5 ? 'mid' : 'low'));
      })
      : h('p', { class: 'hint mt-12' }, 'Nothing to measure yet.'));

  const activity = progress.recentActivity || [];
  const activityCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Activity'),
    activity.length
      ? activity.map((a) => feedItem(a, me, today))
      : h('p', { class: 'hint mt-12' }, 'No check-ins yet. They’ll appear here as they land.'));

  const digestCard = h('section', { class: 'card' },
    h('h2', { class: 'h2' }, 'Weekly digest'),
    h('p', { class: 'hint' }, 'Preview of Sunday’s summary. It goes out at 18:00 in each member’s own timezone.'),
    digest.e ? h('div', { class: 'mt-12' }, inlineError(digest.e.message)) : digestBody(digest.d));

  return h('div', { class: famClass(fam) },
    pageHead({
      crumb: [{ label: 'Circles' }],
      title: group.name,
      sub: `${cap(numberWord(n))} ${plural(n, 'member')} · ${shared.length} shared ${plural(shared.length, 'habit')} · ${joint.length} joint`,
      actions: [
        stack,
        h('button', { type: 'button', class: 'btn btn-card', style: 'height:48px', onclick: invite }, 'Invite'),
      ],
    }),
    h('div', { class: 'grid-3', style: 'margin-top:20px' },
      h('div', { class: 'span-2 stack' }, hero, sharedSection, jointSection),
      h('div', { class: 'stack' }, membersCard, consistencyCard, activityCard, digestCard)));
}

function digestBody(d) {
  const group = (title, items) => items && items.length
    ? h('div', { class: 'digest-group' }, h('h4', null, title), h('div', { class: 'row' }, items))
    : null;
  const any = (d.onTarget || []).length || (d.behind || []).length || (d.uncoveredJointHabits || []).length;
  return h('div', null,
    h('div', { class: 'digest-subject' }, d.subject),
    h('div', { class: 'hint' }, `${dayMonth(d.weekStart)} – ${dayMonth(d.weekEnd)}`),
    group('Held their streak', (d.onTarget || []).map((o) => chip(`${o.displayName} · ${o.habitName}`, 'good chip-sm'))),
    group('Quietly dropped off', (d.behind || []).map((b) => chip(`${b.displayName} · ${b.habitName} · ${b.done}/${b.target}`, 'bad chip-sm'))),
    group('Joint habits that went uncovered', (d.uncoveredJointHabits || []).map((u) =>
      chip(`${u.habitName} · ${u.uncoveredDays} ${plural(u.uncoveredDays, 'day')}`, 'open chip-sm'))),
    !any ? h('p', { class: 'hint mt-12' }, 'Nothing to report yet this week.') : null,
    d.body ? h('details', { class: 'body-text' }, h('summary', null, 'Email text'), h('pre', null, d.body)) : null);
}

