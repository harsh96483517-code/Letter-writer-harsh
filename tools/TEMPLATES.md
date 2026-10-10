# Writer template format

Built-in letter templates live in `android/app/src/main/assets/templates/<category>.json`.
Each file is a JSON array of template objects. The app loads every `*.json` file in that folder.
Run `python3 tools/validate_templates.py` after any change. It must print `OK`.

Templates are **editable examples, not official government forms**. Use neutral, professional
language. Never claim a template is legally approved or guaranteed to be accepted.

## Template object

```json
{
  "id": "cmp_electricity",
  "category": "complaints",
  "style": "application",
  "titleHi": "बिजली शिकायत पत्र",
  "titleEn": "Electricity complaint",
  "descHi": "बिजली आपूर्ति की समस्या की शिकायत",
  "descEn": "Complaint about power supply problems",
  "keywords": ["bijli", "electricity", "power cut", "बिजली", "बत्ती", "light", "discom", "vidyut"],
  "recipient": {
    "hi": { "designation": "अधिशासी अभियंता महोदय", "office": "विद्युत वितरण खंड" },
    "en": { "designation": "The Executive Engineer", "office": "Electricity Distribution Division" }
  },
  "salutation": { "hi": "महोदय / महोदया,", "en": "Respected Sir/Madam," },
  "subject": {
    "hi": "बिजली आपूर्ति की समस्या के समाधान हेतु शिकायत",
    "en": "Complaint regarding electricity supply problem"
  },
  "body": {
    "hi": "मेरा नाम {{name}} है और मैं {{address}} का निवासी हूँ।\nविनम्र निवेदन है कि {{since_when}} से हमारे क्षेत्र में बिजली आपूर्ति की समस्या बनी हुई है। {{problem_details}}\nइससे घर के दैनिक कार्यों और बच्चों की पढ़ाई में कठिनाई हो रही है। यह समस्या पहले भी बताई गई थी, परंतु अभी तक समाधान नहीं हुआ है।\nअतः आपसे विनम्र निवेदन है कि कृपया मामले की जाँच कराकर शीघ्र समाधान कराने की कृपा करें।\nधन्यवाद।",
    "en": "My name is {{name}} and I live at {{address}}.\nI would like to bring to your notice that our area has been facing problems with electricity supply since {{since_when}}. {{problem_details}}\nThis is causing difficulty in daily household work and in my children's studies. The matter was raised earlier as well, but it has not been resolved so far.\nI therefore request you to kindly look into the matter and arrange for a prompt resolution.\nThank you."
  },
  "closing": { "hi": "भवदीय,", "en": "Yours faithfully," },
  "fields": [
    {
      "key": "since_when",
      "labelHi": "समस्या कब से है",
      "labelEn": "Since when",
      "hintHi": "जैसे: 5 अक्टूबर से",
      "hintEn": "e.g. 5 October",
      "multiline": false,
      "required": false
    },
    {
      "key": "problem_details",
      "labelHi": "समस्या का विवरण",
      "labelEn": "Problem details",
      "hintHi": "जैसे: दिन में 8 घंटे बिजली नहीं आती",
      "hintEn": "e.g. no power for 8 hours a day",
      "multiline": true,
      "required": false
    }
  ]
}
```

## Rules

**Identity.** `id` is lowercase letters, digits and underscores, exactly as listed in
`tools/validate_templates.py`. `category` is one of `gov`, `education`, `schemes`, `complaints`,
`banking`, `employment`, `personal`. `style` is `application` (formal, with the recipient block and
subject line) or `personal` (friendly letter; `recipient` may be `null`).

**Text.** Every text value comes in Hindi (`hi`, Devanagari Unicode) and English (`en`). The English
is natural Indian formal English. Titles are short (under 40 characters). Descriptions are one
short line (under 70 characters). `keywords` has at least 5 entries mixing Hindi words, English
words and common romanised Hindi (Hinglish) that people would type when searching.

**Body.** Paragraphs are separated by a single `\n` (never a blank line). Use 3 to 5 paragraphs:
introduce the applicant, state the matter using the template's fields, give the reason or
background, then close with a polite request, for example
"अतः आपसे विनम्र निवेदन है कि कृपया ... करने की कृपा करें।", and finish with "धन्यवाद।" (Hindi) or
"Thank you." (English). The body must not include the recipient block, subject line, salutation,
closing, signature, date or address block. The app adds those around the body.

**Placeholders.** Write `{{key}}`. Standard keys the app fills from the form:
`name`, `father`, `address` (the full address, already combined from village, post office, block,
district, state and PIN), `village`, `post`, `block`, `district`, `state`, `pin`, `mobile`, `date`,
`recipient_district`, `recipient_office`, `recipient_designation`. Prefer `{{address}}` to the
individual address parts. Any other key must be declared in `fields`. Empty values are shown to the
user as a highlighted blank such as `[आपका नाम]`, so write sentences that still read well with a
blank in them.

**Fields.** 1 to 4 fields per template (the template-specific facts the user fills in): `key`
(lowercase, digits, underscores), `labelHi`, `labelEn`, `hintHi`, `hintEn` (an example), `multiline`
(boolean) and `required` (boolean, normally `false`). Every field must be used in a body.

**Hindi must be gender neutral.** Do not use verb forms that depend on the speaker's gender:
avoid "करता हूँ/करती हूँ", "रहता हूँ/रहती हूँ", "चाहता/चाहती हूँ", "गया/गई". Use constructions such as
"निवेदन है कि", "मेरी प्रार्थना है कि", "आपसे अनुरोध है", "मेरा निवास ... में है", "मैं ... का
निवासी हूँ", "विद्यार्थी" (not छात्र/छात्रा), and "पुत्र/पुत्री" where a relation is needed.

**Closing.** Hindi `भवदीय,` (formal) or `सादर / स्नेह सहित,` (personal). English `Yours faithfully,`
(formal) or `Yours sincerely,` (personal).

**Technical.** Valid UTF-8 JSON. Escape double quotes inside strings. No `$` character, no
`TODO`, no carriage returns, no HTML, no markdown.
