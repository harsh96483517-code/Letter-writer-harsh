# पत्र (Patra): letter writer

Write a leave application, resignation, job application, complaint or thank-you
letter in English or Hindi. Fill in a few details, watch the letter appear on
ruled notebook paper, then print it, save it as a PDF, copy the text or download it.

- No sign-up, no server, no build step. Everything stays in your browser.
- Your work is saved on your device and comes back when you reopen the page.
- Anything you have not filled in yet shows as a highlighted `[blank]`, and the
  page counts how many are left.

## Use it

Open `index.html` in a browser. That is all.

To put it online, turn on GitHub Pages: **Settings → Pages → Deploy from a branch →
`main` / root**. The site is then served at
`https://<your-username>.github.io/letter-writer-harsh/`.

## Letter types

Leave application, resignation letter, job application, complaint letter,
thank-you letter, and a blank letter. Each has an English and a Hindi version.
The Hindi wording avoids gendered verbs so it works for everyone.

## Files

| File | What it does |
| --- | --- |
| `index.html` | Page structure |
| `style.css` | Look and feel, mobile layout, print layout |
| `templates.js` | The letter types: their questions and wording in both languages |
| `app.js` | Form, live preview, saving, copy, download, print |

## Add a letter type

Add an entry to `TEMPLATES` in `templates.js`: list its `fields` (the questions),
then write `subject`, `salutation`, `closing` and `body` for `en` and `hi`.
The list in the app and the preview pick it up automatically.
