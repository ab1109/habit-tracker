// Join a circle from an invite link (#/join/{token}).
import { h, famClass, famFor, field, emptyState, toast, inlineError } from '../ui.js';
import { api } from '../api.js';
import { currentIdentity } from '../identity.js';
import { dateTime } from '../dates.js';
import { numberWord, plural, cap } from '../schedule.js';
import { pageHead, groupsChanged } from '../components.js';

export async function renderJoin(ctx, token) {
  let invite;
  try {
    invite = await api.getInvite(token);
  } catch (e) {
    if (e.status !== 404 && e.status !== 400 && e.status !== 403) throw e;
    document.title = 'Invite link · Cohabit';
    return h('div', null,
      pageHead({ title: 'This invite doesn’t work' }),
      h('div', { class: 'card mt-20', style: 'max-width:620px' },
        emptyState({
          title: 'Invalid or expired link',
          text: `${String(e.message).replace(/\.$/, '')}. Invite links last seven days — ask whoever sent it for a fresh one.`,
          action: h('a', { class: 'btn btn-secondary', href: '#/' }, 'Back to Today'),
        })));
  }

  const fam = famFor(invite.groupId);
  const n = invite.memberCount || 0;
  document.title = `Join ${invite.groupName} · Cohabit`;

  if (invite.alreadyMember) {
    return h('div', { class: famClass(fam) },
      pageHead({ crumb: [{ label: 'Invite' }], title: `You’re already in ${invite.groupName}` }),
      h('section', { class: 'hero mt-20', style: 'max-width:620px' },
        h('span', { class: 'chip' }, `${cap(numberWord(n))} ${plural(n, 'member')}`),
        h('h2', { class: 'hero-title' }, invite.groupName),
        h('p', { class: 'hero-text' }, 'This link is for a circle you’ve already joined. Nothing to do.'),
        h('div', { class: 'row mt-20' },
          h('a', { class: 'btn btn-primary', href: `#/groups/${invite.groupId}` }, `Open ${invite.groupName}`))));
  }

  const me = currentIdentity();
  const nameInput = h('input', { class: 'input', value: me && me.name ? me.name : '', maxlength: '60', autocomplete: 'name' });
  const errorEl = h('div', { class: 'form-error', hidden: true });
  const joinBtn = h('button', { type: 'submit', class: 'btn btn-primary btn-lg' }, 'Join circle');

  const form = h('form', { class: 'hero mt-20', style: 'max-width:620px', novalidate: true },
    h('span', { class: 'chip' }, `${cap(numberWord(n))} ${plural(n, 'member')}`),
    h('h2', { class: 'hero-title' }, invite.groupName),
    h('p', { class: 'hero-text' },
      'You’ve been invited to this circle. Your habits stay private unless you share one here; joint habits belong to the whole group.'),
    h('div', { class: 'mt-20', style: 'max-width:380px' },
      field('Your name in this circle', nameInput, 'This is what the others will see next to your check-ins.')),
    errorEl,
    h('div', { class: 'row mt-20' }, joinBtn),
    invite.expiresAt ? h('p', { class: 'hero-text', style: 'font-size:12px' }, `Link valid until ${dateTime(invite.expiresAt)}.`) : null);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorEl.hidden = true;
    const name = nameInput.value.trim();
    if (!name) {
      errorEl.replaceChildren(inlineError('Add the name the circle will see.'));
      errorEl.hidden = false;
      nameInput.focus();
      return;
    }
    joinBtn.disabled = true;
    try {
      const group = await api.acceptInvite(token, name);
      toast(`Welcome to ${group.name}`, 'success');
      groupsChanged();
      location.hash = `#/groups/${group.id}`;
    } catch (err) {
      errorEl.replaceChildren(inlineError(err.message));
      errorEl.hidden = false;
      joinBtn.disabled = false;
    }
  });

  return h('div', { class: famClass(fam) },
    pageHead({ crumb: [{ label: 'Invite' }], title: `Join ${invite.groupName}` }),
    form);
}
