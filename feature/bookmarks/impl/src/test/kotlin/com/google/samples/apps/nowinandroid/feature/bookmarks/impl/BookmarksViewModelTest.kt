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

import com.google.samples.apps.nowinandroid.core.data.repository.CompositeUserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.testing.data.newsResourcesTestData
import com.google.samples.apps.nowinandroid.core.testing.repository.TestNewsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.util.MainDispatcherRule
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * To learn more about how this test handles Flows created with stateIn, see
 * https://developer.android.com/kotlin/flow/test#statein
 */
class BookmarksViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val userDataRepository = TestUserDataRepository()
    private val newsRepository = TestNewsRepository()
    private val userNewsResourceRepository = CompositeUserNewsResourceRepository(
        newsRepository = newsRepository,
        userDataRepository = userDataRepository,
    )
    private lateinit var viewModel: BookmarksViewModel

    @Before
    fun setup() {
        viewModel = BookmarksViewModel(
            userDataRepository = userDataRepository,
            userNewsResourceRepository = userNewsResourceRepository,
        )
    }

    @Test
    fun stateIsInitiallyLoading() = runTest {
        assertEquals(Loading, viewModel.feedUiState.value)
    }

    @Test
    fun oneBookmark_showsInFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(item.feed.size, 1)
    }

    @Test
    fun oneBookmark_whenRemoving_removesFromFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        // Set the news resources to be used by this test
        newsRepository.sendNewsResources(newsResourcesTestData)
        // Start with the resource saved
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        // Use viewModel to remove saved resource
        viewModel.removeFromSavedResources(newsResourcesTestData[0].id)
        // Verify list of saved resources is now empty
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(item.feed.size, 0)
        assertTrue(viewModel.shouldDisplayUndoBookmark)
    }

    @Test
    fun feedUiState_resourceIsViewed_setResourcesViewed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        // Given
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        val itemBeforeViewed = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeViewed)
        assertFalse(itemBeforeViewed.feed.first().hasBeenViewed)

        // When
        viewModel.setNewsResourceViewed(newsResourcesTestData[0].id, true)

        // Then
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertTrue(item.feed.first().hasBeenViewed)
    }

    @Test
    fun feedUiState_undoneBookmarkRemoval_bookmarkIsRestored() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        // Given
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        viewModel.removeFromSavedResources(newsResourcesTestData[0].id)
        assertTrue(viewModel.shouldDisplayUndoBookmark)
        val itemBeforeUndo = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeUndo)
        assertEquals(0, itemBeforeUndo.feed.size)

        // When
        viewModel.undoBookmarkRemoval()

        // Then
        assertFalse(viewModel.shouldDisplayUndoBookmark)
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(1, item.feed.size)
    }

    @Test
    fun bookmarkNote_showsInFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        userDataRepository.updateBookmarkNote(newsResourcesTestData[0].id, "Interesting read")

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals("Interesting read", item.feed.first().bookmarkNote)
    }

    @Test
    fun updateBookmarkNote_updatesNoteInFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)

        viewModel.updateBookmarkNote(newsResourcesTestData[0].id, "Updated personal note")

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals("Updated personal note", item.feed.first().bookmarkNote)
    }

    @Test
    fun deleteBookmarkNote_removesNoteFromFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        userDataRepository.updateBookmarkNote(newsResourcesTestData[0].id, "To delete")

        viewModel.deleteBookmarkNote(newsResourcesTestData[0].id)

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertNull(item.feed.first().bookmarkNote)
    }

    @Test
    fun removeFromSavedResources_cleansUpNote_andUndoRestoresBoth() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        userDataRepository.updateBookmarkNote(newsResourcesTestData[0].id, "Restorable note")

        viewModel.removeFromSavedResources(newsResourcesTestData[0].id)
        assertTrue(viewModel.shouldDisplayUndoBookmark)

        val itemBeforeUndo = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeUndo)
        assertEquals(0, itemBeforeUndo.feed.size)

        // When
        viewModel.undoBookmarkRemoval()

        // Then both bookmark and note are restored
        val itemAfterUndo = viewModel.feedUiState.value
        assertIs<Success>(itemAfterUndo)
        assertEquals(1, itemAfterUndo.feed.size)
        assertEquals("Restorable note", itemAfterUndo.feed.first().bookmarkNote)
    }

    @Test
    fun selectionMode_enterExitAndToggle() = runTest {
        assertFalse(viewModel.isInSelectionMode)
        assertTrue(viewModel.selectedBookmarkIds.isEmpty())

        viewModel.enterSelectionMode("1")
        assertTrue(viewModel.isInSelectionMode)
        assertEquals(setOf("1"), viewModel.selectedBookmarkIds)

        viewModel.toggleBookmarkSelection("2")
        assertEquals(setOf("1", "2"), viewModel.selectedBookmarkIds)

        viewModel.toggleBookmarkSelection("1")
        assertEquals(setOf("2"), viewModel.selectedBookmarkIds)

        viewModel.exitSelectionMode()
        assertFalse(viewModel.isInSelectionMode)
        assertTrue(viewModel.selectedBookmarkIds.isEmpty())
    }

    @Test
    fun selectAllBookmarks_togglesAll() = runTest {
        val allIds = setOf("1", "2", "3")

        viewModel.enterSelectionMode("1")
        assertEquals(setOf("1"), viewModel.selectedBookmarkIds)

        // When not all are selected, selectAll selects all
        viewModel.selectAllBookmarks(allIds)
        assertEquals(allIds, viewModel.selectedBookmarkIds)

        // When all are already selected, selectAll deselects all
        viewModel.selectAllBookmarks(allIds)
        assertTrue(viewModel.selectedBookmarkIds.isEmpty())
    }

    @Test
    fun removeSelectedBookmarks_bulkRemovesAndDeletesNotes_andUndoRestoresAll() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        val id1 = newsResourcesTestData[0].id
        val id2 = newsResourcesTestData[1].id

        userDataRepository.setNewsResourceBookmarked(id1, true)
        userDataRepository.updateBookmarkNote(id1, "Note 1")
        userDataRepository.setNewsResourceBookmarked(id2, true)
        userDataRepository.updateBookmarkNote(id2, "Note 2")

        val initialFeed = viewModel.feedUiState.value
        assertIs<Success>(initialFeed)
        assertEquals(2, initialFeed.feed.size)

        // Enter selection mode and select both
        viewModel.enterSelectionMode()
        viewModel.selectAllBookmarks(setOf(id1, id2))

        // Bulk remove
        viewModel.removeSelectedBookmarks()

        assertFalse(viewModel.isInSelectionMode)
        assertTrue(viewModel.shouldDisplayUndoBulkRemoval)
        assertEquals(2, viewModel.bulkRemovedCount)

        val feedAfterRemove = viewModel.feedUiState.value
        assertIs<Success>(feedAfterRemove)
        assertEquals(0, feedAfterRemove.feed.size)

        // Undo bulk removal
        viewModel.undoBulkRemoval()

        assertFalse(viewModel.shouldDisplayUndoBulkRemoval)
        val feedAfterUndo = viewModel.feedUiState.value
        assertIs<Success>(feedAfterUndo)
        assertEquals(2, feedAfterUndo.feed.size)

        val restored1 = feedAfterUndo.feed.find { it.id == id1 }
        val restored2 = feedAfterUndo.feed.find { it.id == id2 }
        assertEquals("Note 1", restored1?.bookmarkNote)
        assertEquals("Note 2", restored2?.bookmarkNote)
    }
}
