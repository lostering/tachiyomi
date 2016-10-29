package eu.kanade.tachiyomi.data.source.online.vietnamese

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.network.GET
import eu.kanade.tachiyomi.data.network.POST
import eu.kanade.tachiyomi.data.source.VN
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.model.MangasPage
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.ParsedOnlineSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class Blogtruyen(override val id: Int) : ParsedOnlineSource() {

    override val name = "BlogTruyen"

    override val baseUrl = "http://blogtruyen.com"

    override val lang: Language get() = VN

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun popularMangaInitialUrl() = "$baseUrl"

    override fun latestUpdatesInitialUrl() = "$baseUrl"

    override fun popularMangaSelector() = "p:has(span.ellipsis)"

    override fun latestUpdatesSelector() = "div.bg-white.storyitem div.fl-r"

    override fun popularMangaFromElement(element: Element, manga: Manga) {
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text().trim()
        }
    }

    override fun latestUpdatesFromElement(element: Element, manga: Manga) {
        element.select("a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text().trim()
            //manga.title = it.attr("href").toString().substringAfterLast('/')
        }
    }

    override fun popularMangaParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(popularMangaSelector())) {
            Manga.create(id).apply {
                popularMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = "$baseUrl/ajax/Search/AjaxLoadListManga?key=tatca&orderBy=3&p=${page.page + 1}"

    }

    override fun latestUpdatesParse(response: Response, page: MangasPage) {
        val document = response.asJsoup()
        for (element in document.select(latestUpdatesSelector())) {
            Manga.create(id).apply {
                latestUpdatesFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = "$baseUrl/page-${page.page + 1}"
    }

    override fun popularMangaNextPageSelector() = null

    override fun latestUpdatesNextPageSelector() = null


    override fun searchMangaInitialUrl(query: String, filters: List<Filter>) = "$baseUrl/timkiem/nangcao/1/0/${getFilterParams(filters)}/-1?txt=${Uri.encode(query)}"

    private fun getFilterParams(filters: List<Filter>): String = filters
            .map {
                ";i" + it.id
            }.joinToString()

    override fun searchMangaRequest(page: MangasPage, query: String, filters: List<Filter>): Request {
        if (page.page == 1) {
            page.url = searchMangaInitialUrl(query, filters)
        }
        return GET(page.url, headers)
    }

    override fun searchMangaParse(response: Response, page: MangasPage, query: String, filters: List<Filter>) {
        val document = response.asJsoup()
        for (element in document.select(searchMangaSelector())) {
            Manga.create(id).apply {
                searchMangaFromElement(element, this)
                page.mangas.add(this)
            }
        }

        page.nextPageUrl = "$baseUrl/timkiem/nangcao/1/0/${getFilterParams(filters)}/-1?txt=${Uri.encode(query)}&p=${page.page + 1}"

    }



    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element, manga: Manga) {
        popularMangaFromElement(element, manga)
    }

    override fun searchMangaNextPageSelector() = null

    override fun mangaDetailsParse(document: Document, manga: Manga) {
        val infoElement = document.select("section.manga-detail").first()

        manga.author = infoElement.select("p:contains(Tác giả:)").text().toString().substringAfterLast(':')
        manga.title = document.title().substringBeforeLast('|')
        manga.genre = infoElement.select("p:contains(Thể loại:) > *:gt(0)").text()
        manga.description = infoElement.select("div.content").text()
        manga.status = infoElement.select("p:contains(Trạng thái:)").text().toString().substringAfterLast(':').orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select("div.thumbnail img").first()?.attr("src")


    }

    fun parseStatus(status: String) = when {
        status.contains("Đang tiến hành") -> Manga.ONGOING
        status.contains("Hoàn Thành") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

    override fun chapterListSelector() = "div#list-chapters p"

    override fun chapterFromElement(element: Element, chapter: Chapter) {
        val urlElement = element.select("a").first()

        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = element.select("span.publishedDate").first()?.text()?.let {
            SimpleDateFormat("dd/MM/yyyy").parse(it).time
        } ?: 0
    }

    override fun pageListRequest(chapter: Chapter) = POST(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response, pages: MutableList<Page>) {
        //language=RegExp
        val p = Pattern.compile("""img src="(.+?)"""")
        val m = p.matcher(response.asJsoup().select("article#content").toString())
        var i = 0
        while (m.find()) {
            pages.add(Page(i++, "", m.group(1)))
        }
    }

    // Not used
    override fun pageListParse(document: Document, pages: MutableList<Page>) {
    }

    override fun imageUrlRequest(page: Page) = GET(page.url)

    override fun imageUrlParse(document: Document) = ""

    // $("select[name=\"genres\"]").map((i,el) => `Filter("${i}", "${$(el).next().text().trim()}")`).get().join(',\n')
    // on http://kissmanga.com/AdvanceSearch
    override fun getFilterList(): List<Filter> = listOf(
            Filter("0", "Action"),
            Filter("1", "Adult"),
            Filter("2", "Adventure"),
            Filter("3", "Comedy"),
            Filter("4", "Comic"),
            Filter("5", "Cooking"),
            Filter("6", "Doujinshi"),
            Filter("7", "Drama"),
            Filter("8", "Ecchi"),
            Filter("9", "Fantasy"),
            Filter("10", "Gender Bender"),
            Filter("11", "Harem"),
            Filter("12", "Historical"),
            Filter("13", "Horror"),
            Filter("14", "Josei"),
            Filter("15", "Lolicon"),
            Filter("16", "Manga"),
            Filter("17", "Manhua"),
            Filter("18", "Manhwa"),
            Filter("19", "Martial Arts"),
            Filter("20", "Mature"),
            Filter("21", "Mecha"),
            Filter("22", "Medical"),
            Filter("23", "Music"),
            Filter("24", "Mystery"),
            Filter("25", "One shot"),
            Filter("26", "Psychological"),
            Filter("27", "Romance"),
            Filter("28", "School Life"),
            Filter("29", "Sci-fi"),
            Filter("30", "Seinen"),
            Filter("31", "Shotacon"),
            Filter("32", "Shoujo"),
            Filter("33", "Shoujo Ai"),
            Filter("34", "Shounen"),
            Filter("35", "Shounen Ai"),
            Filter("36", "Slice of Life"),
            Filter("37", "Smut"),
            Filter("38", "Sports"),
            Filter("39", "Supernatural"),
            Filter("40", "Tragedy"),
            Filter("41", "Webtoon"),
            Filter("42", "Yaoi"),
            Filter("43", "Yuri")
    )
}
