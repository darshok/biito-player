package com.biito.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.biito.player.navigation.EntryProviderInstaller
import com.biito.player.navigation.Navigator
import com.biito.player.ui.theme.BiitoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var entryProviderScopes: @JvmSuppressWildcards Set<EntryProviderInstaller>

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiitoTheme {
                Scaffold { paddingValues ->
                    SharedTransitionLayout {
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides this
                        ) {
                            NavDisplay(
                                backStack = navigator.backStack,
                                modifier = Modifier.padding(paddingValues),
                                onBack = { navigator.goBack() },
                                entryProvider = entryProvider {
                                    entryProviderScopes.forEach { builder -> this.builder() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}