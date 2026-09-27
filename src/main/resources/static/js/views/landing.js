// Public landing page (from the mock's Landing board). Shown signed-out in GOOGLE mode (it is the
// sign-in screen) and, in DEV_HEADER mode, before a local identity exists.
// The circle preview is a clearly labelled illustration; everything else describes real features.
import { h, icon, famClass } from '../ui.js';

const SAMPLE = [
  { name: 'Maya', initials: 'MR', done: 3, fam: 'sky' },
  { name: 'Dev', initials: 'DK', done: 4, fam: 'plum' },
  { name: 'Priya', initials: 'PS', done: 4, fam: 'mint' },
  { name: 'Sam', initials: 'SA', done: 3, fam: 'lilac' },
];

const svg = (paths, stroke = 'currentColor', size = 26, vb = 24) => h('span', {
  'aria-hidden': 'true', style: 'display:inline-flex',
  svg: `<svg width="${size}" height="${size}" viewBox="0 0 ${vb} ${vb}" fill="none" stroke="${stroke}" stroke-width="1.7">${paths}</svg>`,
});

const GOOGLE_G = '<svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true"><path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z"/><path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/><path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z"/><path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z"/></svg>';

/**
 * @param {{ google: boolean, loginUrl: string, invited: boolean, onStart: () => void }} opts
 *   google: GOOGLE mode (CTAs link to loginUrl); otherwise DEV_HEADER (CTAs call onStart).
 */
