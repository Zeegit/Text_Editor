package editor;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
    JTextField inputName;
    JTextArea textArea;
    JFileChooser fileChooser;
    JTextField searchField;
    JCheckBox useRegExCheckbox;

    ArrayList<SearchResult> searchResult;
    int searchPos;
    private boolean searchDone;

    class SearchResult {
        int start;
        int end;
        String group;

        public SearchResult(int start, int end, String group) {
            this.start = start;
            this.end = end;
            this.group = group;
        }


    }

    public class Seacher extends SwingWorker<String, String> {
        private String text;
        private String regexp;
        private boolean useRegexp;

        Pattern pattern;
        Matcher matcher;

        ArrayList<SearchResult> sr;

        public Seacher(String text, String regexp, boolean useRegexp) {
            this.text = text;
            this.regexp = regexp;
            this.useRegexp = useRegexp;
            sr = new ArrayList<>();
        }


        @Override
        protected String doInBackground() {
            if (useRegexp) {
                pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(text);

                while (matcher.find()) {
                    sr.add(new SearchResult(matcher.start(), matcher.end(), matcher.group()));
                }
            } else {
                int start = -1;
                while (true) {
                    start = text.indexOf(regexp, start + 1);
                    if (start == -1) {
                        break;
                    }
                    sr.add(new SearchResult(start, start + regexp.length(), regexp));
                }
            }
            return null;
        }

        @Override
        protected void done() {
            setSearchResult(sr);
        }
    }

    private synchronized void setSearchResult(ArrayList<SearchResult> searchResult) {
        this.searchResult = searchResult;
        searchPos = -1;
        searchDone = true;
        setSearchPositionNext();
    }

    public class LoadActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            try {
                textArea.setText("");
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    textArea.setText(readFileAsString(selectedFile.getCanonicalPath()));
                    setPosition(0, 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class SaveActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            int returnValue = fileChooser.showSaveDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try (PrintWriter printWriter = new PrintWriter(selectedFile)) {
                    printWriter.print(textArea.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class SearchActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            Seacher task = new Seacher(textArea.getText(), searchField.getText(), useRegExCheckbox.isSelected());
            task.execute();
        }
    }

    public class NextMatchActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            setSearchPositionNext();
        }
    }

    public class PrevMatchActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            setSearchPositionPrev();
        }
    }

    void setSearchPositionNext() {
        if (searchDone) {
            searchPos++;
            if (searchPos >= searchResult.size()) searchPos = 0;
            setPosition(searchResult.get(searchPos).start, searchResult.get(searchPos).start+searchResult.get(searchPos).group.length());
        }
    }

    void setSearchPositionPrev() {

        if (searchDone) {
            searchPos--;
            if (searchPos < 0) searchPos = searchResult.size()-1;
            setPosition(searchResult.get(searchPos).start, searchResult.get(searchPos).start+searchResult.get(searchPos).group.length());
        }
    }

    void setPosition(int start, int end) {
        textArea.setCaretPosition(start);
        textArea.select(start, end);
        textArea.grabFocus();
    }

    public TextEditor() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 400);
        setTitle("Text Editor");

        fileChooser = new JFileChooser();
        fileChooser.setName("FileChooser");
        add(fileChooser);

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);

        setJMenuBar(createMenu());

        setVisible(true);

    }

    JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        menuFile.setName("MenuFile");

        JMenuItem loadMenuItem = new JMenuItem("Load");
        loadMenuItem.setName("MenuOpen");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setName("MenuSave");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setName("MenuExit");

        JMenu menuSearch = new JMenu("Search");
        menuSearch.setName("MenuSearch");

        JMenuItem menuStartSearch = new JMenuItem("Start search");
        menuStartSearch.setName("MenuStartSearch");
        JMenuItem menuPreviousMatch = new JMenuItem("Pevious search");
        menuPreviousMatch.setName("MenuPreviousMatch");
        JMenuItem menuNextMatch = new JMenuItem("Nexy match");
        menuNextMatch.setName("MenuNextMatch");
        JMenuItem menuUseRegExp = new JMenuItem("Use regular expression");
        menuUseRegExp.setName("MenuUseRegExp");


        loadMenuItem.addActionListener(new LoadActionListener());
        saveMenuItem.addActionListener(new SaveActionListener());
        exitMenuItem.addActionListener(actionEvent -> dispose());
        menuStartSearch.addActionListener(new SearchActionListener());
        menuPreviousMatch.addActionListener(new PrevMatchActionListener());
        menuNextMatch.addActionListener(new NextMatchActionListener());
        menuUseRegExp.addActionListener(actionEvent -> useRegExCheckbox.setSelected(!useRegExCheckbox.isSelected()));

        menuFile.add(loadMenuItem);
        menuFile.add(saveMenuItem);
        menuFile.addSeparator();
        menuFile.add(exitMenuItem);

        menuSearch.add(menuStartSearch);
        menuSearch.add(menuPreviousMatch);
        menuSearch.add(menuNextMatch);
        menuSearch.add(menuUseRegExp);

        menuBar.add(menuFile);
        menuBar.add(menuSearch);

        return menuBar;
    }

    JComponent createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.YELLOW);

        textArea = new JTextArea();
        textArea.setName("TextArea");

        JScrollPane scrollableTextArea = new JScrollPane(textArea);
        scrollableTextArea.setName("ScrollPane");

        scrollableTextArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollableTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        centerPanel.add(scrollableTextArea, BorderLayout.CENTER);

        return centerPanel;
    }

    JComponent createTopPanel() {
        JPanel topPanel = new JPanel();
        //topPanel.setBackground(Color.GREEN);

        searchField = new JTextField();
        searchField.setName("SearchField");
        searchField.setColumns(20);

        JButton loadButton = new JButton(new ImageIcon("images/open.png"));
        loadButton.setName("OpenButton");
        loadButton.addActionListener(new LoadActionListener());

        JButton saveButton = new JButton(new ImageIcon("images/save.png"));
        saveButton.setName("SaveButton");
        saveButton.addActionListener(new SaveActionListener());

        JButton startSearchButton = new JButton(new ImageIcon("images/search.png"));
        startSearchButton.setName("StartSearchButton");
        startSearchButton.addActionListener(new SearchActionListener());

        JButton previousMatchButton = new JButton(new ImageIcon("images/back.png"));
        previousMatchButton.setName("PreviousMatchButton");
        previousMatchButton.addActionListener(new PrevMatchActionListener());

        JButton nextMatchButton = new JButton(new ImageIcon("images/next.png"));
        nextMatchButton.setName("NextMatchButton");
        nextMatchButton.addActionListener(new NextMatchActionListener());

        useRegExCheckbox = new JCheckBox("Use regex");
        useRegExCheckbox.setName("UseRegExCheckbox");

        topPanel.add(loadButton);
        topPanel.add(saveButton);

        topPanel.add(searchField);

        topPanel.add(startSearchButton);
        topPanel.add(previousMatchButton);
        topPanel.add(nextMatchButton);
        topPanel.add(useRegExCheckbox);

        return topPanel;
    }

    public static String readFileAsString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

}
