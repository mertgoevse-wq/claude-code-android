# Attachments

Getting a file, an image, or a screen into the conversation.

## What can be attached

| Type | How | Limit | Notes |
|---|---|---|---|
| A project file | The `@` button, with completion over the project's file list | None | The reference is a path, not a copy. The agent reads the live file. |
| A device file | The attach button, via the system picker | None | Copied into the app's storage for the duration of the run |
| An image | The attach button, or the camera | 10 MB | Passed to the model as an image. Vision is supported on current models. |
| A screenshot | Share into the app from the gallery picker | 10 MB | Same as an image |
| A command output | A selection in the terminal, "In Chat senden" | 64 KB | Text, wrapped in a fenced block |
| A log file from a run | From the run's log, "An Chat senden" | 64 KB | Text, truncated with a marker |
| A URL | Typed in the message | None | The agent fetches it, subject to the network permission |
| A folder | Not supported | — | The project is the folder. |

## Project file references

The `@` button, which is the most-used one.

| Aspect | Behaviour |
|---|---|
| Trigger | The `@` key in the composer, or the button |
| Completion | Searches the project's file list as you type. Ranked: exact prefix, then basename, then substring |
| Grouping | By directory, with a breadcrumb for the current scope |
| Scope | Narrowing with `/` — `@src/main/` lists only that subtree |
| Recent | The last ten references, offered when the field is empty |
| Insertion | The path relative to the project root, in a `@path` reference the engine understands |
| Path with spaces | Quoted correctly, and the insertion is visible before sending |
| A path that does not exist | Shown in the completion as unavailable and not selectable. The message can still be sent; the reference simply will not resolve, and the agent reports it. |
| A file over 1 MB | Shown with its size. Still selectable. Some engines truncate; the app says "große Datei" rather than pretending. |
| A binary file | Shown with the binary marker |
| A `.gitignore`d file | Not offered. The app respects the ignore rules, because a reference to an ignored file is a reference to a file the project does not have. |

**A project reference is not a copy.** If the file changes mid-run, the agent sees the new content. This is correct: the agent is working on the project, not on a snapshot of it.

## Device files and images

| Aspect | Behaviour |
|---|---|
| Picking | The system picker via `ACTION_OPEN_DOCUMENT`, so no broad storage permission is needed |
| Copying | Copied into the app's own storage at `filesDir/cca/attachments/{conversation}/{id}`, because a content URI can be revoked mid-run |
| Lifetime | Deleted when the conversation is erased, or by the attachment-cleanup maintenance task, or when the user clears them. Never deleted while a run might still need them. |
| Display | A thumbnail for images, a file row for everything else: name, size, type |
| The model sees | Images as images. Text files by their path, with the content read on demand. |
| Naming | The original name, sanitised. A duplicate name gets a suffix rather than overwriting. |
| Sending | A progress state in the composer while a large file is copied, with a cancel |
| Failure | "Die Datei konnte nicht gelesen werden." with the reason, and the message can still be sent without it |

### Image handling

| Property | Behaviour |
|---|---|
| Formats | JPEG, PNG, WebP, HEIC. HEIC is converted to JPEG if the model does not accept it, and the conversion is noted. |
| Size | Downscaled so the long edge is at most 1568 px, the practical maximum for a vision model. The original is kept on disk. |
| Quality | JPEG at 85 %, or PNG for images with transparency |
| Metadata | Stripped. Location data in a photo has no business being sent to a model. |
| EXIF orientation | Applied, so a rotated photo is not sent sideways |
| Animated images | The first frame, with a note |
| Very large images | Downscaled with a visible note, never silently |

**Metadata stripping is a privacy decision and it is stated in the composer** when an image with location data is attached: "Standortdaten wurden entfernt." A user who wants to send a location deliberately can say so in the message text.

## Command output from the terminal

