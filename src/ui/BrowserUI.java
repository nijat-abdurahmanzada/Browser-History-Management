package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import history.BrowserHistory;
import models.HttpRequestData;
import models.HttpResponseData;
import network.HttpClientService;

/**
 * Swing window for the browser history manager.
 *
 * All history logic lives in BrowserHistory. This class only shows the
 * history and passes the user's clicks to BrowserHistory's methods.
 */
public class BrowserUI extends JFrame {

    private static final String HOMEPAGE = "https://www.google.com";

    private final BrowserHistory browser = new BrowserHistory(HOMEPAGE);
    private final HttpClientService http = new HttpClientService();

    // top bar
    private final JButton backButton = new JButton("< Back");
    private final JButton forwardButton = new JButton("Forward >");
    private final JSpinner stepsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final JTextField addressField = new JTextField();
    private final JButton goButton = new JButton("Go");

    // current page area
    private final JLabel urlLabel = new JLabel();
    private final JLabel visitedLabel = new JLabel(" ");
    private final JLabel loadLabel = new JLabel(" ");
    private final JLabel titleLabel = new JLabel(" ");
    private final JEditorPane pageView = new JEditorPane();     // rendered page
    private final JTextArea bodyArea = new JTextArea();         // HTML source
    private final JCheckBox loadPagesBox = new JCheckBox("Load pages from the internet (status, title and body)");

    private static final int MAX_BODY_LENGTH = 100000;   // longer pages are cut so the text area stays fast

