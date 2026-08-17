package com.biito.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.biito.player.ui.player.PlayerScreen
import com.biito.player.ui.theme.BiitoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiitoTheme {
                PlayerScreen()
            }
        }
    }
}