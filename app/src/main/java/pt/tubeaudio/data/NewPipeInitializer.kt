package pt.tubeaudio.data

import android.content.Context
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

object NewPipeInitializer {
    @Volatile private var initialized = false
    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            NewPipe.init(OkHttpDownloader(), Localization("pt", "PT"), ContentCountry("PT"))
            initialized = true
        }
    }
}
