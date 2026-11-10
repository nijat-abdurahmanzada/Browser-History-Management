
package history;

import models.HistoryNode;

public class BrowserHistory {
    private DoublyLinkedList history;
    private HistoryNode current;
    private DeletedHistoryStack deletedHistory;
    private static final int MAX_CAPACITY = 50;

    public BrowserHistory(String homepage) {
        history = new DoublyLinkedList();
        deletedHistory = new DeletedHistoryStack();
        current = history.addLast(homepage);
    }

    public void visit(String url) {
        HistoryNode last = history.getLast();

        while (current != last) {
            HistoryNode remove = last;
            last = remove.getPrev();
            history.remove(remove);
        }

        current = history.addLast(url);

        if (history.getSize() > MAX_CAPACITY) {
            history.removeFirst();
        }
    }

    public String back(int steps) {
        if (current == null || steps < 0) {
            return null;
        }

        for (int i = 0; i < steps; i++) {
            if (current.getPrev() != null) {
                current = current.getPrev();
            }
        }

        return current.getUrl();
    }

    public String forward(int steps) {
        if (current == null || steps < 0) {
            return null;
        }

        for (int i = 0; i < steps; i++) {
            if (current.getNext() != null) {
                current = current.getNext();
            }
        }

        return current.getUrl();
    }

    public String currentPage() {
        return current == null ? null : current.getUrl();
    }

    public void displayHistory() {
        HistoryNode curr = history.getFirst();

        while (curr != null) {
            if (curr == current) {
                System.out.println(curr.getUrl() + " (CURRENT)");
            } else {
                System.out.println(curr.getUrl());
            }

            curr = curr.getNext();
        }
    }

    public void deleteHistory() {
        history.clear();
        current = null;
        deletedHistory.clear();
    }

    public boolean deleteUrl(String url) {
        HistoryNode curr = history.getFirst();

        while (curr != null && !curr.getUrl().equals(url)) {
            curr = curr.getNext();
        }

        if (curr == null) {
            return false;
        }

        if (curr == current) {
            if (curr.getPrev() != null) {
                current = curr.getPrev();
            } else {
                current = curr.getNext();
            }
        }

        deletedHistory.push(curr.getUrl());
        return history.remove(curr);
    }

    public boolean restore() {
        if (deletedHistory.isEmpty()) {
            return false;
        }

        String url = deletedHistory.pop();
        current = history.addLast(url);

        if (history.getSize() > MAX_CAPACITY) {
            history.removeFirst();
        }

        return true;
    }
}
