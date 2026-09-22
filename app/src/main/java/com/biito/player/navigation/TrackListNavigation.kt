package com.biito.player.navigation

import com.biito.player.ui.feature.tracklist.TrackListScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object TrackListRoute

@Module
@InstallIn(ActivityRetainedComponent::class)
object TrackListNavigationModule {

    @IntoSet
    @Provides
    fun provideEntryProviderInstaller(navigator: Navigator): EntryProviderInstaller = {
        entry<TrackListRoute> {
            TrackListScreen(
                onNavigateToPlayer = { navigator.goTo(PlayerRoute) }
            )
        }
    }
}
