/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

private const val MAX_NOTE_LENGTH = 280

@Composable
fun BookmarkNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.core_ui_bookmark_note_title))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        if (it.length <= MAX_NOTE_LENGTH) {
                            noteText = it
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.core_ui_bookmark_note_hint)) },
                    supportingText = {
                        Text(
                            text = "${noteText.length}/$MAX_NOTE_LENGTH",
                            modifier = Modifier.testTag("bookmarkNoteDialog:counter"),
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bookmarkNoteDialog:textField"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(noteText.trim().ifEmpty { null }) },
                modifier = Modifier.testTag("bookmarkNoteDialog:save"),
            ) {
                Text(text = stringResource(id = R.string.core_ui_bookmark_note_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("bookmarkNoteDialog:skip"),
                ) {
                    Text(text = stringResource(id = R.string.core_ui_bookmark_note_skip))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bookmarkNoteDialog:cancel"),
                ) {
                    Text(text = stringResource(id = R.string.core_ui_bookmark_note_cancel))
                }
            }
        },
        modifier = modifier.testTag("bookmarkNoteDialog"),
    )
}

@Composable
fun EditBookmarkNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.core_ui_bookmark_note_edit_title))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        if (it.length <= MAX_NOTE_LENGTH) {
                            noteText = it
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.core_ui_bookmark_note_hint)) },
                    supportingText = {
                        Text(
                            text = "${noteText.length}/$MAX_NOTE_LENGTH",
                            modifier = Modifier.testTag("editBookmarkNoteDialog:counter"),
                        )
                    },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editBookmarkNoteDialog:textField"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(noteText.trim()) },
                modifier = Modifier.testTag("editBookmarkNoteDialog:save"),
            ) {
                Text(text = stringResource(id = R.string.core_ui_bookmark_note_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("editBookmarkNoteDialog:delete"),
                ) {
                    Text(text = stringResource(id = R.string.core_ui_bookmark_note_delete))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("editBookmarkNoteDialog:cancel"),
                ) {
                    Text(text = stringResource(id = R.string.core_ui_bookmark_note_cancel))
                }
            }
        },
        modifier = modifier.testTag("editBookmarkNoteDialog"),
    )
}

