/* Patra: letter writer. No build step, no dependencies. */
(() => {
  'use strict';

  const STORE_KEY = 'patra:v1';
  const $ = (sel) => document.querySelector(sel);

  /* ---------- Words that are not part of a template ---------- */

  const BLANK = {
    name: { en: 'Your name', hi: 'आपका नाम' },
    address: { en: 'Your address', hi: 'आपका पता' },
    toName: { en: 'Name or designation', hi: 'नाम या पद' },
    toOrg: { en: 'Organisation and place', hi: 'संस्था और स्थान' },
    message: { en: 'Your message', hi: 'आपका संदेश' },
  };
  const LABEL = {
    date: { en: 'Date', hi: 'दिनांक' },
    subject: { en: 'Subject', hi: 'विषय' },
    to: { en: 'To,', hi: 'सेवा में,' },
  };
  const COMMON_PH = {
    'f-name': { en: 'Aarav Sharma', hi: 'आरव शर्मा' },
    'f-address': { en: '12 Gandhi Road\nJaipur 302001', hi: '12, गांधी मार्ग\nजयपुर 302001' },
    'f-contact': { en: '98765 43210', hi: '98765 43210' },
    'f-toName': { en: 'The Principal', hi: 'प्रधानाचार्य महोदय' },
    'f-toOrg': { en: 'Sunrise Public School, Jaipur', hi: 'सनराइज़ पब्लिक स्कूल, जयपुर' },
  };

  /* ---------- State ---------- */
  /* S.f  : details shared by every letter (name, address, recipient, date)
     S.t  : per letter type: v = answers, ov = subject/greeting/closing the person
            changed (missing = automatic), body = the person's own text
            (null = written automatically from the answers)                   */

  const fresh = () => ({
    type: 'leave',
    lang: 'en',
    f: { name: '', address: '', contact: '', toName: '', toOrg: '', date: null },
    t: {},
  });

  const load = () => {
    const s = fresh();
    try {
      const saved = JSON.parse(localStorage.getItem(STORE_KEY) || 'null');
      if (saved && typeof saved === 'object') {
        if (TEMPLATES[saved.type]) s.type = saved.type;
        if (saved.lang === 'hi') s.lang = 'hi';
        if (saved.f && typeof saved.f === 'object') {
          for (const k of Object.keys(s.f)) {
            if (typeof saved.f[k] === 'string') s.f[k] = saved.f[k];
          }
        }
        if (saved.t && typeof saved.t === 'object') s.t = saved.t;
      }
    } catch (e) { /* storage blocked or corrupt: start fresh */ }
    return s;
  };

  let S = load();
  // The data is tiny, so save on every change. A delayed save would lose the
  // last few keystrokes if the tab is closed right after typing.
  const save = () => {
    try { localStorage.setItem(STORE_KEY, JSON.stringify(S)); } catch (e) { /* storage blocked: the app still works */ }
  };

  const tpl = () => TEMPLATES[S.type];
  const cur = () => {
    const t = (S.t[S.type] = S.t[S.type] || {});
    t.v = t.v || {};
    t.ov = t.ov || {};
    if (t.body === undefined) t.body = null;
    return t;
  };

  /* ---------- Dates ---------- */

  const pad = (n) => String(n).padStart(2, '0');
  const todayISO = () => {
    const d = new Date();
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  };
  const parseISO = (iso) => {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || '');
    return m ? new Date(+m[1], +m[2] - 1, +m[3]) : null;
  };
  const fmtDate = (iso, lang) => {
    const d = parseISO(iso);
    if (!d) return '';
    return new Intl.DateTimeFormat(lang === 'hi' ? 'hi-IN' : 'en-IN', {
      day: 'numeric', month: 'long', year: 'numeric',
    }).format(d);
  };
  const daysBetween = (a, b) => {
    const da = parseISO(a), db = parseISO(b);
    return Math.round((Date.UTC(db.getFullYear(), db.getMonth(), db.getDate()) -
      Date.UTC(da.getFullYear(), da.getMonth(), da.getDate())) / 86400000) + 1;
  };

  /* ---------- Writing the letter ---------- */

  // Turn the person's answers into text for the template functions.
  const answers = (tp, v, lang) => {
    const g = {};
    for (const f of tp.fields) {
      let val = (v[f.key] || '').trim();
      if (f.type === 'date') val = val ? fmtDate(val, lang) : '';
      else if (f.type === 'text') val = val.replace(/[.।\s]+$/, '');
      g[f.key] = val || (f.req ? `[${f.blank[lang]}]` : '');
    }
    if (v.from && v.to && v.to > v.from) {
      g.range = true;
      g.days = daysBetween(v.from, v.to);
    }
    return g;
  };

  const lines = (s) => s.split('\n').map((x) => x.trim()).filter(Boolean);

  const compose = () => {
    const L = S.lang, tp = tpl(), tx = tp[L], t = cur(), f = S.f;
    const g = answers(tp, t.v, L);
    const blank = (k) => `[${BLANK[k][L]}]`;

    const subject = t.ov.subject != null ? t.ov.subject : tx.subject(g);
    const body = (t.body != null ? t.body : tx.body(g)).trimEnd();

    return {
      date: `${LABEL.date[L]}: ${fmtDate(f.date || todayISO(), L)}`,
      to: [LABEL.to[L], f.toName.trim() || blank('toName'),
        ...(f.toOrg.trim() ? lines(f.toOrg) : [blank('toOrg')])],
      subject: subject ? `${LABEL.subject[L]}: ${subject}` : '',
      salutation: t.ov.salutation != null ? t.ov.salutation : tx.salutation,
      body: body.trim() ? body : blank('message'),
      closing: t.ov.closing != null ? t.ov.closing : tx.closing,
      sign: [f.name.trim() || blank('name'),
        ...(f.address.trim() ? lines(f.address) : [blank('address')]),
        f.contact.trim()].filter(Boolean),
    };
  };

  const toText = (c) =>
    [c.date, c.to.join('\n'), c.subject, c.salutation, c.body, c.closing, c.sign.join('\n')]
      .filter(Boolean).join('\n\n') + '\n';

  /* ---------- Drawing the paper ---------- */

  const BLANK_RE = /(\[[^\]\n]+\])/;

  const fill = (el, text) => {
    text.split(BLANK_RE).forEach((part, i) => {
      if (!part) return;
      if (i % 2) {
        const m = document.createElement('mark');
        m.className = 'blank';
        m.textContent = part;
        el.append(m);
      } else {
        el.append(document.createTextNode(part));
      }
    });
  };

  const drawPaper = (c) => {
    const paper = $('#paper');
    paper.lang = S.lang;
    paper.replaceChildren();
    const add = (cls, text) => {
      const p = document.createElement('p');
      if (cls) p.className = cls;
      fill(p, text);
      paper.append(p);
    };
    add('', c.date);
    add('', c.to.join('\n'));
    if (c.subject) add('subject', c.subject);
    add('', c.salutation);
    add('body', c.body);
    add('closing', c.closing);
    add('tail', c.sign.join('\n'));
  };

  const drawBlanks = (text) => {
    const n = (text.match(/\[[^\]\n]+\]/g) || []).length;
    const el = $('#blanks');
    el.textContent = n ? `${n} ${n === 1 ? 'blank' : 'blanks'} left to fill in` : 'No blanks left';
    el.classList.toggle('open', n > 0);
  };

  /* ---------- Form ---------- */

  const typesEl = $('#types');
  const tfieldsEl = $('#tfields');

  const buildTypes = () => {
    typesEl.replaceChildren();
    for (const [id, tp] of Object.entries(TEMPLATES)) {
      const label = document.createElement('label');
      label.className = 'type';
      label.dataset.id = id;
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'type';
      input.value = id;
      const name = document.createElement('span');
      name.className = 't-name';
      name.textContent = tp.title;
      const hint = document.createElement('span');
      hint.className = 't-hint';
      hint.textContent = tp.hint;
      label.append(input, name, hint);
      typesEl.append(label);
    }
  };

  const markType = () => {
    for (const label of typesEl.children) {
      const on = label.dataset.id === S.type;
      label.classList.toggle('on', on);
      label.firstChild.checked = on;
    }
  };

  const buildDetails = () => {
    const tp = tpl(), t = cur();
    tfieldsEl.replaceChildren();
    $('#details-section').hidden = tp.fields.length === 0;
    for (const f of tp.fields) {
      const wrap = document.createElement('div');
      wrap.className = 'field';
      const label = document.createElement('label');
      label.htmlFor = `d-${f.key}`;
      label.textContent = f.label;
      const input = document.createElement(f.type === 'textarea' ? 'textarea' : 'input');
      input.id = `d-${f.key}`;
      if (f.type === 'textarea') input.rows = 3;
      else input.type = f.type === 'date' ? 'date' : 'text';
      input.value = t.v[f.key] || '';
      if (f.ph) input.placeholder = f.ph[S.lang];
      input.addEventListener('input', () => {
        t.v[f.key] = input.value;
        update();
      });
      wrap.append(label, input);
      if (f.help) {
        const help = document.createElement('p');
        help.className = 'help';
        help.textContent = f.help;
        wrap.append(help);
      }
      if (f.key === 'to') {
        const warn = document.createElement('p');
        warn.className = 'help warn';
        warn.id = 'range-warn';
        warn.hidden = true;
        warn.textContent = 'This date is before the first day, so the letter asks for one day only.';
        wrap.append(warn);
      }
      tfieldsEl.append(wrap);
    }
  };

  // Show the current values in the shared fields and the three "wording" fields.
  const syncCommon = () => {
    const f = S.f, t = cur();
    for (const k of ['name', 'address', 'contact', 'toName', 'toOrg']) $(`#f-${k}`).value = f[k];
    for (const [id, ph] of Object.entries(COMMON_PH)) $(`#${id}`).placeholder = ph[S.lang];
    $('#f-date').value = f.date || todayISO();
    syncWording();
    syncBody(true);
  };

  const syncWording = () => {
    const t = cur(), tx = tpl()[S.lang];
    const g = answers(tpl(), t.v, S.lang);
    const auto = { subject: tx.subject(g), salutation: tx.salutation, closing: tx.closing };
    for (const k of Object.keys(auto)) {
      const input = $(`#o-${k}`);
      if (input === document.activeElement) continue;
      input.value = t.ov[k] != null ? t.ov[k] : auto[k];
    }
  };

  const syncBody = (force) => {
    const ta = $('#f-body'), t = cur(), tp = tpl();
    if (force || ta !== document.activeElement) {
      ta.value = t.body != null ? t.body : tp[S.lang].body(answers(tp, t.v, S.lang));
    }
    ta.placeholder = S.type === 'blank' ? 'Write your letter here.' : '';
    $('#rewrite').hidden = !(t.body != null && S.type !== 'blank');
    $('#body-help').textContent = t.body != null
      ? 'You changed the text, so new details will not update it.'
      : S.type === 'blank' ? 'Write whatever you need.' : 'Edit the text any way you like. It updates as you fill in details until you change it.';
  };

  /* ---------- Update everything that depends on the state ---------- */

  const update = () => {
    const t = cur();
    const c = compose();
    drawPaper(c);
    drawBlanks(toText(c));
    syncWording();
    syncBody(false);
    const warn = $('#range-warn');
    if (warn) warn.hidden = !(t.v.from && t.v.to && t.v.to < t.v.from);
    save();
  };

  const rebuildAll = () => {
    markType();
    buildDetails();
    syncCommon();
    $('input[name="lang"][value="' + S.lang + '"]').checked = true;
    update();
  };

  /* ---------- Events ---------- */

  const bind = () => {
    typesEl.addEventListener('change', (e) => {
      if (e.target.name !== 'type') return;
      S.type = e.target.value;
      rebuildAll();
    });

    $('#lang').addEventListener('change', (e) => {
      S.lang = e.target.value === 'hi' ? 'hi' : 'en';
      rebuildAll();
    });

    for (const k of ['name', 'address', 'contact', 'toName', 'toOrg']) {
      $(`#f-${k}`).addEventListener('input', (e) => { S.f[k] = e.target.value; update(); });
    }

    $('#f-date').addEventListener('input', (e) => {
      S.f.date = e.target.value && e.target.value !== todayISO() ? e.target.value : null;
      update();
    });

    for (const k of ['subject', 'salutation', 'closing']) {
      const input = $(`#o-${k}`);
      input.addEventListener('input', () => {
        const t = cur();
        if (input.value.trim() === '') delete t.ov[k];
        else t.ov[k] = input.value;
        update();
      });
      // Emptying a field means "go back to the automatic wording".
      input.addEventListener('change', syncWording);
      input.addEventListener('blur', syncWording);
    }

    $('#f-body').addEventListener('input', (e) => {
      cur().body = e.target.value;
      update();
    });

    $('#rewrite').addEventListener('click', () => {
      cur().body = null;
      syncBody(true);
      update();
    });

    $('#reset').addEventListener('click', () => {
      if (!window.confirm('Clear everything you have written on this device?')) return;
      S = fresh();
      rebuildAll();
      toast('Started over');
    });

    $('#copy').addEventListener('click', copyText);
    $('#download').addEventListener('click', downloadText);
    $('#print').addEventListener('click', printLetter);

    for (const btn of document.querySelectorAll('.tabs button')) {
      btn.addEventListener('click', () => {
        $('#app').dataset.view = btn.dataset.go;
        for (const b of document.querySelectorAll('.tabs button')) {
          b.setAttribute('aria-pressed', String(b === btn));
        }
        window.scrollTo(0, 0);
      });
    }
  };

  /* ---------- Copy, download, print ---------- */

  let toastTimer = 0;
  const toast = (msg) => {
    const el = $('#toast');
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 2200);
  };

  const copyText = async () => {
    const text = toText(compose());
    try {
      await navigator.clipboard.writeText(text);
    } catch (e) {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.append(ta);
      ta.select();
      let ok = false;
      try { ok = document.execCommand('copy'); } catch (err) { ok = false; }
      ta.remove();
      if (!ok) { toast('Could not copy. Select the text in the letter and copy it.'); return; }
    }
    toast('Letter text copied');
  };

  const downloadText = () => {
    // The BOM makes Hindi open correctly in Windows Notepad.
    const blob = new Blob(['﻿' + toText(compose())], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${tpl().slug}.txt`;
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    toast('Downloaded');
  };

  const printLetter = () => {
    // The browser uses the page title as the PDF file name.
    const before = document.title;
    const who = S.f.name.trim();
    document.title = who ? `${tpl().title} - ${who}` : tpl().title;
    const restore = () => { document.title = before; window.removeEventListener('afterprint', restore); };
    window.addEventListener('afterprint', restore);
    window.print();
  };

  /* ---------- Start ---------- */

  buildTypes();
  bind();
  rebuildAll();
})();