    // history list
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);
    private int currentIndex = -1;          // position of the current page in the list, -1 if empty
    private String shownUrl = null;         // URL the page area currently shows

    private final JLabel statusLabel = new JLabel(" ");

    public BrowserUI() {
        setTitle("Browser History Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 560);
        setLocationRelativeTo(null);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildPageArea(), BorderLayout.CENTER);
        add(buildHistoryPanel(), BorderLayout.EAST);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        refresh();
    }

    // ===================== LAYOUT =====================

    private JPanel buildTopBar() {
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        navigation.add(backButton);
        navigation.add(forwardButton);
        navigation.add(new JLabel("Steps:"));
        navigation.add(stepsSpinner);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(navigation, BorderLayout.WEST);
        top.add(addressField, BorderLayout.CENTER);
        top.add(goButton, BorderLayout.EAST);

        backButton.addActionListener(e -> goBack());
        forwardButton.addActionListener(e -> goForward());
        goButton.addActionListener(e -> visitAddress());
        addressField.addActionListener(e -> visitAddress());    // Enter key

        return top;
    }

    private JPanel buildPageArea() {
        // top: heading, URL, visit time, load result (stacked)
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Current page");
        heading.setFont(heading.getFont().deriveFont(Font.PLAIN, 13f));
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.BOLD, 22f));

        info.add(heading);
        info.add(Box.createVerticalStrut(8));
        info.add(urlLabel);
        info.add(Box.createVerticalStrut(8));
        info.add(visitedLabel);
        info.add(Box.createVerticalStrut(8));
        info.add(loadLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(titleLabel);
        info.add(Box.createVerticalStrut(8));

        // middle: two tabs, the rendered page and its HTML source
        pageView.setEditable(false);
        pageView.setContentType("text/html");
        pageView.addHyperlinkListener(e -> {
            // clicking a link visits it, so it is added to the history like in a real browser
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                browser.visit(e.getURL().toString());
                status("Followed link to " + e.getURL());
                refresh();
            }
        });

        bodyArea.setEditable(false);
        bodyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Page", new JScrollPane(pageView));
        tabs.addTab("HTML source", new JScrollPane(bodyArea));

        JPanel page = new JPanel(new BorderLayout(5, 5));
        page.setBorder(BorderFactory.createEmptyBorder(15, 15, 8, 15));
        page.add(info, BorderLayout.NORTH);
        page.add(tabs, BorderLayout.CENTER);
        page.add(loadPagesBox, BorderLayout.SOUTH);

        loadPagesBox.addActionListener(e -> {
            if (loadPagesBox.isSelected() && browser.currentPage() != null) {
                loadPage(browser.currentPage());
            } else {
                clearPage();
            }
        });

        return page;
    }

    private JPanel buildHistoryPanel() {
        historyList.setCellRenderer(new HistoryRenderer());
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();             // double-click jumps to that page
                }
            }
        });

        JButton openButton = new JButton("Open selected");
        JButton deleteButton = new JButton("Delete selected");
        JButton restoreButton = new JButton("Restore");
        JButton clearButton = new JButton("Clear all");
        JButton exportButton = new JButton("Export...");
        JButton importButton = new JButton("Import...");

        openButton.addActionListener(e -> openSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        restoreButton.addActionListener(e -> restore());
        clearButton.addActionListener(e -> clearHistory());
        exportButton.addActionListener(e -> exportHistory());
        importButton.addActionListener(e -> importHistory());

        JPanel buttons = new JPanel(new GridLayout(0, 2, 5, 5));
        buttons.add(openButton);
        buttons.add(deleteButton);
        buttons.add(restoreButton);
        buttons.add(clearButton);
        buttons.add(exportButton);
        buttons.add(importButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));
        panel.setPreferredSize(new Dimension(340, 0));
        panel.add(new JLabel("History (oldest first, [<] = current page)"), BorderLayout.NORTH);
        panel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ===================== ACTIONS =====================

    private void visitAddress() {
        String url = addressField.getText().trim();
        if (url.isEmpty()) {
            status("Type a URL first.");
            return;
        }
        browser.visit(url);
        status("Visited " + url);
        refresh();
    }

    private void goBack() {
        int steps = (Integer) stepsSpinner.getValue();
        browser.back(steps);
        status("Went back " + steps + " step(s).");
        refresh();
    }

    private void goForward() {
        int steps = (Integer) stepsSpinner.getValue();
        browser.forward(steps);
        status("Went forward " + steps + " step(s).");
        refresh();
    }

    /** Moves the cursor to the selected page by going back or forward the right number of steps. */
    private void openSelected() {
        int target = historyList.getSelectedIndex();
        if (target == -1) {
            status("Select a page in the history first.");
            return;
        }
        if (target < currentIndex) {
            browser.back(currentIndex - target);
        } else if (target > currentIndex) {
            browser.forward(target - currentIndex);
        }
        status("Opened " + browser.currentPage());
        refresh();
    }

    private void deleteSelected() {
        int index = historyList.getSelectedIndex();
        if (index == -1) {
            status("Select a page in the history first.");
            return;
        }
        String url = historyModel.get(index);
        browser.deleteUrl(url);                 // removes the oldest page with this URL
        status("Deleted " + url + ". Press Restore to bring it back.");
        refresh();
    }

    private void restore() {
        if (browser.restore()) {
            status("Restored the most recently deleted page.");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Nothing was restored.\n"
                            + "Either no pages have been deleted, or the deleted page's\n"
                            + "place in the history no longer exists.",
                    "Restore", JOptionPane.INFORMATION_MESSAGE);
        }
        refresh();
    }

    private void clearHistory() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the whole history? This cannot be restored.",
                "Clear all", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            browser.deleteHistory();
            status("History cleared. Type a URL to start again.");
            refresh();
        }
    }

    private void exportHistory() {
        JFileChooser chooser = textFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String fileName = chooser.getSelectedFile().getPath();
        if (!fileName.toLowerCase().endsWith(".txt")) {
            fileName += ".txt";
        }
        try {
            browser.exportHistory(fileName);
            status("Exported history to " + fileName);
        } catch (IOException e) {
            showError("Could not export the history:\n" + e.getMessage());
        }
    }

    private void importHistory() {
        JFileChooser chooser = textFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String fileName = chooser.getSelectedFile().getPath();
        try {
            browser.importHistory(fileName);
            status("Imported history from " + fileName);
            refresh();
        } catch (IOException e) {
            showError("Could not import the history:\n" + e.getMessage());
        }
    }

    // ===================== DISPLAY =====================

    /** Redraws everything from the current state of the BrowserHistory. */
    private void refresh() {
        // fill the list from "url1 | url2 (CURRENT) | url3"
        historyModel.clear();
        currentIndex = -1;
        String history = browser.getHistoryString();
        if (!history.isEmpty()) {
            String[] pages = history.split(" \\| ");
            for (int i = 0; i < pages.length; i++) {
                String page = pages[i];
                if (page.endsWith(" (CURRENT)")) {
                    currentIndex = i;
                    page = page.substring(0, page.length() - " (CURRENT)".length());
                }
                historyModel.addElement(page);
            }
        }

        // current page area
        String url = browser.currentPage();
        String info = browser.currentPageInfo();
        if (url == null) {
            urlLabel.setText("No page open");
            visitedLabel.setText(" ");
            addressField.setText("");
        } else {
            urlLabel.setText(url);
            int start = info.indexOf("visited: ");
            visitedLabel.setText(start == -1 ? " " : "Visited: " + info.substring(start + "visited: ".length()));
            addressField.setText(url);
        }

        // page changed: clear the old load result, and load the new page if wanted
        if (url == null ? shownUrl != null : !url.equals(shownUrl)) {
            shownUrl = url;
            clearPage();
            if (url != null && loadPagesBox.isSelected()) {
                loadPage(url);
            }
        }

        backButton.setEnabled(currentIndex > 0);
        forwardButton.setEnabled(currentIndex != -1 && currentIndex < historyModel.size() - 1);

        if (currentIndex != -1) {
            historyList.ensureIndexIsVisible(currentIndex);
        }
        historyList.repaint();
        setTitle("Browser History Manager - " + historyModel.size() + " page(s)");
    }

    /** Empties the load result, the rendered page and the source. */
    private void clearPage() {
        loadLabel.setText(" ");
        titleLabel.setText(" ");
        pageView.setText("");
        bodyArea.setText("");
    }

    /** What the background download produces: the response, and the page ready to show. */
    private static class LoadedPage {
        HttpResponseData response;
        HTMLDocument document;          // null if the page could not be rendered
    }

    /**
     * Downloads and renders the page in the background so the window does not freeze.
     * Downloading uses our HttpClientService; rendering uses Swing's HTML support.
     */
    private void loadPage(String url) {
        clearPage();
        loadLabel.setText("Loading...");
        String fullUrl = url.contains("://") ? url : "https://" + url;

        SwingWorker<LoadedPage, Void> worker = new SwingWorker<LoadedPage, Void>() {
            @Override
            protected LoadedPage doInBackground() throws Exception {
                LoadedPage result = new LoadedPage();
                result.response = http.sendRequest(new HttpRequestData(fullUrl, "GET", null, null));

                String body = result.response.getBody() == null ? "" : result.response.getBody();
                try {
                    result.document = render(withoutScriptsAndStyles(body), fullUrl);
                } catch (Exception e) {
                    result.document = null;     // show the source only
                }
                return result;
            }

            @Override
            protected void done() {
                if (!url.equals(browser.currentPage())) {
                    return;                                   // the user has moved on, ignore this result
                }
                try {
                    LoadedPage result = get();
                    String body = result.response.getBody() == null ? "" : result.response.getBody();

                    loadLabel.setText("Status: " + result.response.getStatusCode()
                            + "     Size: " + body.length() + " characters");
                    titleLabel.setText("Title: " + titleOf(body));

                    if (result.document != null) {
                        pageView.setDocument(result.document);
                        pageView.setCaretPosition(0);
                    } else {
                        pageView.setText("<html><body><p>This page could not be rendered. "
                                + "See the HTML source tab.</p></body></html>");
                    }

                    if (body.length() > MAX_BODY_LENGTH) {
                        body = body.substring(0, MAX_BODY_LENGTH) + "\n\n... (cut, page is longer)";
                    }
                    bodyArea.setText(body);
                    bodyArea.setCaretPosition(0);             // scroll to the top
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    loadLabel.setText("Could not load page: " + reason);
                }
            }
        };
        worker.execute();
    }

    /**
     * Turns HTML text into a document the JEditorPane can show.
     * The base URL makes relative links and images (like "/about" or "logo.png") work.
     */
    private static HTMLDocument render(String html, String baseUrl) throws Exception {
        HTMLEditorKit kit = new HTMLEditorKit();
        HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
        document.putProperty("IgnoreCharsetDirective", Boolean.TRUE);   // don't stop at <meta charset>
        document.setBase(URI.create(baseUrl).toURL());
        kit.read(new StringReader(html), document, 0);
        return document;
    }

    /**
     * Removes <script> and <style> blocks. Swing cannot run JavaScript and only
     * understands simple CSS, and these blocks would otherwise show up as text.
     */
    private static String withoutScriptsAndStyles(String html) {
        return html.replaceAll("(?is)<script.*?</script>", "")
                .replaceAll("(?is)<style.*?</style>", "");
    }

    /** Text between <title> and </title> in the HTML, or "(no title)". */
    private static String titleOf(String html) {
        if (html == null) {
            return "(no title)";
        }
        String lower = html.toLowerCase();
        int open = lower.indexOf("<title");
        if (open == -1) {
            return "(no title)";
        }
        int start = lower.indexOf(">", open);
        int end = lower.indexOf("</title>", start);
        if (start == -1 || end == -1) {
            return "(no title)";
        }
        return html.substring(start + 1, end).trim();
    }

    private void status(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JFileChooser textFileChooser() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        return chooser;
    }

    /** Shows each page as "3. url", and adds [<] in bold for the current page. */
    private class HistoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String text = (index + 1) + ". " + value;
            if (index == currentIndex) {
                text += "   [<]";
            }
            JLabel label = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            label.setFont(label.getFont().deriveFont(index == currentIndex ? Font.BOLD : Font.PLAIN));
            return label;
        }
    }

    // ===================== START =====================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());   // native Windows look
        } catch (Exception e) {
            // keep the default look if the system one is not available
        }
        SwingUtilities.invokeLater(() -> new BrowserUI().setVisible(true));
    }
}