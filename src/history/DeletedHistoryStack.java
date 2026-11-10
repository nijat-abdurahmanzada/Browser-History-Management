package history;

import models.HistoryNode;

public class DeletedHistoryStack {
    private HistoryNode top;
    private int size;

    public DeletedHistoryStack() {
        top = null;
        size = 0;
    }

    public void push(String url) {
        HistoryNode newNode = new HistoryNode(url);

        if (top != null) {
            top.setPrev(newNode);
            newNode.setNext(top);
        }

        top = newNode;
        size++;
    }

    public String pop() {
        if (top == null)
            return null;

        HistoryNode node = top;

        if (node.getNext() == null) {
            top = null;
        } else {
            top = node.getNext();
            top.setPrev(null);
        }

        node.setNext(null);
        node.setPrev(null);

        size--;

        return node.getUrl();
    }

    public String peek() {
        if (top == null)
            return null;

        return top.getUrl();
    }

    public boolean isEmpty() {
        if (size != 0)
            return false;

        return true;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        top = null;
        size = 0;
    }
}