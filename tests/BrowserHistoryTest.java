package tests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import history.BrowserHistory;
import history.DeletedHistoryStack;
import history.DoublyLinkedList;
import models.HistoryNode;

public class BrowserHistoryTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        testDoublyLinkedList();
        testDeletedHistoryStack();
        testNavigation();
        testBranching();
        testCapacity();
        testDelete();
        testRestore();
        testDeleteHistory();
        testExportImport();

        System.out.println("\n===== TEST RESULTS =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total: " + (passed + failed));
    }

    // ===================== HELPERS =====================

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    // Checks a history string, and prints what we actually got if it doesn't match. 
    private static void checkHistory(String name, BrowserHistory browser, String expected) {
        String actual = browser.getHistoryString();
        check(name, actual.equals(expected));
        if (!actual.equals(expected)) {
            System.out.println("       expected: " + expected);
            System.out.println("       actual:   " + actual);
        }
    }

    /**
     * Captures what displayHistory() prints and returns it on one line,
     * e.g. "home | A (CURRENT) | B". This checks the whole list and the cursor at once.
     */
    private static String historyOf(BrowserHistory browser) {
        return browser.getHistoryString();
    }

    // Number of pages in the history. 
    private static int sizeOf(BrowserHistory browser) {
        return browser.getSize();
    }

    // URLs of a list from head to tail, e.g. "A B C". 
    private static String listOf(DoublyLinkedList list) {
        StringBuilder result = new StringBuilder();
        for (HistoryNode curr = list.getFirst(); curr != null; curr = curr.getNext()) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(curr.getUrl());
        }
        return result.toString();
    }

    /**
     * Checks that every prev link matches the next links, that the walk ends at
     * the tail, and that the number of nodes equals getSize().
     */
    private static boolean linksOk(DoublyLinkedList list) {
        int count = 0;
        HistoryNode previous = null;
        for (HistoryNode curr = list.getFirst(); curr != null; curr = curr.getNext()) {
            if (curr.getPrev() != previous) {
                return false;
            }
            previous = curr;
            count++;
        }
        return previous == list.getLast() && count == list.getSize();
    }

    // A history with home, A, B, C, currently on C. 
    private static BrowserHistory fourPages() {
        BrowserHistory browser = new BrowserHistory("home");
        browser.visit("A");
        browser.visit("B");
        browser.visit("C");
        return browser;
    }

    // A full history of 50 pages, p0 ... p49, currently on p49. 
    private static BrowserHistory fullHistory() {
        BrowserHistory browser = new BrowserHistory("p0");
        for (int i = 1; i < 50; i++) {
            browser.visit("p" + i);
        }
        return browser;
    }

    // ===================== DOUBLY LINKED LIST =====================

    private static void testDoublyLinkedList() {
        System.out.println("\n--- DoublyLinkedList Tests ---");
        DoublyLinkedList list = new DoublyLinkedList();

        check("Initially empty", list.isEmpty());

        list.addLast("Google");
        list.addLast("YouTube");
        list.addFirst("Facebook");

        check("Add elements", list.getSize() == 3);
        check("AddFirst", list.getFirst().getUrl().equals("Facebook"));
        check("AddLast", list.getLast().getUrl().equals("YouTube"));
        check("Links consistent after adds", linksOk(list));

        list.removeFirst();
        check("RemoveFirst", list.getFirst().getUrl().equals("Google"));

        list.removeLast();
        check("RemoveLast", list.getLast().getUrl().equals("Google"));

        list.remove(list.getFirst());
        check("Remove specific node", list.isEmpty());

        list.addLast("GitHub");
        list.clear();

        check("Clear", list.isEmpty());
        check("Size after clear", list.getSize() == 0);

        // remove() clears the removed node's own links
        DoublyLinkedList three = new DoublyLinkedList();
        three.addLast("A");
        HistoryNode b = three.addLast("B");
        three.addLast("C");
        three.remove(b);
        check("Remove middle node", listOf(three).equals("A C") && linksOk(three));
        check("Removed node has no links", b.getPrev() == null && b.getNext() == null);

        // contains
        check("Contains node in list", three.contains(three.getFirst()));
        check("Does not contain removed node", !three.contains(b));

        // insertAfter in the middle
        three.insertAfter(three.getFirst(), b);
        check("InsertAfter middle", listOf(three).equals("A B C") && linksOk(three));

        // insertAfter the tail
        HistoryNode d = new HistoryNode("D");
        three.insertAfter(three.getLast(), d);
        check("InsertAfter tail updates tail", three.getLast() == d && linksOk(three));

        // insertFirst
        HistoryNode z = new HistoryNode("Z");
        three.insertFirst(z);
        check("InsertFirst", listOf(three).equals("Z A B C D") && three.getFirst() == z && linksOk(three));

        // insertFirst into an empty list
        DoublyLinkedList empty = new DoublyLinkedList();
        HistoryNode only = new HistoryNode("only");
        empty.insertFirst(only);
        check("InsertFirst into empty list",
                empty.getFirst() == only && empty.getLast() == only && empty.getSize() == 1);
    }

    // ===================== DELETED HISTORY STACK =====================

    private static void testDeletedHistoryStack() {

        System.out.println("\n--- DeletedHistoryStack Tests ---");

        DeletedHistoryStack stack = new DeletedHistoryStack();

        check("Initially empty", stack.isEmpty());

        stack.push(new HistoryNode("Google"));
        stack.push(new HistoryNode("YouTube"));

        check("Push elements", stack.getSize() == 2);
        check("Peek", stack.peek().getUrl().equals("YouTube"));
        check("Pop", stack.pop().getUrl().equals("YouTube"));
        check("LIFO order", stack.pop().getUrl().equals("Google"));
        check("Empty after pop", stack.isEmpty());

        check("Pop empty stack", stack.pop() == null);
        check("Peek empty stack", stack.peek() == null);

        stack.push(new HistoryNode("GitHub"));
        stack.clear();

        check("Clear stack", stack.isEmpty());
        check("Size after clear", stack.getSize() == 0);

        // the stack must not touch prev: it holds the page's old neighbour
        HistoryNode neighbour = new HistoryNode("A");
        HistoryNode deleted = new HistoryNode("B");
        deleted.setPrev(neighbour);
        stack.push(deleted);
        stack.push(new HistoryNode("C"));
        stack.pop();
        HistoryNode popped = stack.pop();
        check("Pop keeps prev (old neighbour)", popped == deleted && popped.getPrev() == neighbour);
        check("Pop clears next (stack link)", popped.getNext() == null);
    }

    // ===================== NAVIGATION =====================

    private static void testNavigation() {

        System.out.println("\n--- Navigation Tests ---");

        BrowserHistory browser = new BrowserHistory("Google");

        check("Homepage", browser.currentPage().equals("Google"));

        browser.visit("YouTube");
        browser.visit("GitHub");

        check("Visit", browser.currentPage().equals("GitHub"));
        check("Back", browser.back(1).equals("YouTube"));
        check("Forward", browser.forward(1).equals("GitHub"));

        // spec edge case: back(100) on a history of 2 pages
        BrowserHistory two = new BrowserHistory("home");
        two.visit("A");
        check("back(100) on 2 pages stops at oldest", two.back(100).equals("home"));
        check("forward(100) stops at newest", two.forward(100).equals("A"));

        BrowserHistory four = fourPages();
        check("back(2)", four.back(2).equals("A"));
        check("forward(1)", four.forward(1).equals("B"));
        check("back(0) does not move", four.back(0).equals("B"));
        check("forward(0) does not move", four.forward(0).equals("B"));
        check("Negative steps return null", four.back(-1) == null && four.forward(-3) == null);
        check("Negative steps do not move", four.currentPage().equals("B"));
    }

    // ===================== BRANCHING =====================

    private static void testBranching() {

        System.out.println("\n--- Branching Tests ---");

        // spec edge case: visiting a new page from the middle clears forward history
        BrowserHistory browser = fourPages();
        browser.back(2);                                 // at A
        browser.visit("X");
        checkHistory("Visit from middle removes forward pages", browser, "home | A | X (CURRENT)");
        check("No forward history after branching", browser.forward(1).equals("X"));
        check("Back still works after branching", browser.back(1).equals("A"));

        // branching from the oldest page
        BrowserHistory fromStart = fourPages();
        fromStart.back(100);                             // at home
        fromStart.visit("Y");
        checkHistory("Visit from oldest page", fromStart, "home | Y (CURRENT)");

        // visiting from the newest page removes nothing
        BrowserHistory fromEnd = fourPages();
        fromEnd.visit("D");
        checkHistory("Visit from newest page keeps everything", fromEnd, "home | A | B | C | D (CURRENT)");
    }

    // ===================== CAPACITY =====================

    private static void testCapacity() {

        System.out.println("\n--- Capacity Tests ---");

        BrowserHistory browser = fullHistory();
        check("50 pages fit", sizeOf(browser) == 50);

        browser.visit("p50");                            // 51st page
        check("Never more than 50 pages", sizeOf(browser) == 50);
        check("Oldest page pruned", historyOf(browser).startsWith("p1 | "));
        check("Newest page is current", browser.currentPage().equals("p50"));

        for (int i = 51; i < 60; i++) {
            browser.visit("p" + i);
        }
        check("Still 50 pages after many visits", sizeOf(browser) == 50);
        check("Oldest is p10 after 60 visits", historyOf(browser).startsWith("p10 | "));
        check("back(100) stops at oldest kept page", browser.back(100).equals("p10"));
    }

    // ===================== DELETE =====================

    private static void testDelete() {

        System.out.println("\n--- Delete Tests ---");

        BrowserHistory browser = fourPages();
        check("Delete existing URL", browser.deleteUrl("B"));
        checkHistory("Deleted page is gone", browser, "home | A | C (CURRENT)");
        check("Back skips deleted page", browser.back(1).equals("A"));

        check("Delete non-existing URL", !browser.deleteUrl("Twitter"));

        // deleting the current page moves the cursor back one page
        BrowserHistory current = fourPages();
        current.deleteUrl("C");
        checkHistory("Deleting current moves cursor to previous", current, "home | A | B (CURRENT)");

        // deleting the current page when it is the oldest moves the cursor forward
        BrowserHistory oldest = fourPages();
        oldest.back(100);                                // at home
        oldest.deleteUrl("home");
        checkHistory("Deleting current oldest moves cursor to next", oldest, "A (CURRENT) | B | C");

        // deleting the only page leaves an empty history
        BrowserHistory single = new BrowserHistory("home");
        single.deleteUrl("home");
        check("Deleting only page", single.currentPage() == null && sizeOf(single) == 0);

        // duplicate URLs: the first (oldest) occurrence is deleted
        BrowserHistory duplicates = new BrowserHistory("home");
        duplicates.visit("A");
        duplicates.visit("B");
        duplicates.visit("A");
        duplicates.deleteUrl("A");
        checkHistory("Duplicate URL deletes oldest occurrence", duplicates, "home | B | A (CURRENT)");
    }

    // ===================== RESTORE =====================

    private static void testRestore() {

        System.out.println("\n--- Restore Tests ---");

        // restore puts the page back in its old place and keeps the cursor
        BrowserHistory browser = fourPages();
        browser.back(2);                                 // at A
        browser.deleteUrl("B");
        check("Restore returns true", browser.restore());
        checkHistory("Restore puts B back after A, cursor stays", browser, "home | A (CURRENT) | B | C");
        check("Forward reaches restored page", browser.forward(1).equals("B"));
        check("Back from restored page", browser.back(1).equals("A"));

        // restoring with an empty stack
        BrowserHistory browser2 = fourPages();
        check("Restore with empty stack", !browser2.restore());

        // delete B then its neighbour A: A comes back first, then B after it
        BrowserHistory neighbours = fourPages();
        neighbours.deleteUrl("B");
        neighbours.deleteUrl("A");
        neighbours.restore();
        checkHistory("First restore brings back A", neighbours, "home | A | C (CURRENT)");
        neighbours.restore();
        checkHistory("Second restore brings back B after A", neighbours, "home | A | B | C (CURRENT)");

        // delete A then B (B's prev is home at that point)
        BrowserHistory reverse = fourPages();
        reverse.deleteUrl("A");
        reverse.deleteUrl("B");
        reverse.restore();
        reverse.restore();
        checkHistory("Delete A, B and restore both", reverse, "home | A | B | C (CURRENT)");

        // restoring the oldest page puts it back at the start
        BrowserHistory head = fourPages();
        head.deleteUrl("home");
        head.restore();
        checkHistory("Restore oldest page as head", head, "home | A | B | C (CURRENT)");

        // restoring a deleted current page does not move the cursor
        BrowserHistory current = fourPages();
        current.deleteUrl("C");                          // cursor moves to B
        current.restore();
        checkHistory("Restore deleted current page, cursor stays", current, "home | A | B (CURRENT) | C");

        // restoring into an empty history makes the page current
        BrowserHistory single = new BrowserHistory("home");
        single.deleteUrl("home");
        check("Restore into empty history", single.restore() && "home".equals(single.currentPage()));

        // prev was cut off by visit(): the page cannot go back
        BrowserHistory branched = fourPages();
        branched.deleteUrl("B");                         // B's prev is A
        branched.back(100);                              // at home
        branched.visit("X");                             // A and C are cut off
        check("Restore fails when prev was cut off", !branched.restore());
        checkHistory("History unchanged after failed restore", branched, "home | X (CURRENT)");
        check("Failed restore still removes it from the stack", !branched.restore());

        // full history: restore adds the page and prunes the oldest
        BrowserHistory full = fullHistory();
        full.deleteUrl("p10");                           // 49 pages
        full.visit("X");                                 // 50 pages again
        check("Restore into full history", full.restore());
        check("Still 50 pages after restore", sizeOf(full) == 50);
        check("Oldest page pruned after restore", historyOf(full).startsWith("p1 | "));
        check("Restored page back in its place", historyOf(full).contains("p9 | p10 | p11"));
        check("Cursor unchanged after restore", full.currentPage().equals("X"));

        // full history, cursor on the oldest page: cursor moves before pruning
        BrowserHistory cursorOldest = fullHistory();
        cursorOldest.deleteUrl("p20");
        cursorOldest.visit("X");
        cursorOldest.back(100);                          // at p0
        cursorOldest.restore();
        check("Cursor moves off a pruned page", cursorOldest.currentPage().equals("p1"));
        check("Pruned page is gone", historyOf(cursorOldest).startsWith("p1 (CURRENT) | "));

        // full history, restored page was the oldest: it is pruned right away
        BrowserHistory prunedRight = fullHistory();
        prunedRight.deleteUrl("p0");                     // p0 had no prev
        prunedRight.visit("X");
        check("Restore fails when page is pruned right away", !prunedRight.restore());
        check("Size still 50", sizeOf(prunedRight) == 50);
        check("p0 not in history", historyOf(prunedRight).startsWith("p1 | "));
    }

    // ===================== DELETE ALL HISTORY =====================

    private static void testDeleteHistory() {

        System.out.println("\n--- Delete History Tests ---");

        BrowserHistory browser = fourPages();
        browser.deleteUrl("B");
        browser.deleteHistory();

        check("Delete all history", browser.currentPage() == null);
        check("History is empty", sizeOf(browser) == 0);
        check("Back after delete", browser.back(1) == null);
        check("Forward after delete", browser.forward(1) == null);
        check("Deleted pages cannot be restored", !browser.restore());

        browser.visit("new");
        checkHistory("Visit after deleting all history", browser, "new (CURRENT)");
    }

    // ===================== EXPORT / IMPORT =====================

    // Writes text to a file and returns the file name.
    private static String fileWith(String fileName, String content) {
        try {
            FileWriter writer = new FileWriter(fileName);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not write " + fileName, e);
        }
        return fileName;
    }

    // Reads a file and returns its lines joined with " | ", e.g. "0 | home | A".
    private static String readFile(String fileName) throws IOException {
        Scanner reader = new Scanner(new File(fileName));
        String result = "";
        while (reader.hasNextLine()) {
            if (!result.isEmpty()) {
                result += " | ";
            }
            result += reader.nextLine();
        }
        reader.close();
        return result;
    }

    // True if importing this file throws an IOException.
    private static boolean importFails(BrowserHistory browser, String fileName) {
        try {
            browser.importHistory(fileName);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private static void testExportImport() {

        System.out.println("\n--- Export / Import Tests ---");

        try {
            // round trip: export, import into another browser, same history and cursor
            BrowserHistory original = fourPages();
            original.back(1);                            // at B
            original.exportHistory("test_export.txt");

            BrowserHistory copy = new BrowserHistory("other");
            copy.importHistory("test_export.txt");
            checkHistory("Import restores pages and cursor", copy, "home | A | B (CURRENT) | C");
            check("Back works after import", copy.back(1).equals("A"));
            check("Forward works after import", copy.forward(2).equals("C"));

            // visiting after import still branches correctly
            copy.back(1);                                // at B
            copy.visit("X");
            checkHistory("Visit after import", copy, "home | A | B | X (CURRENT)");

            // export writes the index of the current page, then one URL per line
            BrowserHistory small = new BrowserHistory("home");
            small.visit("A");
            small.back(1);                               // at home
            small.exportHistory("test_format.txt");
            check("File format", readFile("test_format.txt").equals("0 | home | A"));

            // import clears the deleted-pages stack
            BrowserHistory withDeleted = fourPages();
            withDeleted.deleteUrl("B");
            withDeleted.importHistory("test_export.txt");
            check("Deleted pages cannot be restored after import", !withDeleted.restore());

            // empty history round trip
            BrowserHistory empty = new BrowserHistory("home");
            empty.deleteHistory();
            empty.exportHistory("test_empty.txt");
            BrowserHistory fromEmpty = fourPages();
            fromEmpty.importHistory("test_empty.txt");
            check("Import empty history", fromEmpty.currentPage() == null && sizeOf(fromEmpty) == 0);

            // blank lines in the file are skipped
            BrowserHistory blanks = new BrowserHistory("x");
            blanks.importHistory(fileWith("test_blanks.txt", "1\nhome\n\nA\n\n"));
            checkHistory("Blank lines are skipped", blanks, "home | A (CURRENT)");

            // more than 50 pages in the file: keep the newest 50
            String many = "59\n";
            for (int i = 0; i < 60; i++) {
                many += "p" + i + "\n";
            }
            BrowserHistory big = new BrowserHistory("x");
            big.importHistory(fileWith("test_many.txt", many));
            check("Import keeps only 50 pages", sizeOf(big) == 50);
            check("Import keeps the newest pages", historyOf(big).startsWith("p10 | "));
            check("Cursor kept when its page is kept", big.currentPage().equals("p59"));

            String manyAtStart = "0" + many.substring(2);   // same pages, cursor on p0
            BrowserHistory bigStart = new BrowserHistory("x");
            bigStart.importHistory(fileWith("test_many_start.txt", manyAtStart));
            check("Cursor moves to oldest kept page when its page is dropped",
                    bigStart.currentPage().equals("p10"));
        } catch (IOException e) {
            check("Export/import threw " + e.getMessage(), false);
        }

        // invalid files: an IOException is thrown and the history is unchanged
        BrowserHistory browser = fourPages();
        check("Missing file fails", importFails(browser, "no_such_file_123.txt"));
        check("Empty file fails", importFails(browser, fileWith("test_bad_empty.txt", "")));
        check("Non-number first line fails", importFails(browser, fileWith("test_bad_number.txt", "abc\nhome\n")));
        check("Index out of range fails", importFails(browser, fileWith("test_bad_range.txt", "5\nhome\nA\n")));
        check("Negative index with pages fails", importFails(browser, fileWith("test_bad_negative.txt", "-1\nhome\n")));
        check("Index with no pages fails", importFails(browser, fileWith("test_bad_nopages.txt", "0\n")));
        checkHistory("History unchanged after failed imports", browser, "home | A | B | C (CURRENT)");
    }
}