/*
 * Letter templates for Patra.
 *
 * Every template has:
 *   fields  - the details the person fills in (shown in the "Details" section)
 *   en / hi - for each language: subject(g), salutation, closing, body(g)
 *
 * `g` holds the person's answers, already cleaned and formatted for the
 * letter's language. A required answer that is still empty becomes a
 * [blank in brackets], which the preview highlights.
 *
 * To add a letter type, add an entry here. Nothing else needs to change.
 */

const paras = (list) => list.filter(Boolean).join('\n\n');

const TEMPLATES = {
  leave: {
    title: 'Leave application',
    hint: 'Time off from school or work',
    slug: 'leave-application',
    fields: [
      {
        key: 'from', label: 'First day of leave', type: 'date', req: true,
        blank: { en: 'first day of leave', hi: 'अवकाश का पहला दिन' },
      },
      {
        key: 'to', label: 'Last day of leave', type: 'date',
        help: 'Leave this empty if it is a single day.',
      },
      {
        key: 'reason', label: 'Why do you need leave?', type: 'text', req: true,
        blank: { en: 'reason for leave', hi: 'अवकाश का कारण' },
        ph: {
          en: 'I have a fever and my doctor advised rest',
          hi: 'मुझे बुखार है और डॉक्टर ने आराम की सलाह दी है',
        },
        help: 'Write a short sentence. In the letter it follows the word “because”.',
      },
    ],
    en: {
      subject: () => 'Application for leave',
      salutation: 'Respected Sir/Madam,',
      closing: 'Yours faithfully,',
      body: (g) => {
        const when = g.range
          ? `from ${g.from} to ${g.to} (${g.days} ${g.days === 1 ? 'day' : 'days'})`
          : `on ${g.from}`;
        return paras([
          `I am writing to request leave ${when}, because ${g.reason}.`,
          'I will catch up on any work I miss as soon as I return. I would be grateful if you could grant me leave for this period.',
          'Thank you for your consideration.',
        ]);
      },
    },
    hi: {
      subject: () => 'अवकाश हेतु प्रार्थना पत्र',
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'भवदीय,',
      body: (g) => {
        const when = g.range ? `${g.from} से ${g.to} तक (${g.days} दिन)` : g.from;
        return paras([
          `सविनय निवेदन है कि मुझे ${when} का अवकाश चाहिए, क्योंकि ${g.reason}।`,
          'कृपया उक्त अवधि के लिए मेरा अवकाश स्वीकृत करने की कृपा करें। छूटा हुआ कार्य लौटने पर पूरा कर लिया जाएगा।',
          'आपकी अति कृपा होगी।',
        ]);
      },
    },
  },

  resignation: {
    title: 'Resignation letter',
    hint: 'Give notice and leave on good terms',
    slug: 'resignation-letter',
    fields: [
      {
        key: 'position', label: 'Your post', type: 'text', req: true,
        blank: { en: 'your post', hi: 'आपका पद' },
        ph: { en: 'Sales Executive', hi: 'सेल्स एक्ज़ीक्यूटिव' },
      },
      {
        key: 'lastDay', label: 'Last working day', type: 'date', req: true,
        blank: { en: 'last working day', hi: 'अंतिम कार्य दिवस' },
      },
      {
        key: 'reason', label: 'Reason (optional)', type: 'text',
        ph: { en: 'I am moving to another city', hi: 'किसी दूसरे शहर में जाना' },
        help: 'Skip this if you would rather not give a reason.',
      },
    ],
    en: {
      subject: (g) => `Resignation from the post of ${g.position}`,
      salutation: 'Dear Sir/Madam,',
      closing: 'Yours faithfully,',
      body: (g) =>
        paras([
          `Please accept this letter as formal notice of my resignation from the post of ${g.position}. My last working day will be ${g.lastDay}.`,
          g.reason && `I am leaving because ${g.reason}.`,
          'Thank you for the opportunities and support I have received here. I will help hand over my work smoothly before I go.',
          'Please let me know if you need anything else from me.',
        ]),
    },
    hi: {
      subject: (g) => `${g.position} पद से त्यागपत्र`,
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'भवदीय,',
      body: (g) =>
        paras([
          `कृपया ${g.position} पद से मेरा त्यागपत्र स्वीकार करें। मेरा अंतिम कार्य दिवस ${g.lastDay} होगा।`,
          g.reason && `त्यागपत्र देने का कारण: ${g.reason}।`,
          'यहाँ कार्य करते हुए जो अवसर और सहयोग मिला, उसके लिए हार्दिक धन्यवाद। अपने कार्य का सुचारु हस्तांतरण करने में मेरी ओर से पूरा सहयोग रहेगा।',
          'यदि मुझसे किसी और जानकारी की आवश्यकता हो तो कृपया बताएँ।',
        ]),
    },
  },

  job: {
    title: 'Job application',
    hint: 'Apply for a post with a short cover letter',
    slug: 'job-application',
    fields: [
      {
        key: 'position', label: 'Post you are applying for', type: 'text', req: true,
        blank: { en: 'post applied for', hi: 'आवेदित पद' },
        ph: { en: 'Customer Support Executive', hi: 'कस्टमर सपोर्ट एक्ज़ीक्यूटिव' },
      },
      {
        key: 'source', label: 'Where did you see the job? (optional)', type: 'text',
        ph: { en: 'on your company website', hi: 'आपकी वेबसाइट' },
      },
      {
        key: 'background', label: 'Your experience or education', type: 'textarea', req: true,
        blank: { en: 'your experience or education', hi: 'आपका अनुभव या शिक्षा' },
        ph: {
          en: 'I have two years of experience in customer support and a B.Com degree from Delhi University.',
          hi: 'मुझे ग्राहक सहायता में दो वर्ष का अनुभव है और मैंने दिल्ली विश्वविद्यालय से बी.कॉम किया है।',
        },
      },
      {
        key: 'strengths', label: 'Your strengths (optional)', type: 'textarea',
        ph: {
          en: 'I am organised, quick to learn, and comfortable working in a team.',
          hi: 'मैं व्यवस्थित हूँ, जल्दी सीखता/सीखती हूँ और टीम में काम करना मुझे पसंद है।',
        },
      },
    ],
    en: {
      subject: (g) => `Application for the post of ${g.position}`,
      salutation: 'Dear Sir/Madam,',
      closing: 'Yours faithfully,',
      body: (g) =>
        paras([
          `I am writing to apply for the post of ${g.position}${g.source ? `, which I found ${g.source}` : ''}.`,
          g.background,
          g.strengths,
          'I have attached my resume for your review. I would welcome the chance to discuss how I can contribute to your team, and I am available for an interview at your convenience.',
        ]),
    },
    hi: {
      subject: (g) => `${g.position} पद हेतु आवेदन`,
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'भवदीय,',
      body: (g) =>
        paras([
          `${g.position} पद के लिए मेरा आवेदन प्रस्तुत है।${g.source ? ` इस पद की जानकारी मुझे ${g.source} से मिली।` : ''}`,
          g.background,
          g.strengths,
          'मेरा बायोडाटा संलग्न है। कृपया मुझे साक्षात्कार का अवसर प्रदान करें। आपकी सुविधा के अनुसार उपस्थित होने में मुझे कोई कठिनाई नहीं होगी।',
        ]),
    },
  },

  complaint: {
    title: 'Complaint letter',
    hint: 'Report a problem and ask for action',
    slug: 'complaint-letter',
    fields: [
      {
        key: 'topic', label: 'What is the complaint about?', type: 'text', req: true,
        blank: { en: 'what the complaint is about', hi: 'शिकायत का विषय' },
        ph: { en: 'the repeated power cuts in our colony', hi: 'हमारी कॉलोनी में बार-बार बिजली कटौती' },
      },
      { key: 'when', label: 'Date it happened (optional)', type: 'date' },
      {
        key: 'issue', label: 'What happened?', type: 'textarea', req: true,
        blank: { en: 'what happened', hi: 'क्या हुआ' },
        ph: {
          en: 'Describe what happened, who was involved, and how it has affected you.',
          hi: 'बताइए कि क्या हुआ, कौन शामिल था और इसका आप पर क्या असर पड़ा।',
        },
      },
      {
        key: 'ask', label: 'What do you want done?', type: 'text', req: true,
        blank: { en: 'what you want done', hi: 'आप क्या कार्रवाई चाहते हैं' },
        ph: {
          en: 'look into this matter and fix it within a week',
          hi: 'इस मामले की जाँच कर एक सप्ताह में समाधान किया जाए',
        },
        help: 'In the letter this follows “I request you to …”.',
      },
    ],
    en: {
      subject: (g) => `Complaint about ${g.topic}`,
      salutation: 'Dear Sir/Madam,',
      closing: 'Yours faithfully,',
      body: (g) =>
        paras([
          `I am writing to complain about ${g.topic}.${g.when ? ` This happened on ${g.when}.` : ''}`,
          g.issue,
          `I request you to ${g.ask}. I would be grateful for a reply at the earliest.`,
        ]),
    },
    hi: {
      subject: (g) => `${g.topic} के संबंध में शिकायत`,
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'भवदीय,',
      body: (g) =>
        paras([
          `यह पत्र ${g.topic} के संबंध में शिकायत दर्ज कराने हेतु है।${g.when ? ` यह घटना ${g.when} को हुई।` : ''}`,
          g.issue,
          `आपसे अनुरोध है कि ${g.ask}। शीघ्र उत्तर देने की कृपा करें।`,
        ]),
    },
  },

  thanks: {
    title: 'Thank-you letter',
    hint: 'Thank someone for their help or time',
    slug: 'thank-you-letter',
    fields: [
      {
        key: 'forWhat', label: 'What are you thankful for?', type: 'text', req: true,
        blank: { en: 'what you are thankful for', hi: 'धन्यवाद का कारण' },
        ph: { en: 'guiding me during my internship', hi: 'इंटर्नशिप के दौरान आपके मार्गदर्शन' },
        help: 'In the letter this follows “Thank you very much for …”.',
      },
      {
        key: 'detail', label: 'Anything to add? (optional)', type: 'textarea',
        ph: {
          en: 'Your advice helped me finish my first project on time.',
          hi: 'आपकी सलाह से मेरा पहला प्रोजेक्ट समय पर पूरा हो सका।',
        },
      },
    ],
    en: {
      subject: () => '',
      salutation: 'Dear Sir/Madam,',
      closing: 'With warm regards,',
      body: (g) =>
        paras([
          `Thank you very much for ${g.forWhat}.`,
          g.detail,
          'I truly appreciate your time and support, and I hope we stay in touch.',
        ]),
    },
    hi: {
      subject: () => '',
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'सादर,',
      body: (g) =>
        paras([
          `${g.forWhat} के लिए आपका बहुत-बहुत धन्यवाद।`,
          g.detail,
          'आपके समय और सहयोग के लिए मैं हृदय से आभारी हूँ। आशा है कि हमारा संपर्क बना रहेगा।',
        ]),
    },
  },

  blank: {
    title: 'Blank letter',
    hint: 'Write your own text',
    slug: 'letter',
    fields: [],
    en: {
      subject: () => '',
      salutation: 'Dear Sir/Madam,',
      closing: 'Yours sincerely,',
      body: () => '',
    },
    hi: {
      subject: () => '',
      salutation: 'आदरणीय महोदय/महोदया,',
      closing: 'भवदीय,',
      body: () => '',
    },
  },
};
