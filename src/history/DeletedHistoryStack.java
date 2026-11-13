package history;

import models.HistoryNode;

public class DeletedHistoryStack {
    private HistoryNode top;
    private int size;

    public DeletedHistoryStack() {
        top = null;
        size = 0;
    }

    public void push(HistoryNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot push a null page.");
        }
        node.setNext(top);
        top = node;
        size++;
    }

    public HistoryNode pop() {
        if (top == null) {
            return null;
        }
        HistoryNode node = top;
        top = node.getNext();       
        node.setNext(null);
        size--;
        return node;
    }

    public HistoryNode peek() {
        return top;
    }

    public boolean isEmpty() {
        return size==0;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        top = null;
        size = 0;
    }
}