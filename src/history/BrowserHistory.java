
package history;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

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

    // nicat to faiq

    public String currentPageInfo() {
        if (current == null) {
            return null;
        }
        return current.getUrl() + " | visited: " + current.getVisitedAt();
    }

    public String getHistoryString() {
        String result = "";
        HistoryNode curr = history.getFirst();

        while (curr != null) {
            if (!result.isEmpty()) {
                result += " | ";
            }
            result += curr.getUrl();
            if (curr == current) {
                result += " (CURRENT)";
            }
            curr = curr.getNext();
        }
        return result;
    }

    public void displayHistory() {
        System.out.println(getHistoryString());
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

        HistoryNode prev = curr.getPrev();    
        history.remove(curr);                 
        curr.setPrev(prev);                     
        deletedHistory.push(curr);             
        return true;
    }

    public boolean restore() {
        if (deletedHistory.isEmpty()) {
            return false;
        }

        HistoryNode node = deletedHistory.pop();
        HistoryNode prev = node.getPrev();       
        node.setPrev(null);

        if (prev == null) {
            history.insertFirst(node);
        } else if (history.contains(prev)) {
            history.insertAfter(prev, node);
        } else {
            return false;
        }

        if (current == null) {
            current = node;    
        }

        if (history.getSize() > MAX_CAPACITY) {
            if (history.getFirst() == current) {
                current = current.getNext();   
            }
            HistoryNode pruned = history.removeFirst();
            if (pruned == node) {
                return false;    // removing right after restoring
            }
        }

        return true;
    }

    public int getSize() {
        return history.getSize();
    }

    public void exportHistory(String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        writer.write(indexOfCurrent() + "\n");

        HistoryNode curr = history.getFirst();
        while (curr != null) {
            writer.write(curr.getUrl() + "\n");
            curr = curr.getNext();
        }

        writer.close();
    }

    public void importHistory(String fileName) throws IOException {
        Scanner reader = new Scanner(new File(fileName));

        String firstLine = null;
        if (reader.hasNextLine()) {
            firstLine = reader.nextLine();
        }

        DoublyLinkedList loaded = new DoublyLinkedList();
        while (reader.hasNextLine()) {
            String line = reader.nextLine().trim();
            if (!line.isEmpty()) {
                loaded.addLast(line);
            }
        }
        reader.close();

        if (firstLine == null) {
            throw new IOException("File is empty.");
        }

        int currentIndex;
        try {
            currentIndex = Integer.parseInt(firstLine.trim());
        } catch (NumberFormatException e) {
            throw new IOException("First line must be the index of the current page.");
        }

        if (loaded.isEmpty() && currentIndex != -1) {
            throw new IOException("File has no pages, so the current page index must be -1.");
        }
        if (!loaded.isEmpty() && (currentIndex < 0 || currentIndex >= loaded.getSize())) {
            throw new IOException("Current page index " + currentIndex + " is out of range.");
        }

        HistoryNode newCurrent = nodeAt(loaded, currentIndex);

        while (loaded.getSize() > MAX_CAPACITY) {
            HistoryNode removed = loaded.removeFirst();
            if (removed == newCurrent) {
                newCurrent = loaded.getFirst();
            }
        }

        history = loaded;
        current = newCurrent;
        deletedHistory.clear();
    }

    private int indexOfCurrent() {
        int index = 0;
        HistoryNode curr = history.getFirst();
        while (curr != null) {
            if (curr == current) {
                return index;
            }
            index++;
            curr = curr.getNext();
        }
        return -1;
    }

    private static HistoryNode nodeAt(DoublyLinkedList list, int index) {
        if (index < 0) {
            return null;
        }
        HistoryNode curr = list.getFirst();
        for (int i = 0; i < index && curr != null; i++) {
            curr = curr.getNext();
        }
        return curr;
    }
}
