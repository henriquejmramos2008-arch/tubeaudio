package pt.tubeaudio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.tubeaudio.ui.PlayerScreen
import pt.tubeaudio.ui.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val askNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFA898FF),
                onPrimary = Color(0xFF0C101B),
                background = Color(0xFF0C101B),
                onBackground = Color.White,
                surface = Color(0xFF191F30),
                onSurface = Color.White
            )) { PlayerScreen(viewModel()) }
        }
    }
}
