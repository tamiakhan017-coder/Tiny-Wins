import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Picture sliding puzzle (the classic "8-puzzle").
 *
 * Rules:
 *  - The picture is cut into 9 tiles and the bottom-right tile is removed,
 *    which leaves one empty space.
 *  - You can only slide a tile that touches the empty space (up, down, left, right).
 *  - Put all the tiles back in the right order to finish the picture.
 *
 * The shuffle is made by sliding tiles randomly from the finished picture,
 * so every puzzle can always be solved.
 *
 * The picture must be saved as  puzzle.png  (or puzzle.jpg)  in the same folder
 * as the .java files.
 */
public class PuzzleGame extends JFrame {

    static final int SIZE = 3;               // 3 x 3 board
    static final int TILES = SIZE * SIZE;
    static final int MISSING = TILES - 1;    // the tile that is taken out (bottom-right)

    // Picture file names the game looks for (first one found is used)
    static final String[] PICTURE_NAMES = {"puzzle.png", "puzzle.jpg", "puzzle.jpeg"};

    private final ImageIcon[] icons = new ImageIcon[TILES];
    private ImageIcon preview;               // the whole picture, for "Show Picture"
    final int[] order = new int[TILES];      // order[position] = which tile is there
    private final JButton[] buttons = new JButton[TILES];
    private JLabel status;

    int moves = 0;
    boolean solved = false;
    boolean started = false;
    long startMillis = 0;
    long endMillis = 0;
    private javax.swing.Timer clock;         // updates the time display

    // ===== COLORS (same strawberry milkshake look as the Tiny Wins menu) =====
    private static final Color BACKGROUND = new Color(255, 232, 238);
    private static final Color STRAWBERRY = new Color(230, 100, 140);
    private static final Color BERRY      = new Color(139, 46, 84);
    private static final Color EMPTY_TILE = new Color(250, 214, 224);

    // ---------------------------------------------------------------
    // Starting the game
    // ---------------------------------------------------------------

    // Run the puzzle on its own
    public static void main(String[] args) {
        launch(true);
    }

    // exitOnClose = true  -> closing the window quits the program
    // exitOnClose = false -> closing the window only closes the puzzle (used by Tiny Wins)
    public static void launch(boolean exitOnClose) {
        SwingUtilities.invokeLater(() -> {

            BufferedImage picture = loadPicture();

            if (picture == null) {
                JOptionPane.showMessageDialog(
                    null,
                    "Cannot find the puzzle picture!\n" +
                    "Save it as  puzzle.png  in the same folder as PuzzleGame.java.",
                    "Puzzle",
                    JOptionPane.ERROR_MESSAGE
                );
                if (exitOnClose) {
                    System.exit(0);
                }
                return; // from the menu: just don't open the puzzle
            }

            new PuzzleGame(picture, exitOnClose);
        });
    }

