/*******************************************************************************
 *  ExternalSubRepository.kt
 * ****************************************************************************
 * Copyright © 2018 KPlayer authors and VideoLAN
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
 ******************************************************************************/

package com.kbizsoft.KPlayer.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.kbizsoft.tools.CoroutineContextProvider
import com.kbizsoft.tools.SingletonHolder
import com.kbizsoft.tools.livedata.LiveDataMap
import com.kbizsoft.KPlayer.database.ExternalSubDao
import com.kbizsoft.KPlayer.database.MediaDatabase
import com.kbizsoft.KPlayer.gui.dialogs.State
import com.kbizsoft.KPlayer.gui.dialogs.SubtitleItem
import java.io.File

class ExternalSubRepository(private val externalSubDao: ExternalSubDao, private val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider()) {

    private var _downloadingSubtitles = LiveDataMap<Long, SubtitleItem>()

    @Suppress("UNCHECKED_CAST")
    val downloadingSubtitles: LiveData<Map<Long, SubtitleItem>>
        get() = _downloadingSubtitles as LiveData<Map<Long, SubtitleItem>>

    fun saveDownloadedSubtitle(idSubtitle: String, subtitlePath: String, mediaPath: String, language: String, movieReleaseName: String): Job {
        return GlobalScope.launch(coroutineContextProvider.IO) { externalSubDao.insert(com.kbizsoft.KPlayer.mediadb.models.ExternalSub(idSubtitle, subtitlePath, mediaPath, language, movieReleaseName)) }
    }

    fun getDownloadedSubtitles(mediaUri: Uri): LiveData<List<com.kbizsoft.KPlayer.mediadb.models.ExternalSub>> {
        val externalSubs = externalSubDao.get(mediaUri.path!!)
        return externalSubs.map { list ->
            val existExternalSubs: MutableList<com.kbizsoft.KPlayer.mediadb.models.ExternalSub> = mutableListOf()
            list.forEach {
                if (File(Uri.decode(it.subtitlePath)).exists())
                    existExternalSubs.add(it)
                else
                    deleteSubtitle(it.mediaPath, it.idSubtitle)
            }
            existExternalSubs
        }
    }

    fun deleteSubtitle(mediaPath: String, idSubtitle: String) {
        GlobalScope.launch { externalSubDao.delete(mediaPath, idSubtitle) }
    }

    fun addDownloadingItem(key: Long, item: SubtitleItem) {
        _downloadingSubtitles.add(key, item.copy(state = State.Downloading))
    }

    fun removeDownloadingItem(key: Long) {
        _downloadingSubtitles.remove(key)
    }

    fun getDownloadingSubtitle(key: Long) = _downloadingSubtitles.get(key)

    companion object : SingletonHolder<ExternalSubRepository, Context>({ ExternalSubRepository(MediaDatabase.getInstance(it).externalSubDao()) })
}