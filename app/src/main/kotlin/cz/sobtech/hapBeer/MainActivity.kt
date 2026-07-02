package cz.sobtech.hapBeer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.sobtech.hapBeer.navigation.PivoNavGraph
import cz.sobtech.hapBeer.ui.theme.HapBeerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HapBeerTheme {
                PivoNavGraph()
            }
        }
    }
}