    // Look for the picture in the folder the program was started from,
    // and also next to the compiled .class files.
    static BufferedImage loadPicture() {

        File[] folders = new File[2];
        folders[0] = new File(".");

        try {
            File here = new File(
                PuzzleGame.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            folders[1] = here.isDirectory() ? here : here.getParentFile();
        } catch (Exception ignored) { }

        for (File folder : folders) {
            if (folder == null) continue;

            for (String name : PICTURE_NAMES) {
                File file = new File(folder, name);

                if (file.isFile()) {
                    try {
                        BufferedImage image = ImageIO.read(file);
                        if (image != null) {
                            return image;
                        }
                    } catch (Exception ignored) { }
                }
            }
        }

        return null;
    }

    PuzzleGame(BufferedImage picture, boolean exitOnClose) {

        setTitle("Picture Puzzle");
        setDefaultCloseOperation(
            exitOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE
        );
        setResizable(false);

        clock = new javax.swing.Timer(1000, e -> updateBoard());

        // Stop the clock when the window closes
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                clock.stop();
            }
        });

        makeTiles(picture);
        createInterface();
        shuffleTiles();
        updateBoard();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---------------------------------------------------------------
    // Cutting the picture
    // ---------------------------------------------------------------

    private void makeTiles(BufferedImage picture) {

        int cutW = picture.getWidth() / SIZE;
        int cutH = picture.getHeight() / SIZE;

        // Keep the picture's shape (no stretching): fit the board into about 600 x 460
        double scale = Math.min(600.0 / (cutW * SIZE), 460.0 / (cutH * SIZE));
        int tileW = (int) Math.round(cutW * scale);
        int tileH = (int) Math.round(cutH * scale);

        for (int i = 0; i < TILES; i++) {

            BufferedImage piece = picture.getSubimage(
                (i % SIZE) * cutW,
                (i / SIZE) * cutH,
                cutW,
                cutH
            );

            Image scaled = piece.getScaledInstance(tileW, tileH, Image.SCALE_SMOOTH);
            icons[i] = new ImageIcon(scaled);
        }

        // Whole picture for the "Show Picture" button
        double previewScale = Math.min(560.0 / picture.getWidth(), 420.0 / picture.getHeight());
        preview = new ImageIcon(picture.getScaledInstance(
            (int) Math.round(picture.getWidth() * previewScale),
            (int) Math.round(picture.getHeight() * previewScale),
            Image.SCALE_SMOOTH
        ));
    }

    // ---------------------------------------------------------------
    // Window
    // ---------------------------------------------------------------

    private void createInterface() {

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(STRAWBERRY, 8),
            BorderFactory.createEmptyBorder(14, 24, 18, 24)
        ));

        JLabel title = new JLabel("Picture Puzzle", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 34));
        title.setForeground(BERRY);
        root.add(title, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(SIZE, SIZE, 4, 4));
        board.setBackground(STRAWBERRY);
        board.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        for (int i = 0; i < TILES; i++) {

            JButton button = new JButton();
            button.setFocusable(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setBackground(EMPTY_TILE);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setPreferredSize(new Dimension(
                icons[0].getIconWidth(), icons[0].getIconHeight()
            ));

            final int position = i;
            button.addActionListener(e -> clickTile(position));

            buttons[i] = button;
            board.add(button);
        }

        JPanel boardHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardHolder.setOpaque(false);
        boardHolder.add(board);
        root.add(boardHolder, BorderLayout.CENTER);

        status = new JLabel("", SwingConstants.CENTER);
        status.setFont(new Font("SansSerif", Font.BOLD, 15));
        status.setForeground(BERRY);

        JButton shuffleButton = makeButton("Shuffle Again");
        shuffleButton.addActionListener(e -> {
            shuffleTiles();
            updateBoard();
        });

        JButton pictureButton = makeButton("Show Picture");
        pictureButton.addActionListener(e ->
            JOptionPane.showMessageDialog(
                this,
                new JLabel(preview),
                "The finished picture",
                JOptionPane.PLAIN_MESSAGE
            )
        );

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(shuffleButton);
        buttonRow.add(pictureButton);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(buttonRow, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JButton makeButton(String text) {

        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setBackground(STRAWBERRY);
        button.setForeground(Color.WHITE);
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createLineBorder(BERRY, 2));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 40));
        return button;
    }

    // ---------------------------------------------------------------
    // Rules
    // ---------------------------------------------------------------

    // Start from the finished picture, then slide tiles around at random.
    // (Shuffling by legal moves guarantees the puzzle can be solved.)
    void shuffleTiles() {

        Random random = new Random();

        do {
            for (int i = 0; i < TILES; i++) {
                order[i] = i;
            }

            int empty = TILES - 1;
            int previous = -1;

            for (int step = 0; step < 300; step++) {

                int[] neighbours = neighboursOf(empty);
                int pick;

                // Don't immediately undo the last slide
                do {
                    pick = neighbours[random.nextInt(neighbours.length)];
                } while (pick == previous && neighbours.length > 1);

                swap(empty, pick);
                previous = empty;
                empty = pick;
            }
        } while (isSolved()); // never start already finished

        moves = 0;
        solved = false;
        started = false;
        startMillis = 0;
        endMillis = 0;
        clock.stop();
    }

    private int[] neighboursOf(int position) {

        int r = position / SIZE;
        int c = position % SIZE;

        int[] temp = new int[4];
        int count = 0;

        if (r > 0)        temp[count++] = position - SIZE;
        if (r < SIZE - 1) temp[count++] = position + SIZE;
        if (c > 0)        temp[count++] = position - 1;
        if (c < SIZE - 1) temp[count++] = position + 1;

        return Arrays.copyOf(temp, count);
    }

    private void swap(int a, int b) {
        int t = order[a];
        order[a] = order[b];
        order[b] = t;
    }

    boolean isSolved() {
        for (int i = 0; i < TILES; i++) {
            if (order[i] != i) {
                return false;
            }
        }
        return true;
    }

    private int emptyPosition() {
        for (int i = 0; i < TILES; i++) {
            if (order[i] == MISSING) {
                return i;
            }
        }
        return -1;
    }

    // Can this tile slide into the empty space? (only if they touch)
    boolean canSlide(int position) {
        int empty = emptyPosition();
        int dr = Math.abs(position / SIZE - empty / SIZE);
        int dc = Math.abs(position % SIZE - empty % SIZE);
        return dr + dc == 1;
    }

    void clickTile(int position) {

        if (solved) {
            return;
        }

        if (!canSlide(position)) {
            status.setText("Only tiles next to the empty space can move!");
            return;
        }

        // The clock starts with the very first slide
        if (!started) {
            started = true;
            startMillis = System.currentTimeMillis();
            clock.start();
        }

        swap(position, emptyPosition());
        moves++;

        if (isSolved()) {
            solved = true;
            endMillis = System.currentTimeMillis();
            clock.stop();
            updateBoard();

            JOptionPane.showMessageDialog(
                this,
                "You finished the picture!\n\n" +
                "Moves: " + moves + "\n" +
                "Time:  " + formatTime(elapsedMillis()),
                "Puzzle Completed!",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            updateBoard();
        }
    }

    // ---------------------------------------------------------------
    // Time
    // ---------------------------------------------------------------

    long elapsedMillis() {
        if (!started) return 0;
        long end = (endMillis != 0) ? endMillis : System.currentTimeMillis();
        return end - startMillis;
    }

    static String formatTime(long millis) {
        long total = millis / 1000;
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    // ---------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------

    void updateBoard() {

        for (int i = 0; i < TILES; i++) {

            int tile = order[i];
            boolean hidden = (tile == MISSING) && !solved;

            buttons[i].setIcon(hidden ? null : icons[tile]);
            buttons[i].setBorder(
                BorderFactory.createLineBorder(hidden ? EMPTY_TILE : Color.WHITE, 2)
            );
        }

        String time = formatTime(elapsedMillis());

        if (solved) {
            status.setText("You did it!  Moves: " + moves + "   Time: " + time);
        } else if (moves == 0) {
            status.setText("Slide a tile into the empty space!   Time: " + time);
        } else {
            status.setText("Moves: " + moves + "   Time: " + time);
        }
    }
}
