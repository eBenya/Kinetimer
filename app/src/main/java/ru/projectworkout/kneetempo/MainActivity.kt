package ru.projectworkout.kneetempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import ru.projectworkout.kneetempo.ui.WorkoutApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF176B58),
                        onPrimary = Color.White,
                        secondary = Color(0xFFE9A23B),
                        background = Color(0xFFF7F8F5),
                        surface = Color.White,
                    ),
            ) {
                WorkoutApp()
            }
        }
    }
}
