package dz.baladiya.check.library

import android.content.Context
import android.net.Uri
import dz.baladiya.check.rules.LibrarySection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * الربط بمكتبة https://cb-maktaba.net/gestion_du_personnel.php
 *
 * - لا توجد واجهة برمجية في الموقع؛ نقرأ صفحة HTML ونخزّنها محلياً (ذاكرة مؤقتة).
 * - إن تعذّر الاتصال نستعمل آخر نسخة مخزّنة، وإن لم توجد نكتفي برابط القسم.
 * - يُنصح بالتواصل مع صاحب الموقع للحصول على إذن/نسخة فهرس رسمية.
 */
class LibraryRepository(context: Context) {

    companion object {
        const val PAGE_URL = "https://cb-maktaba.net/gestion_du_personnel.php"
        private const val CACHE_NAME = "library_index.json"
        private const val TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    private val cacheFile = File(context.filesDir, CACHE_NAME)
    @Volatile private var index: LibraryIndex? = loadCache()

    val current: LibraryIndex? get() = index

    /** رابط القسم داخل الصفحة (يعمل حتى دون فهرس محلي). */
    fun sectionUrl(section: LibrarySection): String = PAGE_URL + "#" + Uri.encode(section.anchor)

    /** وثائق القسم المقروءة من الصفحة (قد تكون فارغة). */
    fun itemsFor(section: LibrarySection, limit: Int = 8): List<LibraryItem> =
        index?.sections?.get(section.anchor).orEmpty().take(limit)

    fun isStale(): Boolean {
        val i = index ?: return true
        return System.currentTimeMillis() - i.fetchedAtMillis > TTL_MILLIS
    }

    /** يحدّث الفهرس من الإنترنت. يعيد عدد الوثائق، أو null عند الفشل. */
    suspend fun refresh(): Int? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(PAGE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "BaladiyaCheck/0.1 (Android; librairie-index)")
                setRequestProperty("Accept-Language", "ar")
            }
            if (conn.responseCode != 200) return@withContext null
            val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = LibraryParser.parse(html, PAGE_URL)
            if (parsed.isEmpty()) return@withContext null
            val fresh = LibraryIndex(System.currentTimeMillis(), parsed)
            index = fresh
            saveCache(fresh)
            parsed.values.sumOf { it.size }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCache(i: LibraryIndex) {
        val root = JSONObject().put("fetchedAt", i.fetchedAtMillis)
        val secs = JSONObject()
        for ((k, items) in i.sections) {
            val arr = JSONArray()
            items.forEach { arr.put(JSONObject().put("n", it.number).put("t", it.title).put("u", it.url)) }
            secs.put(k, arr)
        }
        root.put("sections", secs)
        cacheFile.writeText(root.toString())
    }

    private fun loadCache(): LibraryIndex? = try {
        if (!cacheFile.exists()) null else {
            val root = JSONObject(cacheFile.readText())
            val secs = root.getJSONObject("sections")
            val map = linkedMapOf<String, List<LibraryItem>>()
            for (k in secs.keys()) {
                val arr = secs.getJSONArray(k)
                map[k] = List(arr.length()) {
                    val o = arr.getJSONObject(it)
                    LibraryItem(o.getString("n"), o.getString("t"), o.getString("u"))
                }
            }
            LibraryIndex(root.getLong("fetchedAt"), map)
        }
    } catch (e: Exception) {
        null
    }
}
