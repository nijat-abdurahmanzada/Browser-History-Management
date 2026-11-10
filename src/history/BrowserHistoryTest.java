
package history;

public class BrowserHistoryTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        testDoublyLinkedList();
        testDeletedHistoryStack();
        testBrowserHistory();

        System.out.println("\n===== TEST RESULTS =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total: " + (passed + failed));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    // DOUBLY LINKED LIST
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
    }

    // DELETED HISTORY STACK
    private static void testDeletedHistoryStack() {

        System.out.println("\n--- DeletedHistoryStack Tests ---");

        DeletedHistoryStack stack = new DeletedHistoryStack();

        check("Initially empty", stack.isEmpty());

        stack.push("Google");
        stack.push("YouTube");

        check("Push elements", stack.getSize() == 2);
        check("Peek", stack.peek().equals("YouTube"));
        check("Pop", stack.pop().equals("YouTube"));
        check("LIFO order", stack.pop().equals("Google"));
        check("Empty after pop", stack.isEmpty());

        check("Pop empty stack", stack.pop() == null);

        stack.push("GitHub");
        stack.clear();

        check("Clear stack", stack.isEmpty());
        check("Size after clear", stack.getSize() == 0);
    }

    // BROWSER HISTORY
    private static void testBrowserHistory() {

        System.out.println("\n--- BrowserHistory Tests ---");

        BrowserHistory browser = new BrowserHistory("Google");

        check("Homepage", browser.currentPage().equals("Google"));

        browser.visit("YouTube");
        browser.visit("GitHub");

        check("Visit", browser.currentPage().equals("GitHub"));

        check("Back", browser.back(1).equals("YouTube"));
        check("Forward", browser.forward(1).equals("GitHub"));

        browser.back(1);
        browser.visit("Facebook");

        check("Visit clears forward history",
                browser.currentPage().equals("Facebook"));

        check("Delete existing URL", browser.deleteUrl("YouTube"));

        check("Restore deleted URL", browser.restore());

        check("Delete non-existing URL",
                !browser.deleteUrl("Twitter"));

        browser.deleteHistory();

        check("Delete all history", browser.currentPage() == null);

        check("Back after delete", browser.back(1) == null);
        check("Forward after delete", browser.forward(1) == null);
    }
}
