package dz.baladiya.check.library

/** وثيقة في مكتبة cb-maktaba.net (الرابط يشير عادةً إلى ملف على Google Drive). */
data class LibraryItem(
    val number: String,
    val title: String,
    val url: String
)

/** فهرس مقروء من الصفحة: معرّف القسم ← وثائقه. */
data class LibraryIndex(
    val fetchedAtMillis: Long,
    val sections: Map<String, List<LibraryItem>>
) {
    val isEmpty get() = sections.values.all { it.isEmpty() }
}
