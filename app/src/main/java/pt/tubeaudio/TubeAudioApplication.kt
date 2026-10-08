package pt.tubeaudio
import android.app.Application
import pt.tubeaudio.data.NewPipeInitializer
class TubeAudioApplication : Application() {
    override fun onCreate() { super.onCreate(); NewPipeInitializer.initialize(this) }
}
