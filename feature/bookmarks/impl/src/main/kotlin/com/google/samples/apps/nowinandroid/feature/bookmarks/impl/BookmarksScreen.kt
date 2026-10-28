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

import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.DraggableScrollbar
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.rememberDraggableScroller
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.scrollbarState
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalTintTheme
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import com.google.samples.apps.nowinandroid.core.ui.TrackScreenViewEvent
import com.google.samples.apps.nowinandroid.core.ui.TrackScrollJank
import com.google.samples.apps.nowinandroid.core.ui.UserNewsResourcePreviewParameterProvider
import com.google.samples.apps.nowinandroid.core.ui.newsFeed
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.R

@Composable
internal fun BookmarksScreen(
    onTopicClick: (String) -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedUiState.collectAsStateWithLifecycle()
    BookmarksScreen(
        feedState = feedState,
        onShowSnackbar = onShowSnackbar,
        removeFromBookmarks = viewModel::removeFromSavedResources,
        onNewsResourceViewed = { viewModel.setNewsResourceViewed(it, true) },
        onTopicClick = onTopicClick,
        modifier = modifier,
        shouldDisplayUndoBookmark = viewModel.shouldDisplayUndoBookmark,
        undoBookmarkRemoval = viewModel::undoBookmarkRemoval,
        clearUndoState = viewModel::clearUndoState,
        isInSelectionMode = viewModel.isInSelectionMode,
        selectedBookmarkIds = viewModel.selectedBookmarkIds,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onExitSelectionMode = viewModel::exitSelectionMode,
        onToggleSelect = viewModel::toggleBookmarkSelection,
        onSelectAll = viewModel::selectAllBookmarks,
        onRemoveSelected = viewModel::removeSelectedBookmarks,
        shouldDisplayUndoBulkRemoval = viewModel.shouldDisplayUndoBulkRemoval,
        bulkRemovedCount = viewModel.bulkRemovedCount,
        undoBulkRemoval = viewModel::undoBulkRemoval,
        clearBulkUndoState = viewModel::clearBulkUndoState,
        onEditNote = viewModel::updateBookmarkNote,
        onDeleteNote = viewModel::deleteBookmarkNote,
    )
}

