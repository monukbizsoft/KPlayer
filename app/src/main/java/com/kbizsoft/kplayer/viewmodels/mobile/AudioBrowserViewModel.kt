/*****************************************************************************
 * AudioBrowserViewModel.kt
 *****************************************************************************
 * Copyright © 2019 KPlayer authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.kbizsoft.KPlayer.viewmodels.mobile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.kbizsoft.medialibrary.interfaces.media.Playlist
import com.kbizsoft.resources.KEY_AUDIO_CURRENT_TAB
import com.kbizsoft.tools.KEY_ARTISTS_SHOW_ALL
import com.kbizsoft.tools.Settings
import com.kbizsoft.KPlayer.gui.audio.AudioBrowserFragment
import com.kbizsoft.KPlayer.providers.medialibrary.*
import com.kbizsoft.KPlayer.viewmodels.MedialibraryViewModel

class AudioBrowserViewModel(context: Context) : MedialibraryViewModel(context) {

    var currentTab = Settings.getInstance(context).getInt(KEY_AUDIO_CURRENT_TAB, 0)
    val artistsProvider = ArtistsProvider(context, this,
            Settings.getInstance(context).getBoolean(KEY_ARTISTS_SHOW_ALL, false))
    val albumsProvider = AlbumsProvider(null, context, this)
    val tracksProvider = TracksProvider(null, context, this)
    val genresProvider = GenresProvider(context, this)
    private val playlistsProvider = PlaylistsProvider(context, this, Playlist.Type.Audio)
    override val providers = arrayOf(artistsProvider, albumsProvider, tracksProvider, genresProvider, playlistsProvider)
    val providersInCard = arrayOf(true, true, false, false, true)

    var showResumeCard = settings.getBoolean("audio_resume_card", true)
    val displayModeKeys = arrayOf("display_mode_audio_browser_artists", "display_mode_audio_browser_albums", "display_mode_audio_browser_track", "display_mode_audio_browser_genres", "display_mode_playlists_AudioOnly")


    init {
        watchAlbums()
        watchArtists()
        watchGenres()
        watchMedia()
        watchPlaylists()
        //Initial state coming from preferences and falling back to [providersInCard] hardcoded values
        for (i in displayModeKeys.indices) {
            providersInCard[i] = settings.getBoolean(displayModeKeys[i], providersInCard[i])
        }

    }

    override fun refresh() {
        artistsProvider.showAll = settings.getBoolean(KEY_ARTISTS_SHOW_ALL, false)
        viewModelScope.launch {
            if (currentTab < providers.size) providers[currentTab].awaitRefresh()
            for ((index, provider) in providers.withIndex()) {
                if (index != currentTab && provider.loading.hasObservers()) provider.awaitRefresh()
            }
        }
    }

    class Factory(val context: Context): ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AudioBrowserViewModel(context.applicationContext) as T
        }
    }
}

internal fun AudioBrowserFragment.getViewModel() = ViewModelProvider(requireActivity(), AudioBrowserViewModel.Factory(requireContext())).get(AudioBrowserViewModel::class.java)