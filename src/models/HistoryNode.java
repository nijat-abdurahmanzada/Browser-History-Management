package models;

public class HistoryNode {
    public HistoryNode prev;
    public HistoryNode next;
    public String url;

    public HistoryNode(HistoryNode prev, String url, HistoryNode next) {
        this.prev = prev;
    }
}
