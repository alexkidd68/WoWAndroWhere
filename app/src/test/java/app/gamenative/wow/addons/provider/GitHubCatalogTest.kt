package app.gamenative.wow.addons.provider

import app.gamenative.wow.addons.AddonCategory
import app.gamenative.wow.addons.AddonFlavor
import app.gamenative.wow.addons.SourceRef
import app.gamenative.wow.addons.provider.Fixtures.base
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubCatalogTest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<String>()
    private var catalogAvailable = true
    private var now = 0L

    private fun start(fallback: Boolean = false): GitHubProvider {
        server.dispatcher = Fixtures.dispatcher { req ->
            requests += req.url.encodedPath
            when (req.url.encodedPath) {
                "/catalog.json" -> if (catalogAvailable) Fixtures.json(Fixtures.load("github-catalog.json")) else null
                "/upstream/catalog.json" -> Fixtures.json(Fixtures.load("github-catalog.json"))
                else -> null
            }
        }
        server.start()
        return GitHubProvider(
            OkHttpClient(),
            apiBase = server.base(),
            catalogUrl = "${server.base()}/catalog.json",
            fallbackCatalogUrl = if (fallback) "${server.base()}/upstream/catalog.json" else null,
            clock = { now },
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun popularListsCatalogWithFlavorsAndCategories() = runBlocking {
        val addons = start().popular(0).associateBy { it.projectId }
        assertEquals(setOf("Questie/Questie", "BigWigsMods/LittleWigs", "Nevcairiel/Mapster"), addons.keys)
        val questie = addons.getValue("Questie/Questie")
        assertEquals(setOf(AddonFlavor.MAIN, AddonFlavor.FALLBACK), questie.latest.keys)
        assertEquals(setOf(AddonCategory.QUESTS), questie.categories)
        assertTrue(questie.latest.getValue(AddonFlavor.MAIN).downloadUrl.startsWith("https://github.com/Questie/Questie/releases/download/"))
        assertEquals(setOf(AddonFlavor.MAIN), addons.getValue("Nevcairiel/Mapster").latest.keys)
        assertEquals(
            listOf(SourceRef("github", "BigWigsMods/BigWigs")),
            addons.getValue("BigWigsMods/LittleWigs").latest.getValue(AddonFlavor.FALLBACK).dependencies,
        )
    }

    @Test
    fun searchAndCategoryFilterTheCatalog() = runBlocking {
        val provider = start()
        assertEquals(listOf("Questie/Questie"), provider.search("quest", 0).map { it.projectId })
        assertEquals(listOf("Nevcairiel/Mapster"), provider.popular(0, AddonCategory.MAP).map { it.projectId })
        assertTrue(provider.search("quest", 1).isEmpty())
    }

    @Test
    fun catalogAddonsNeedNoApiCalls() = runBlocking {
        val provider = start()
        val questie = provider.get("Questie/Questie")!!
        assertEquals("Questie", questie.name)
        provider.getMany(listOf("Nevcairiel/Mapster", "BigWigsMods/LittleWigs"))
        assertTrue(requests.none { it.startsWith("/repos/") })
    }

    @Test
    fun catalogIsCachedForSixHours() = runBlocking {
        val provider = start()
        provider.popular(0)
        now += 5 * 60 * 60 * 1000L
        provider.popular(0)
        assertEquals(1, requests.count { it == "/catalog.json" })
        now += 2 * 60 * 60 * 1000L
        provider.popular(0)
        assertEquals(2, requests.count { it == "/catalog.json" })
    }

    @Test
    fun missingCatalogMeansNoResults() = runBlocking {
        catalogAvailable = false
        assertTrue(start().popular(0).isEmpty())
    }

    @Test
    fun fallbackCatalogIsNotFetchedWhenOursWorks() = runBlocking {
        assertEquals(3, start(fallback = true).popular(0).size)
        assertTrue(requests.none { it == "/upstream/catalog.json" })
    }

    @Test
    fun fallbackCatalogIsUsedWhenOursIsMissing() = runBlocking {
        catalogAvailable = false
        assertEquals(3, start(fallback = true).popular(0).size)
        assertTrue("/upstream/catalog.json" in requests)
    }
}
