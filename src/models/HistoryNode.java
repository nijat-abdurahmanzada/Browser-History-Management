package models;

public class HistoryNode {
    private HistoryNode prev = null;
    private HistoryNode next = null;
    private String url;

    public HistoryNode(HistoryNode prev, String url, HistoryNode next) {
        this.prev = prev;
        this.next = next;
        this.url = url;
    }

    public HistoryNode(String url) {
        this.url = url;
    }

    public HistoryNode getPrev() {
        return prev;
    }

    public void setPrev(HistoryNode prev) {
        this.prev = prev;
    }

    public HistoryNode getNext() {
        return next;
    }

    public void setNext(HistoryNode next) {
        this.next = next;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
