/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.bookmarks.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {

    var shouldDisplayUndoBookmark by mutableStateOf(false)
    private var lastRemovedBookmarkId: String? = null
    private var lastRemovedBookmarkNote: String? = null

    // Multi-select mode state
    var isInSelectionMode by mutableStateOf(false)
    var selectedBookmarkIds by mutableStateOf(emptySet<String>())
    var shouldDisplayUndoBulkRemoval by mutableStateOf(false)
    var bulkRemovedCount by mutableIntStateOf(0)
    private var lastBulkRemovedBookmarks: Map<String, String> = emptyMap()

    val feedUiState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllBookmarked()
            .map<List<UserNewsResource>, NewsFeedUiState>(NewsFeedUiState::Success)
            .onStart { emit(Loading) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Loading,
            )

    fun removeFromSavedResources(newsResourceId: String) {
        val feed = (feedUiState.value as? NewsFeedUiState.Success)?.feed ?: emptyList()
        val currentNote = feed.find { it.id == newsResourceId }?.bookmarkNote
        viewModelScope.launch {
            shouldDisplayUndoBookmark = true
            lastRemovedBookmarkId = newsResourceId
            lastRemovedBookmarkNote = currentNote
            userDataRepository.setNewsResourceBookmarked(newsResourceId, false)
        }
    }

    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    fun undoBookmarkRemoval() {
        viewModelScope.launch {
            lastRemovedBookmarkId?.let { id ->
                userDataRepository.setNewsResourceBookmarked(id, true)
                lastRemovedBookmarkNote?.let { note ->
                    userDataRepository.updateBookmarkNote(id, note)
                }
            }
        }
        clearUndoState()
    }

    fun clearUndoState() {
        shouldDisplayUndoBookmark = false
        lastRemovedBookmarkId = null
        lastRemovedBookmarkNote = null
    }

    fun enterSelectionMode(initialSelectedId: String? = null) {
        isInSelectionMode = true
        selectedBookmarkIds = initialSelectedId?.let { setOf(it) } ?: emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedBookmarkIds = emptySet()
    }

    fun toggleBookmarkSelection(newsResourceId: String) {
        selectedBookmarkIds = if (newsResourceId in selectedBookmarkIds) {
            selectedBookmarkIds - newsResourceId
        } else {
            selectedBookmarkIds + newsResourceId
        }
    }

    fun selectAllBookmarks(allIds: Set<String>) {
        selectedBookmarkIds = if (selectedBookmarkIds.size == allIds.size) {
            emptySet()
        } else {
            allIds
        }
    }

    fun removeSelectedBookmarks() {
        if (selectedBookmarkIds.isEmpty()) return

        val feed = (feedUiState.value as? NewsFeedUiState.Success)?.feed ?: emptyList()
        val toRemove = feed.filter { it.id in selectedBookmarkIds }
        val notesMap = toRemove.associate { it.id to (it.bookmarkNote ?: "") }
        val idsToRemove = selectedBookmarkIds

        bulkRemovedCount = idsToRemove.size
        lastBulkRemovedBookmarks = notesMap
        shouldDisplayUndoBulkRemoval = true

        viewModelScope.launch {
            userDataRepository.removeBookmarkedNewsResources(idsToRemove)
        }
        exitSelectionMode()
    }

    fun undoBulkRemoval() {
        val toRestore = lastBulkRemovedBookmarks
        viewModelScope.launch {
            userDataRepository.restoreBookmarkedNewsResources(toRestore.keys, toRestore)
        }
        clearBulkUndoState()
    }

    fun clearBulkUndoState() {
        shouldDisplayUndoBulkRemoval = false
        lastBulkRemovedBookmarks = emptyMap()
        bulkRemovedCount = 0
    }

    fun updateBookmarkNote(newsResourceId: String, note: String) {
        viewModelScope.launch {
            userDataRepository.updateBookmarkNote(newsResourceId, note)
        }
    }

    fun deleteBookmarkNote(newsResourceId: String) {
        viewModelScope.launch {
            userDataRepository.updateBookmarkNote(newsResourceId, null)
        }
    }
}
