package eu.kanade.tachiyomi.data.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.YamlOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.*
import eu.kanade.tachiyomi.data.source.online.german.WieManga
import eu.kanade.tachiyomi.data.source.online.russian.Mangachan
import eu.kanade.tachiyomi.data.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.data.source.online.russian.Readmanga
import eu.kanade.tachiyomi.data.source.online.vietnamese.Blogtruyen
import eu.kanade.tachiyomi.util.hasPermission
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File

open class SourceManager(private val context: Context) {

    private val sourcesMap = createSources()

    open fun get(sourceKey: Int): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    private fun createOnlineSourceList(): List<Source> = listOf(
            Batoto(1),
            Mangahere(2),
            Mangafox(3),
            Kissmanga(4),
            Readmanga(5),
            Mintmanga(6),
            Mangachan(7),
            Readmangatoday(8),
            Mangasee(9),
            WieManga(10),
            Blogtruyen(11)
    )

    private fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        createOnlineSourceList().forEach { put(it.id, it) }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists() && context.hasPermission(READ_EXTERNAL_STORAGE)) {
            val yaml = Yaml()
            for (file in parsersDir.listFiles().filter { it.extension == "yml" }) {
                try {
                    val map = file.inputStream().use { yaml.loadAs(it, Map::class.java) }
                    YamlOnlineSource(map).let { put(it.id, it) }
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }
    }

}
