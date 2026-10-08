package pt.tubeaudio

import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import pt.tubeaudio.data.OkHttpDownloader

/** Live integration check: run explicitly when diagnosing upstream search failures. */
class LiveSearchTest {
    @Test fun youtubeSearchReturnsTracks() {
        NewPipe.init(OkHttpDownloader(), Localization("pt", "PT"), ContentCountry("PT"))
        val service = NewPipe.getService("YouTube")
        val query = service.searchQHFactory.fromQuery("estou na festa")
        val info = SearchInfo.getInfo(service, query)
        assertTrue("YouTube devolveu resultados vazios", info.relatedItems.isNotEmpty())
    }
}
