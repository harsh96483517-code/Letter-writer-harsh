package app.writer.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.writer.core.tr
import app.writer.design.Glass
import app.writer.design.Spacing
import app.writer.design.WriterType
import app.writer.design.components.GlassCard
import app.writer.design.components.GlassText
import app.writer.design.components.GlassTopBar
import app.writer.design.components.WriterLogo

/** A titled paragraph of plain text. */
private class Section(val title: String, val body: String)

@Composable
private fun InfoPage(title: String, onBack: () -> Unit, header: @Composable () -> Unit = {}, sections: List<Section>) {
    val c = Glass.colors
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = title, onBack = onBack, backLabel = tr("वापस", "Back"))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding()
                .padding(bottom = Spacing.x3),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1_5),
        ) {
            header()
            sections.forEach { section ->
                GlassCard {
                    GlassText(text = section.title, style = WriterType.bodyStrong)
                    GlassText(
                        text = section.body,
                        style = WriterType.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = Spacing.half),
                    )
                }
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    InfoPage(
        title = tr("सहायता", "Help & support"),
        onBack = onBack,
        sections = listOf(
            Section(
                tr("नया पत्र कैसे बनाएं?", "How do I write a new letter?"),
                tr(
                    "नीचे बीच के + बटन को दबाएं और खाली दस्तावेज़ या कोई टेम्पलेट चुनें। फिर फ़ॉर्म में अपनी जानकारी भरें — पत्र अपने-आप तैयार होता जाएगा।",
                    "Tap the + button in the middle of the bottom bar and choose a blank document or a template. Fill in your details in the form and the letter is written for you as you go.",
                ),
            ),
            Section(
                tr("पत्र में [ ] कोष्ठक क्या हैं?", "What are the [ ] brackets in my letter?"),
                tr(
                    "ये खाली जगहें हैं जो आपको भरनी हैं, जैसे [आपका नाम]। PDF बनाने से पहले इन्हें भर लें; वरना ऐप आपको याद दिलाएगा।",
                    "These are blanks you still need to fill in, such as [Your name]. Fill them before exporting; otherwise the app will remind you.",
                ),
            ),
            Section(
                tr("PDF कैसे बनाएं?", "How do I make a PDF?"),
                tr(
                    "पत्र के प्रीव्यू में “PDF बनाएं” दबाएं, फ़ोन की फ़ाइल-विंडो में जगह और नाम चुनें। PDF असली टेक्स्ट वाली होती है, फ़ोटो नहीं।",
                    "Open the letter preview and tap “Export PDF”, then choose a location and name in your phone's file window. The PDF contains real text, not a picture.",
                ),
            ),
            Section(
                tr("प्रिंट कैसे करें?", "How do I print?"),
                tr(
                    "प्रीव्यू में “प्रिंट” दबाएं। फ़ोन का प्रिंट सिस्टम खुलेगा, जहाँ आप प्रिंटर चुन सकते हैं या “PDF के रूप में सेव करें” चुन सकते हैं। प्रिंटर तभी चलेगा जब आपके फ़ोन में प्रिंट सेवा या प्लगइन चालू हो।",
                    "Tap “Print” in the preview. Your phone's print system opens, where you can pick a printer or choose “Save as PDF”. A printer works only if a print service or plugin is set up on your phone.",
                ),
            ),
            Section(
                tr("मेरी फाइलें कहाँ हैं?", "Where are my files?"),
                tr(
                    "दस्तावेज़ और ड्राफ्ट ऐप के अंदर “मेरी फाइलें” टैब में रहते हैं। PDF वहाँ भी दिखती है और उस फ़ोल्डर में भी जो आपने चुना था।",
                    "Documents and drafts are in the “My Files” tab. A PDF is listed there and also saved in the folder you chose.",
                ),
            ),
            Section(
                tr("क्या टेम्पलेट सरकारी फ़ॉर्म हैं?", "Are the templates official forms?"),
                tr(
                    "नहीं। ये संपादन योग्य उदाहरण हैं। हर दफ़्तर का अपना नियम हो सकता है, इसलिए भेजने से पहले जानकारी जाँच लें। Writer यह गारंटी नहीं देता कि कोई आवेदन स्वीकार होगा।",
                    "No. They are editable examples. Every office may have its own rules, so check the details before sending. Writer does not guarantee that any application will be accepted.",
                ),
            ),
        ),
    )
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    InfoPage(
        title = tr("गोपनीयता नीति", "Privacy policy"),
        onBack = onBack,
        sections = listOf(
            Section(
                tr("आपका डेटा आपके फ़ोन में रहता है", "Your data stays on your phone"),
                tr(
                    "आपके पत्र, पते, नाम और मोबाइल नंबर सिर्फ़ इस फ़ोन की ऐप-मेमोरी में रखे जाते हैं। Writer उन्हें किसी सर्वर पर नहीं भेजता।",
                    "Your letters, addresses, names and mobile numbers are kept only in this app's storage on your phone. Writer does not send them to any server.",
                ),
            ),
            Section(
                tr("कोई अकाउंट या लॉगिन नहीं", "No account or sign-in"),
                tr(
                    "ऐप इस्तेमाल करने के लिए किसी अकाउंट, ईमेल या फ़ोन नंबर की ज़रूरत नहीं है।",
                    "You do not need an account, email or phone number to use the app.",
                ),
            ),
            Section(
                tr("अनुमतियाँ", "Permissions"),
                tr(
                    "Writer कोई अनुमति नहीं माँगता: न इंटरनेट, न कॉन्टैक्ट, न SMS, न लोकेशन, न स्टोरेज। PDF सेव करने के लिए फ़ोन की अपनी फ़ाइल-विंडो इस्तेमाल होती है, जहाँ आप खुद जगह चुनते हैं।",
                    "Writer asks for no permissions: no internet, contacts, SMS, location or storage. Saving a PDF uses your phone's own file window, where you choose the location yourself.",
                ),
            ),
            Section(
                tr("विश्लेषण और विज्ञापन", "Analytics and ads"),
                tr(
                    "ऐप में कोई विज्ञापन या ट्रैकिंग नहीं है, और आपके दस्तावेज़ की सामग्री का कोई विश्लेषण नहीं होता।",
                    "The app has no ads or tracking, and the content of your documents is never analysed.",
                ),
            ),
            Section(
                tr("शेयर और प्रिंट", "Sharing and printing"),
                tr(
                    "कोई PDF तभी बाहर जाती है जब आप खुद शेयर या प्रिंट दबाते हैं और उसे कोई ऐप या प्रिंटर चुनते हैं। उसके बाद वह ऐप या सेवा उसे कैसे संभालती है, यह उसकी अपनी नीति पर निर्भर है।",
                    "A PDF leaves the app only when you tap Share or Print and pick an app or printer. What that app or service does with it afterwards follows its own policy.",
                ),
            ),
            Section(
                tr("डेटा हटाना", "Deleting data"),
                tr(
                    "सेटिंग्स में आप दस्तावेज़, सेव की गई जानकारी और हाल की खोज हटा सकते हैं। ऐप हटाने (अनइंस्टॉल) पर ऐप का सारा डेटा भी हट जाता है। जो PDF आपने किसी फ़ोल्डर में सेव की हैं या किसी को भेजी हैं, वे इससे नहीं हटतीं — उन्हें आपको खुद हटाना होगा।",
                    "In Settings you can delete documents, saved details and recent searches. Uninstalling the app removes all its data too. PDFs you saved in a folder or sent to someone are not removed by this — you need to delete them yourself.",
                ),
            ),
        ),
    )
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = Glass.colors
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    InfoPage(
        title = tr("Writer के बारे में", "About Writer"),
        onBack = onBack,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.x2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                WriterLogo(size = 88.dp)
                GlassText(text = "Writer", style = WriterType.display)
                GlassText(
                    text = tr("लिखें • आवेदन करें • परिणाम पाएं", "Write • Apply • Get Results"),
                    style = WriterType.bodySmall,
                    color = c.textSecondary,
                )
                if (version.isNotEmpty()) {
                    GlassText(text = tr("संस्करण $version", "Version $version"), style = WriterType.caption, color = c.textMuted)
                }
            }
        },
        sections = listOf(
            Section(
                tr("Writer क्या है?", "What is Writer?"),
                tr(
                    "हिंदी और English में पत्र और आवेदन लिखने, A4 PDF बनाने और प्रिंट करने का ऐप। बिना इंटरनेट और बिना लॉगिन के चलता है।",
                    "An app for writing letters and applications in Hindi and English, exporting A4 PDFs and printing them. It works without internet and without sign-in.",
                ),
            ),
            Section(
                tr("टेम्पलेट", "Templates"),
                tr(
                    "टेम्पलेट संपादन योग्य उदाहरण हैं, आधिकारिक सरकारी फ़ॉर्म नहीं। इन्हें किसी विभाग की मंज़ूरी प्राप्त नहीं है और स्वीकार होने की कोई गारंटी नहीं है।",
                    "Templates are editable examples, not official government forms. No department has approved them and acceptance is not guaranteed.",
                ),
            ),
            Section(
                tr("फ़ॉन्ट", "Fonts"),
                tr(
                    "पत्रों में Noto Sans और Noto Serif (देवनागरी सहित) फ़ॉन्ट इस्तेमाल होते हैं, जो SIL Open Font License के तहत उपलब्ध हैं।",
                    "Letters use the Noto Sans and Noto Serif fonts (including Devanagari), available under the SIL Open Font License.",
                ),
            ),
        ),
    )
}
