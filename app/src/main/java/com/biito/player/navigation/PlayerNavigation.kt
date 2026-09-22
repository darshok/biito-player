package com.biito.player.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import com.biito.player.ui.feature.player.PlayerScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object PlayerRoute

@Module
@InstallIn(ActivityRetainedComponent::class)
object PlayerNavigationModule {

    @IntoSet
    @Provides
    fun provideEntryProviderInstaller(navigator: Navigator): EntryProviderInstaller = {
        entry<PlayerRoute>(
            metadata = metadata {
                put(NavDisplay.TransitionKey) {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(500)
                    ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                }
                put(NavDisplay.PopTransitionKey) {
                    EnterTransition.None togetherWith slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(500)
                    )
                }
            }
        ) {
            PlayerScreen(
                onBackClick = { navigator.goBack() }
            )
        }
    }
}