| Aspect | Behaviour |
|---|---|
| Trigger | Select lines in the terminal, then "In Chat senden" |
| Insertion | A fenced code block with the text, plus a leading line: "Aus dem Terminal, 14:32, Sitzung bash" |
| Limit | 64 KB, with a visible marker when truncated and the full text saved to a file whose path is referenced |
| Escaping | The output is wrapped in a fence long enough to contain any backticks it has, so a log containing ``` does not break the message |
| Before sending | The composer shows the block so it can be reviewed, because terminal output can be enormous |

## Files as context for long runs

An attachment's path is passed as an `@` reference, so the agent reads it on demand rather than having it in the context from the start. This matters on a phone: a 200 KB log in the context from the beginning is a large fraction of the window spent before the task starts.

For a file the agent will definitely need, the app offers "Immer laden", which puts a summarised version in the context from the start, and says what that will cost in tokens.

## What is never attached

| Never | Reason |
|---|---|
| A file outside the user's reach | The picker is the only source; no path traversal |
| A key or a credential file | `Redactor` runs on attachment content, and files whose names match known key patterns are refused outright with a message |
| A device contact, message, or location | No permission is ever requested for these, so they cannot be attached |
| A file larger than 50 MB without a warning | A confirm, naming the size, offering to attach only the first part or to write it to a project file instead |
| The device's entire storage | There is no such path |

## The composer's attachment display

```
┌───────────────────────────────────────────┐
│ Nachricht an Claude …                     │
├───────────────────────────────────────────┤
│ 📎 build-log.txt · 48 KB        ✕         │
│ 🖼 screenshot.png · 320 KB      ✕         │
│ @src/main/kotlin/App.kt                   │
├───────────────────────────────────────────┤
│ 📎 @ ⊕Modell ⊕Rechte            ~0,04 $ → │
└───────────────────────────────────────────┘
```

| Property | Behaviour |
|---|---|
| The strip | Above the composer, scrolling horizontally when there are more than three |
| Each item | An icon, a name, a size. Truncation from the left, so the extension stays visible. |
| Removal | An `✕` on the item, 48 dp |
| A large file being copied | A progress line on the item, with a cancel |
| During a run | Attachments can be added and they are delivered with the next turn, like a message |
| A failed copy | The item shows the reason in `danger` and a retry, and is not sent |

## Storage

| Aspect | Behaviour |
|---|---|
| Where | `filesDir/cca/attachments/`, app-private, not on shared storage |
| Size | Shown in Settings, under Speicher |
| Cleanup | A maintenance task removes attachments older than 30 days whose conversations are archived, and never removes one a run might need. The count is shown before it runs. |
| The user | Can clear all attachments from Settings, with a confirm, and it is a removal of the app's own cache rather than of the user's files. The originals are untouched — they are still wherever they came from. |
| Never | The app does not delete the user's original file. The copy is the app's; the original belongs to the user. |

## Accessibility

| Requirement | Implementation |
|---|---|
| The attach button | "Datei anhängen" |
| The `@` button | "Datei im Projekt erwähnen" |
| An attachment item | "{name}, {type}, {size}. Entfernen." |
| The completion list | A list; each item announces the full path and whether it exists |
| A file being copied | A live region, on state change only |
| An image | Its alt text, if the user typed one. Without one: "Bild, {width} mal {height}". The image is not described to a screen reader, because the app cannot know what is in it. |
| Metadata stripped | Announced, so the user knows the location did not travel |
| A refused file | The reason, announced, with the pattern that matched |
| Font scale 1.3 | The strip wraps to two rows |
| Target | The `✕` is 48 dp even though it looks like 24 |

## Testing

| Test | Type |
|---|---|
| `ProjectReferenceCompletion` | Unit — ranking, `/` scoping, recent entries, missing files, binaries, large files |
| `ProjectReferenceRespectsIgnore` | E2E — a `.gitignore`d file is not offered |
| `ProjectReferenceLive` | E2E — a file changed mid-run is read at its new content |
| `DeviceFileCopy` | E2E — a file is copied, the run can read it after the content URI is revoked |
| `ImageProcessing` | Unit — resize, format conversion, EXIF rotation, metadata stripping, first frame for animated |
| `ImageMetadataStripped` | E2E — a photo with GPS data is attached, the GPS is gone, and the note appeared |
| `LargeFileConfirm` | UI — a 60 MB file produces a confirm naming the size and the two alternatives |
| `CredentialFileRefused` | E2E — a file named `.env` with a key-shaped line is refused with the reason |
| `TerminalSelectionToChat` | E2E — a 200-line selection inserts a correctly fenced block |
| `FenceEscaping` | Unit — output containing ``` does not break the message |
| `TerminalOutputTruncation` | E2E — 100 KB of output is truncated with a visible marker and a file reference |
| `AttachmentStrip` | Screenshot with four attachments, at 1.0 and 1.3 font scale |
| `AttachmentCleanup` | E2E — the maintenance task removes old attachments, keeps ones a run needs, and states the count first |
| `AttachmentOriginalUntouched` | E2E — clearing the app's attachments leaves the user's original file in place |
