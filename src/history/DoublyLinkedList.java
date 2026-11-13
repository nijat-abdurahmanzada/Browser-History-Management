
package history;

import models.HistoryNode;

public class DoublyLinkedList {
    private HistoryNode head = null;
    private HistoryNode tail = null;
    private int size = 0;

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    public HistoryNode getFirst() {
        return head;
    }

    public HistoryNode getLast() {
        return tail;
    }

    public HistoryNode addLast(String url) {
        HistoryNode newNode = new HistoryNode(url);

        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            newNode.setPrev(tail);
            tail = newNode;
        }

        size++;
        return newNode;
    }

    public HistoryNode addFirst(String url) {
        HistoryNode newNode = new HistoryNode(url);

        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            head.setPrev(newNode);
            newNode.setNext(head);
            head = newNode;
        }

        size++;
        return newNode;
    }

    public boolean remove(HistoryNode node) {
        if (node == null) {
            return false;
        }

        HistoryNode prev = node.getPrev();
        HistoryNode next = node.getNext();

        if (prev == null) {
            head = next;
        } else {
            prev.setNext(next);
        }

        if (next == null) {
            tail = prev;
        } else {
            next.setPrev(prev);
        }

        node.setNext(null);
        node.setPrev(null);

        size--;

        return true;
    }

    public HistoryNode removeFirst() {
        if (size == 0) {
            return null;
        }

        HistoryNode node = head;

        if (node.getNext() == null) {
            head = null;
            tail = null;
        } else {
            head = node.getNext();
            head.setPrev(null);
        }

        node.setNext(null);
        node.setPrev(null);

        size--;

        return node;
    }

    public HistoryNode removeLast() {
        if (size == 0) {
            return null;
        }

        HistoryNode node = tail;

        if (node.getPrev() == null) {
            head = null;
            tail = null;
        } else {
            tail = node.getPrev();
            tail.setNext(null);
        }

        node.setPrev(null);
        node.setNext(null);

        size--;

        return node;
    }

    public boolean contains(HistoryNode node) {
        for (HistoryNode curr = head; curr != null; curr = curr.getNext()) {
            if (curr == node) {
                return true;
            }
        }
        return false;
    }

    public void insertAfter(HistoryNode anchor, HistoryNode node) {
        HistoryNode after = anchor.getNext();
        node.setPrev(anchor);
        node.setNext(after);
        anchor.setNext(node);
        if (after == null) {
            tail = node;          
        } else {
            after.setPrev(node);
        }
        size++;
    }

    public void insertFirst(HistoryNode node) {
        node.setPrev(null);
        node.setNext(head);
        if (head == null) {
            tail = node;
        } else {
            head.setPrev(node);
        }
        head = node;
        size++;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }
}