/**
 * Displays the user's bookmarked articles. Includes support for loading and empty states.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun BookmarksScreen(
    feedState: NewsFeedUiState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    shouldDisplayUndoBookmark: Boolean = false,
    undoBookmarkRemoval: () -> Unit = {},
    clearUndoState: () -> Unit = {},
    isInSelectionMode: Boolean = false,
    selectedBookmarkIds: Set<String> = emptySet(),
    onEnterSelectionMode: (String?) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onSelectAll: (Set<String>) -> Unit = {},
    onRemoveSelected: () -> Unit = {},
    shouldDisplayUndoBulkRemoval: Boolean = false,
    bulkRemovedCount: Int = 0,
    undoBulkRemoval: () -> Unit = {},
    clearBulkUndoState: () -> Unit = {},
    onEditNote: (String, String) -> Unit = { _, _ -> },
    onDeleteNote: (String) -> Unit = {},
) {
    val bookmarkRemovedMessage = stringResource(id = R.string.feature_bookmarks_api_removed)
    val undoText = stringResource(id = R.string.feature_bookmarks_api_undo)
    val bulkRemovedMessage = stringResource(
        id = R.string.feature_bookmarks_api_bulk_removed,
        bulkRemovedCount,
    )

    LaunchedEffect(shouldDisplayUndoBookmark) {
        if (shouldDisplayUndoBookmark) {
            val snackBarResult = onShowSnackbar(bookmarkRemovedMessage, undoText)
            if (snackBarResult) {
                undoBookmarkRemoval()
            } else {
                clearUndoState()
            }
        }
    }

    LaunchedEffect(shouldDisplayUndoBulkRemoval) {
        if (shouldDisplayUndoBulkRemoval) {
            val snackBarResult = onShowSnackbar(bulkRemovedMessage, undoText)
            if (snackBarResult) {
                undoBulkRemoval()
            } else {
                clearBulkUndoState()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        clearUndoState()
        clearBulkUndoState()
    }

    BackHandler(enabled = isInSelectionMode) {
        onExitSelectionMode()
    }

    when (feedState) {
        Loading -> LoadingState(modifier)
        is Success -> if (feedState.feed.isNotEmpty()) {
            BookmarksGrid(
                feedState = feedState,
                removeFromBookmarks = removeFromBookmarks,
                onNewsResourceViewed = onNewsResourceViewed,
                onTopicClick = onTopicClick,
                isInSelectionMode = isInSelectionMode,
                selectedBookmarkIds = selectedBookmarkIds,
                onEnterSelectionMode = onEnterSelectionMode,
                onExitSelectionMode = onExitSelectionMode,
                onToggleSelect = onToggleSelect,
                onSelectAll = onSelectAll,
                onRemoveSelected = onRemoveSelected,
                onEditNote = onEditNote,
                onDeleteNote = onDeleteNote,
                modifier = modifier,
            )
        } else {
            EmptyState(modifier)
        }
    }

    TrackScreenViewEvent(screenName = "Saved")
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    NiaLoadingWheel(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("forYou:loading"),
        contentDesc = stringResource(id = R.string.feature_bookmarks_api_loading),
    )
}

@Composable
private fun BookmarksGrid(
    feedState: NewsFeedUiState,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    isInSelectionMode: Boolean,
    selectedBookmarkIds: Set<String>,
    onEnterSelectionMode: (String?) -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Set<String>) -> Unit,
    onRemoveSelected: () -> Unit,
    onEditNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollableState = rememberLazyStaggeredGridState()
    TrackScrollJank(scrollableState = scrollableState, stateName = "bookmarks:grid")
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 24.dp,
            state = scrollableState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("bookmarks:feed"),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                if (isInSelectionMode) {
                    BookmarksSelectionBar(
                        selectedCount = selectedBookmarkIds.size,
                        onCancel = onExitSelectionMode,
                        onSelectAll = {
                            val allIds = (feedState as? Success)?.feed?.map { it.id }?.toSet() ?: emptySet()
                            onSelectAll(allIds)
                        },
                        onRemove = onRemoveSelected,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { onEnterSelectionMode(null) },
                            modifier = Modifier.testTag("bookmarks:editButton"),
                        ) {
                            Text(text = stringResource(id = R.string.feature_bookmarks_api_edit))
                        }
                    }
                }
            }
            newsFeed(
                feedState = feedState,
                onNewsResourcesCheckedChanged = { id, _ -> removeFromBookmarks(id) },
                onNewsResourceViewed = onNewsResourceViewed,
                onTopicClick = onTopicClick,
                onEditNote = onEditNote,
                onDeleteNote = onDeleteNote,
                isInSelectionMode = isInSelectionMode,
                selectedIds = selectedBookmarkIds,
                onToggleSelect = onToggleSelect,
                onCardLongClick = onEnterSelectionMode,
            )
            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }
        val itemsAvailable = when (feedState) {
            Loading -> 1
            is Success -> feedState.feed.size + 1
        }
        val scrollbarState = scrollableState.scrollbarState(
            itemsAvailable = itemsAvailable,
        )
        scrollableState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 2.dp)
                .align(Alignment.CenterEnd),
            state = scrollbarState,
            orientation = Orientation.Vertical,
            onThumbMoved = scrollableState.rememberDraggableScroller(
                itemsAvailable = itemsAvailable,
            ),
        )
    }
}

@Composable
fun BookmarksSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .testTag("bookmarks:selectionBar"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.testTag("bookmarks:cancelSelection"),
            ) {
                Icon(
                    imageVector = NiaIcons.Close,
                    contentDescription = stringResource(id = R.string.feature_bookmarks_api_cancel),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.feature_bookmarks_api_selected_counter, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("bookmarks:selectedCounter"),
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.testTag("bookmarks:selectAllButton"),
            ) {
                Text(text = stringResource(id = R.string.feature_bookmarks_api_select_all))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = onRemove,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag("bookmarks:removeSelectedButton"),
            ) {
                Text(text = stringResource(id = R.string.feature_bookmarks_api_remove))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .testTag("bookmarks:empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconTint = LocalTintTheme.current.iconTint
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.feature_bookmarks_api_mg_empty_bookmarks),
            colorFilter = if (iconTint != Color.Unspecified) ColorFilter.tint(iconTint) else null,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_error),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun LoadingStatePreview() {
    NiaTheme {
        LoadingState()
    }
}

@Preview
@Composable
private fun BookmarksGridPreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    NiaTheme {
        BookmarksGrid(
            feedState = Success(userNewsResources),
            removeFromBookmarks = {},
            onNewsResourceViewed = {},
            onTopicClick = {},
            isInSelectionMode = false,
            selectedBookmarkIds = emptySet(),
            onEnterSelectionMode = {},
            onExitSelectionMode = {},
            onToggleSelect = {},
            onSelectAll = {},
            onRemoveSelected = {},
            onEditNote = { _, _ -> },
            onDeleteNote = {},
        )
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    NiaTheme {
        EmptyState()
    }
}
