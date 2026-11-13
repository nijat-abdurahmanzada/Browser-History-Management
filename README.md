# Browser History Management

A Java desktop app that works like a web browser's history. You can visit pages, go back and forward, delete pages and restore them, and save the history to a file and load it again. The data structures are written by hand: a **doubly linked list** holds the history and a **stack** holds the deleted pages. No `java.util` collections are used.

A Swing window lets you use the history directly, and it can also download and show the real web page for each URL.

## Features

- **Visit** a URL. If you visit a page after going back, the pages ahead of you are removed, just like in a real browser.
- **Back / Forward** by any number of steps. Navigation stops at the oldest or newest page and never goes past either end.
- **Capacity limit.** The history holds at most **50 pages**. When it is full, the oldest page is removed.
- **Delete a URL.** The oldest page with that URL is removed. If it was the current page, the cursor moves to the previous page, or to the next page if there is no previous one.
- **Restore.** The most recently deleted page goes back where it was, right after its old neighbour. The cursor does not move.
- **Clear all.** Deletes the whole history and the deleted-pages stack.
- **Export / Import** the history as a plain text file. The current page is saved too.
- **Page loading (optional).** The UI can download the current URL and show its HTTP status, title, rendered page and HTML source. Clicking a link in the rendered page adds that page to the history.

## Project structure

```
src/
├── models/
│   ├── HistoryNode.java          # A node: URL, visit time, prev/next links
│   ├── HttpRequestData.java      # URL, method, headers, body
│   └── HttpResponseData.java     # Status code, headers, body
├── history/
│   ├── DoublyLinkedList.java     # The history list (head, tail, size)
│   ├── DeletedHistoryStack.java  # LIFO stack of deleted pages
│   └── BrowserHistory.java       # Navigation, delete/restore, export/import
├── network/
│   └── HttpClientService.java    # Sends GET/POST/PUT/DELETE with java.net.http
└── ui/
    └── BrowserUI.java            # Swing window (main entry point)
tests/
└── BrowserHistoryTest.java       # Test suite (run as a normal main class)
```

`BrowserHistory` holds all the history logic. `BrowserUI` only displays the state and calls `BrowserHistory`'s methods.

## Requirements

- **JDK 11 or newer** (`java.net.http.HttpClient` needs Java 11).
- No external libraries and no build tool are needed.

## Build and run

Run these from the project root. They work in PowerShell, Command Prompt and Bash.

```sh
# Compile everything into out/
javac -d out src/models/*.java src/history/*.java src/network/*.java src/ui/*.java tests/*.java

# Start the app
java -cp out ui.BrowserUI
```

You can also open the folder in IntelliJ IDEA and run `ui.BrowserUI`.

### Using the app

| Control | What it does |
|---|---|
| Address bar + **Go** (or Enter) | Visit the URL |
| **< Back** / **Forward >** + **Steps** | Move that many pages (1–50) |
| History list, double-click or **Open selected** | Jump to that page |
| **Delete selected** | Delete that URL (the oldest page with it) |
| **Restore** | Bring back the most recently deleted page |
| **Clear all** | Delete everything (this cannot be restored) |
| **Export... / Import...** | Save or load the history as a `.txt` file |
| **Load pages from the internet** | Download and show the current page |

The app starts on `https://www.google.com`. In the history list the current page is shown in bold and marked with `[<]`. Page downloads run in the background, so the window does not freeze. Swing cannot run JavaScript and only understands simple CSS, so pages are shown in a basic form.

## Running the tests

```sh
javac -d out src/models/*.java src/history/*.java src/network/*.java tests/*.java
java -cp out tests.BrowserHistoryTest
```

The suite prints `[PASS]` or `[FAIL]` for each check and a total at the end. It currently has **111 checks**, all passing. It covers:

- `DoublyLinkedList` and `DeletedHistoryStack` operations, including link consistency
- Navigation edge cases (`back(100)` on a short history, zero and negative steps)
- Branching (visiting from the middle, the oldest and the newest page)
- The 50-page capacity limit
- Deleting the current, the oldest, the only and a duplicate URL
- Restoring, including when the old neighbour is gone or the history is full
- Export/import round trips and invalid files

> **Note:** the export/import tests write `test_*.txt` files into the directory you run them from.

## History file format

The first line is the **index of the current page** (0-based, or `-1` for an empty history). Each line after it is one URL, oldest first:

```
1
https://www.google.com
https://github.com
https://www.youtube.com
```

In this file the current page is `https://github.com`. When importing:

- Blank lines are skipped.
- If the file has more than 50 pages, only the newest 50 are kept. If the current page was dropped, the cursor moves to the oldest kept page.
- The import fails with an `IOException` if the file is missing or empty, the first line is not a number, or the index is out of range. The current history is left unchanged.
- The deleted-pages stack is cleared, so pages deleted before the import cannot be restored.

## How it works

The history is a doubly linked list of `HistoryNode`s, and `current` points to the page being viewed. Back and forward follow the `prev` and `next` links.

When a page is deleted, its node is unlinked from the list and pushed onto `DeletedHistoryStack`. The stack links nodes through their `next` field, and the node's `prev` field still points to its old neighbour. `restore()` pops the node and puts it back after that neighbour. If the neighbour is no longer in the history (for example, a later visit removed it), the restore fails and returns `false`.

| Operation | Time |
|---|---|
| `visit` | O(k), where k is the number of forward pages removed |
| `back(n)` / `forward(n)` | O(n) |
| `deleteUrl` | O(size) |
| `restore` | O(size) (checks that the old neighbour is still in the list) |
| `exportHistory` / `importHistory` | O(size) |