export function renderLanding({ google, loginUrl, invited, onStart }) {
  // Every call to action goes to Google sign-in (GOOGLE) or the local identity setup (DEV_HEADER).
  const cta = (label, cls, withG) => (google
    ? h('a', { class: cls, href: loginUrl }, withG ? h('span', { svg: GOOGLE_G, style: 'display:inline-flex' }) : null, label)
    : h('button', { type: 'button', class: cls, onclick: onStart }, label));

  const scrollTo = (id) => (e) => {
    e.preventDefault(); // keep the hash router out of it
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const header = h('header', { class: 'lp-header' },
    h('span', { class: 'brand' },
      h('span', { class: 'brand-mark lp-mark' }, icon('logo', 18, 1.9)),
      h('span', { class: 'brand-name lp-brand' }, 'Cohabit')),
    h('nav', { class: 'lp-nav', 'aria-label': 'Page sections' },
      h('a', { href: '#how', onclick: scrollTo('lp-how') }, 'How it works'),
      h('a', { href: '#circles', onclick: scrollTo('lp-circles') }, 'Circles')),
    h('div', { class: 'lp-header-actions' },
      cta('Log in', 'lp-login'),
      cta('Start a circle', 'btn btn-primary lp-start')));

  const banner = invited
    ? h('div', { class: 'lp-banner', role: 'status' }, icon('info', 18),
      google
        ? 'You’ve been invited to join a circle. Sign in to accept.'
        : 'You’ve been invited to join a circle. Set up a local identity to accept.')
    : null;

  const hero = h('section', { class: 'lp-hero' },
    h('div', { class: 'lp-hero-text' },
      banner,
      h('span', { class: 'lp-pill' }, 'Habit tracking, together'),
      h('h1', { class: 'lp-title' }, 'Habits you', h('br'), 'share a roof with.'),
      h('p', { class: 'lp-lede' }, 'Start private, and stay private if you like. Share one habit with a circle for the company, or hand it to the whole group so any single check-in covers the day for everyone.'),
      h('div', { class: 'lp-cta' },
        cta(google ? 'Continue with Google' : 'Get started', `btn btn-primary btn-lg ${google ? 'lp-google' : ''}`, google),
        h('p', { class: 'lp-fine' }, google
          ? 'Free while in beta. Sign in with your Google account.'
          : 'Local development mode: no sign-in needed.'))),
    previewCard());

  const features = h('section', { class: 'lp-section', id: 'lp-circles' },
    h('div', { class: 'lp-features' },
      feature('sky', svg('<circle cx="9" cy="12" r="6"/><circle cx="15" cy="12" r="6"/>'), 'Private, shared, or joint',
        'Keep a habit to yourself, let a circle watch the streak, or make it the group’s so one person’s check-in completes that day for everybody.'),
      feature('lilac', svg('<path d="M3 15c3 0 3-6 6-6s3 6 6 6 3-6 6-6" stroke-linecap="round"/>'), 'Three ways to count',
        'Every day, a number of times a week, or a fixed set of weekdays. Each one is scored on its own terms instead of pretending it’s daily.'),
      feature('coral', svg('<rect x="3.5" y="5.5" width="17" height="13" rx="3"/><path d="m4.5 7.5 7.5 5.5 7.5-5.5" stroke-linejoin="round"/>'), 'One digest a week',
        'Sunday evening, every circle gets a single summary: who held their streak, who quietly dropped off, and which joint habits went uncovered.')));

  const steps = [
    { n: '1', fam: 'sky', title: 'Create a habit', text: 'Pick one of three schedules.', bullets: [
      ['Every day', 'A missing day ends the streak.'],
      ['A number of times a week', 'Any N days count, in any order.'],
      ['Chosen weekdays', 'Only the days you pick are scored.'],
    ] },
    { n: '2', fam: 'lilac', title: 'Share it, or make it joint', text: 'Share a habit with a circle for visibility: each person keeps their own check-ins and streak. Or give the circle a joint habit, where one check-in from anyone closes the day for everybody.' },
    { n: '3', fam: 'mint', title: 'Streaks on each habit’s own terms', text: 'Daily habits count days, weekly quotas count weeks on target, and chosen weekdays count only scheduled days. An unscheduled day never breaks a streak.' },
  ];
  const how = h('section', { class: 'lp-section', id: 'lp-how' },
    h('h2', { class: 'lp-h2' }, 'How it works'),
    h('ol', { class: 'lp-steps' }, steps.map((s) => h('li', { class: `lp-step ${famClass(s.fam)}` },
      h('span', { class: 'lp-step-n', 'aria-hidden': 'true' }, s.n),
      h('h3', { class: 'lp-step-title' }, s.title),
      h('p', { class: 'lp-step-text' }, s.text),
      s.bullets ? h('ul', { class: 'lp-step-list' }, s.bullets.map(([t, d]) => h('li', null, h('strong', null, t), ` — ${d}`))) : null))));

  const band = h('section', { class: 'lp-section' },
    h('div', { class: 'lp-band fam-mint' },
      h('div', null,
        h('h2', { class: 'lp-band-title' }, 'Start private. Share when you’re ready.'),
        h('p', { class: 'lp-band-text' }, 'Nobody sees a habit until you share it, and sharing never mixes your check-ins with anyone else’s.')),
      cta(google ? 'Continue with Google' : 'Get started', 'btn btn-primary btn-lg', google)));

  const footer = h('footer', { class: 'lp-footer' },
    h('span', { class: 'brand-name', style: 'font-size:18px' }, 'Cohabit'),
    h('span', { class: 'hint' }, google ? 'Free while in beta.' : 'Local development mode.'));

  return h('div', { class: 'lp' }, header, hero, features, how, band, footer);
}

function feature(fam, ico, title, text) {
  return h('div', { class: `lp-feature ${famClass(fam)}` },
    h('span', { class: 'lp-feature-ico' }, ico),
    h('h2', { class: 'lp-feature-title' }, title),
    h('p', { class: 'lp-feature-text' }, text));
}

function previewCard() {
  return h('figure', { class: 'lp-preview', 'aria-label': 'Example circle (illustration)' },
    h('div', { class: 'lp-preview-head' },
      h('div', { class: 'row', style: 'gap:12px;flex-wrap:nowrap' },
        h('span', { class: 'lp-preview-ico fam-sky' }, svg('<path d="M5 13.5 8 8l3 3.5L15 5" stroke-linecap="round" stroke-linejoin="round"/>', 'currentColor', 20, 20)),
        h('div', null,
          h('div', { class: 'lp-preview-name' }, 'Morning Runners'),
          h('div', { class: 'hint' }, 'Four runs each, any four days'))),
      h('span', { class: 'chip good' }, '14 of 20')),
    h('div', { class: 'lp-preview-rows' }, SAMPLE.map((m) => h('div', { class: `lp-preview-row ${famClass(m.fam)}` },
      h('div', { class: 'lp-preview-who' },
        h('span', { class: 'avatar md' }, m.initials),
        h('span', null, m.name)),
      h('div', { class: 'lp-pips' }, [0, 1, 2, 3].map((i) => h('span', { class: `pip ${i < m.done ? 'full' : ''}` }))),
      h('span', { class: `chip ${m.done >= 4 ? 'good' : ''} lp-count` }, `${m.done} / 4`)))),
    h('figcaption', { class: 'lp-caption' }, 'Example circle'));
}
