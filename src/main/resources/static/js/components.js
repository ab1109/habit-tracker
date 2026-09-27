// Shared page pieces and the create/share/invite dialogs used by several views.
import { h, icon, openModal, field, toast, famClass, famFor } from './ui.js';
import { api } from './api.js';
import { WEEKDAYS, SHORT_DAYS } from './dates.js';
import { themeSwitch } from './theme.js';
import { currentIdentity, UUID_RE } from './identity.js';

export function groupsChanged() {
  window.dispatchEvent(new CustomEvent('cohabit:groups-changed'));
}

/** Page header with breadcrumb/title/sub/chips on the left and actions + global controls on the right. */
export function pageHead({ crumb, title, sub, chips, actions }) {
  const crumbEl = crumb && crumb.length
    ? h('div', { class: 'crumb' }, crumb.map((c, i) => [
      i > 0 ? ' / ' : null,
      c.href ? h('a', { href: c.href }, c.label) : c.label,
    ]))
    : null;
  return h('header', { class: 'page-head' },
    h('div', { class: 'page-head-text' },
      crumbEl,
      h('h1', { class: 'title' }, title),
      sub && h('p', { class: 'sub' }, sub),
      chips && chips.length ? h('div', { class: 'head-chips' }, chips) : null),
    h('div', { class: 'head-actions' },
      themeSwitch(),
      h('a', {
        class: `icon-btn ${location.hash.startsWith('#/notifications') ? 'active' : ''}`,
        href: '#/notifications', 'aria-label': 'Notifications', title: 'Notifications',
      }, icon('bell', 19, 1.7)),
      actions));
}

// ---------- habit form ----------
function titleDay(i) {
  const s = SHORT_DAYS[i];
  return s.charAt(0) + s.slice(1).toLowerCase();
}

