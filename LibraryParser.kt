package dz.baladiya.check.library

import dz.baladiya.check.rules.ArabicText
import dz.baladiya.check.rules.LibrarySection
import org.jsoup.Jsoup

/**
 * يحلّل HTML صفحة gestion_du_personnel.php.
 *
 * بنية الصفحة (حسب ما وُصف لنا): أقسام، كل قسم جدول بعمودين (الرقم، العنوان)
 * والعنوان رابط إلى ملف. لم نتمكن من فحص HTML الفعلي أثناء التطوير، لذلك
 * يعتمد المحلِّل على استراتيجيتين احتياطيتين:
 *  1) معرّف/اسم الوسم (id/name) المطابق لاسم القسم،
 *  2) نص العنوان (h1..h6 أو عنصر فيه نص القسم فقط).
 * وأي رابط لا يقع تحت قسم معروف يُهمَل.
 */
object LibraryParser {

    private val headingTags = setOf("h1", "h2", "h3", "h4", "h5", "h6", "caption", "th", "b", "strong", "p", "div", "span", "td")

    fun parse(html: String, baseUrl: String): Map<String, List<LibraryItem>> {
        val doc = Jsoup.parse(html, baseUrl)
        val known = LibrarySection.values().associateBy { key(it.anchor) }
        val result = linkedMapOf<String, MutableList<LibraryItem>>()
        var current: String? = null

        for (el in doc.body().allElements) {
            // 1) علامة القسم عبر id/name
            val marker = listOf(el.id(), el.attr("name")).firstOrNull { it.isNotBlank() }
            var section = marker?.let { known[key(it)] }

            // 2) أو عبر نص عنصر قصير يطابق اسم القسم تماماً
            if (section == null && el.tagName() in headingTags) {
                val own = el.ownText().trim()
                if (own.length in 3..80) section = known[key(own)]
            }
            if (section != null) current = section.anchor

            if (el.tagName() == "tr" && current != null) {
                val link = el.selectFirst("a[href]") ?: continue
                val url = link.absUrl("href").ifBlank { link.attr("href") }
                if (url.isBlank() || url.startsWith("#")) continue
                val title = link.text().trim().ifBlank { el.text().trim() }
                val number = el.select("td").firstOrNull()?.text()?.trim().orEmpty()
                val items = result.getOrPut(current) { mutableListOf() }
                if (items.none { it.url == url }) items += LibraryItem(number, title, url)
            }
        }
        return result
    }

    private fun key(s: String) = ArabicText.normalize(s.replace('_', ' ').replace('-', ' ')).trim()
}