/** Returns { el, read() } — read() validates and returns the create-habit body. */
export function habitForm({ placeholder = 'Name this habit' } = {}) {
  let type = 'DAILY';
  let times = 3;
  const days = new Set(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']);
  const radios = {};

  const nameInput = h('input', {
    class: 'input', name: 'name', placeholder, maxlength: '120', autocomplete: 'off', required: true,
  });

  const select = (value) => {
    type = value;
    radios[value].checked = true;
  };

  const stepBtns = [1, 2, 3, 4, 5, 6, 7].map((n) => h('button', {
    type: 'button', 'aria-label': `${n} ${n === 1 ? 'time' : 'times'} a week`,
    onclick: () => { times = n; sync(); select('N_TIMES_PER_WEEK'); },
  }, String(n)));

  const dayBtns = WEEKDAYS.map((d, i) => h('button', {
    type: 'button', 'aria-label': d.charAt(0) + d.slice(1).toLowerCase(),
    onclick: () => { if (days.has(d)) days.delete(d); else days.add(d); sync(); select('SPECIFIC_WEEKDAYS'); },
  }, titleDay(i)));

  function sync() {
    stepBtns.forEach((b, i) => b.setAttribute('aria-pressed', String(i + 1 === times)));
    dayBtns.forEach((b, i) => b.setAttribute('aria-pressed', String(days.has(WEEKDAYS[i]))));
  }
  sync();

  const radio = (value, label, note, extra) => {
    radios[value] = h('input', {
      type: 'radio', name: 'scheduleType', value, checked: value === type,
      onchange: () => { type = value; },
    });
    return h('label', { class: 'radio-card' },
      radios[value],
      h('div', { class: 'grow' },
        h('div', { class: 'opt-title' }, label),
        h('span', { class: 'opt-note' }, note),
        extra && h('div', { class: 'extra' }, extra)));
  };

  const el = h('div', null,
    field('Name', nameInput),
    h('div', { class: 'field', role: 'radiogroup', 'aria-label': 'Schedule' },
      h('span', { class: 'label' }, 'Schedule'),
      radio('DAILY', 'Every day', 'Seven squares. A missing day ends the streak.'),
      radio('N_TIMES_PER_WEEK', 'A number of times a week', 'Capsules. Any N days count, in any order.',
        h('div', { class: 'stepper', role: 'group', 'aria-label': 'Times per week' }, stepBtns)),
      radio('SPECIFIC_WEEKDAYS', 'Chosen weekdays', 'Only the days you pick are scored. The rest show as a dash.',
        h('div', { class: 'wd-toggle', role: 'group', 'aria-label': 'Weekdays' }, dayBtns))));

  function read() {
    const name = nameInput.value.trim();
    if (!name) {
      nameInput.focus();
      throw new Error('Give the habit a name.');
    }
    const body = { name, scheduleType: type };
    if (type === 'N_TIMES_PER_WEEK') body.timesPerWeek = times;
    if (type === 'SPECIFIC_WEEKDAYS') {
      if (days.size === 0) throw new Error('Pick at least one weekday.');
      body.weekdays = WEEKDAYS.filter((d) => days.has(d));
    }
    return body;
  }

  return { el, read };
}

// ---------- dialogs ----------
export function openNewHabit(onCreated) {
  const form = habitForm();
  openModal({
    title: 'New habit',
    sub: 'Start private, and stay private if you like. You can share it with a circle later.',
    content: form.el,
    submitLabel: 'Create habit',
    fam: 'lilac',
    onSubmit: async () => {
      const habit = await api.createHabit(form.read());
      toast(`Added “${habit.name}”`, 'success');
      if (onCreated) onCreated(habit);
    },
  });
}

export function openNewJointHabit(group, onCreated) {
  const form = habitForm({ placeholder: 'Kitchen reset' });
  openModal({
    title: 'New joint habit',
    sub: `Owned by ${group.name}. One check-in from anyone in the circle closes the day for everybody.`,
    content: form.el,
    submitLabel: 'Create joint habit',
    fam: famFor(group.id),
    onSubmit: async () => {
      const habit = await api.createJointHabit(group.id, form.read());
      toast(`“${habit.name}” now belongs to ${group.name}`, 'success');
      if (onCreated) onCreated(habit);
    },
  });
}

export function openNewCircle() {
  const me = currentIdentity();
  const name = h('input', { class: 'input', placeholder: 'Morning Runners', maxlength: '80', autocomplete: 'off' });
  const display = h('input', { class: 'input', value: me ? me.name : '', maxlength: '60', autocomplete: 'off' });
  openModal({
    title: 'New circle',
    sub: 'A circle is a small group. Share a habit with it for the company, or give it a joint habit that anyone can cover.',
    content: h('div', null,
      field('Circle name', name),
      field('Your name in this circle', display, 'This is what the others will see next to your check-ins.')),
    submitLabel: 'Create circle',
    onSubmit: async () => {
      if (!name.value.trim()) throw new Error('Give the circle a name.');
      if (!display.value.trim()) throw new Error('Add the name the circle will see.');
      const group = await api.createGroup(name.value.trim(), display.value.trim());
      toast(`${group.name} is ready. Invite someone next.`, 'success');
      groupsChanged();
      location.hash = `#/groups/${group.id}`;
    },
  });
}

export function openInvite(group, onDone) {
  const uid = h('input', { class: 'input mono', placeholder: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx', autocomplete: 'off', spellcheck: 'false' });
  const display = h('input', { class: 'input', placeholder: 'Dev Kapoor', maxlength: '60', autocomplete: 'off' });
  openModal({
    title: `Invite someone to ${group.name}`,
    sub: 'There are no accounts yet, so invites go by user id. Ask them to copy theirs from their profile menu.',
    content: h('div', null,
      field('Their user id', uid),
      field('Their name in this circle', display)),
    submitLabel: 'Invite',
    fam: famFor(group.id),
    onSubmit: async () => {
      const id = uid.value.trim().toLowerCase();
      if (!UUID_RE.test(id)) throw new Error('That does not look like a user id (a UUID).');
      if (!display.value.trim()) throw new Error('Add a display name for them.');
      await api.invite(group.id, id, display.value.trim());
      toast(`${display.value.trim()} is in ${group.name}`, 'success');
      if (onDone) onDone();
    },
  });
}

/**
 * Share one of my personal habits with a circle.
 * Either pass a fixed habit and choose among groups, or a fixed group and choose among habits.
 */
export function openShare({ habit, groups, group, habits, alreadyShared = new Set(), onDone }) {
  const choosingGroup = !!habit;
  const items = choosingGroup ? groups : habits;
  const available = items.filter((it) => !alreadyShared.has(it.id));
  let content;
  if (available.length === 0) {
    content = h('p', { class: 'hint' }, choosingGroup
      ? (items.length ? 'It is already shared with every circle you are in.' : 'You are not in any circle yet. Create one from the sidebar first.')
      : (items.length ? 'All your habits are already shared here.' : 'You have no personal habits yet. Create one on Today first.'));
  } else {
    content = h('div', { role: 'radiogroup' }, available.map((it, i) =>
      h('label', { class: `radio-card ${choosingGroup ? famClass(famFor(it.id)) : 'fam-lilac'}` },
        h('input', { type: 'radio', name: 'target', value: it.id, checked: i === 0 }),
        h('div', { class: 'grow' },
          h('div', { class: 'opt-title' }, it.name),
          h('span', { class: 'opt-note' }, choosingGroup
            ? `${it.members.length} ${it.members.length === 1 ? 'member' : 'members'}`
            : 'Your own habit')))));
  }
  openModal({
    title: choosingGroup ? `Share “${habit.name}”` : `Share a habit with ${group.name}`,
    sub: 'Sharing is visibility only. Each person keeps their own check-ins and their own streak.',
    content,
    submitLabel: available.length ? 'Share' : null,
    onSubmit: async (form) => {
      const chosen = form.querySelector('input[name=target]:checked');
      if (!chosen) throw new Error('Pick one first.');
      const gid = choosingGroup ? chosen.value : group.id;
      const hid = choosingGroup ? habit.id : chosen.value;
      await api.shareHabit(gid, hid);
      const label = choosingGroup ? groups.find((g) => g.id === gid).name : group.name;
      toast(`Shared with ${label}`, 'success');
      if (onDone) onDone();
    },
  });
}
