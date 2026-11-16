import javax.swing.*;
import java.awt.*;

/**
 * Tiny Wins - the main menu that holds all the games.
 *
 * To add a game:
 * 1. Put the game's .java file in this same folder.
 * 2. Give it a public static void launch(boolean exitOnClose) method.
 * 3. Add one entry in the GAMES list below.
 */

public class TinyWins {

    // A menu entry stores the game title, subtitle and launch action.
    static class Game {

        final String title;
        final String subtitle;
        final Runnable action;

        // Constructor: assigns values to each game.
        Game(String title, String subtitle, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
        }
    }

    // ===== LIST OF GAMES =====
    // Each entry creates one game button in the menu.

    static final Game[] GAMES = {

        // Game 1: Chess
        new Game("Chess", "Two players, full rules",
                () -> ChessGame.launch(false)),

        // Game 2: Catch Stars
        new Game("Catch Stars", "Help Boo catch the stars",
                () -> CatchStars.launch(false)),

        // Game 3: Flower Memory
        new Game("Flower Memory", "Flip cards, match flowers",
                () -> FlowerMemory.launch(false)),

        // Game 4: Puzzle
        new Game("Puzzle", "Slide the tiles to finish it",
                () -> PuzzleGame.launch(false))
    };

    // ===== MENU COLORS (strawberry milkshake) =====

    static final Color BACKGROUND = new Color(255, 232, 238); // milky strawberry pink
    static final Color STRAWBERRY = new Color(230, 100, 140); // buttons and window frame
    static final Color BERRY = new Color(139, 46, 84);        // title and button borders
    static final Color TAGLINE = new Color(214, 96, 135);     // small text under the title
    static final Color SOON_BACKGROUND = new Color(250, 214, 224); // "coming soon" buttons
    static final Color SOON_TEXT = new Color(190, 125, 148);

    // ===== MAIN METHOD =====
    // This method starts the Tiny Wins application.

    public static void main(String[] args) {

        // Runs Swing GUI code safely on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {

            // Create the main window.
            JFrame frame = new JFrame("Tiny Wins");

            // Closing the main window exits the application.
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Main panel uses BorderLayout.
            JPanel root = new JPanel(new BorderLayout(0, 20));

            // Set background color.
            root.setBackground(BACKGROUND);

            // Strawberry frame around the whole window,
            // then empty space between the frame and the content.
            root.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(STRAWBERRY, 8),
                    BorderFactory.createEmptyBorder(25, 35, 25, 35)
                )
            );

            // ===== TITLE =====

            JLabel title = new JLabel(
                "Tiny Wins",
                SwingConstants.CENTER
            );

            title.setFont(new Font("Serif", Font.BOLD, 44));
            title.setForeground(BERRY);

            // Small subtitle under the main title.
            JLabel tagline = new JLabel(
                "Pick a game and play",
                SwingConstants.CENTER
            );

            tagline.setFont(new Font("Arial", Font.PLAIN, 16));
            tagline.setForeground(TAGLINE);

            // Header contains title and subtitle.
            JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));

            // Make header transparent.
            header.setOpaque(false);

            header.add(title);
            header.add(tagline);

            // ===== GAME BUTTON GRID =====
            // GridLayout(2, 2) creates 2 rows and 2 columns.

            JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));

            grid.setOpaque(false);

            // Loop through every game in the GAMES array.
            for (Game game : GAMES) {

                // Create a button for each game.
                grid.add(makeButton(game));
            }

            // Add header at the top.
            root.add(header, BorderLayout.NORTH);

            // Add game buttons in the center.
            root.add(grid, BorderLayout.CENTER);

            // Put the main panel inside the window.
            frame.setContentPane(root);

            // Set window size.
            frame.setSize(560, 520);

            // Set minimum window size.
            frame.setMinimumSize(new Dimension(420, 400));

            // Open window in the center of the screen.
            frame.setLocationRelativeTo(null);

            // Display the window.
            frame.setVisible(true);
        });
    }

    // ===== CREATE GAME BUTTON =====
    // This method creates and returns one button for a game.

    static JButton makeButton(Game game) {

        // If action is not null, the game is ready.
        boolean ready = game.action != null;

        // Create button with HTML formatting for title and subtitle.
        JButton button = new JButton(
            "<html><div style='text-align:center'>" +
            "<span style='font-size:20px'><b>" +
            game.title +
            "</b></span><br>" +
            "<span style='font-size:12px'>" +
            game.subtitle +
            "</span>" +
            "</div></html>"
        );

        // Remove focus border when clicked.
        button.setFocusPainted(false);

        // Enable button background.
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        // Set button border.
        button.setBorder(
            BorderFactory.createLineBorder(BERRY, 2)
        );

        // Ready games have strawberry buttons.
        // Coming Soon games have a lighter pink background.
        button.setBackground(
            ready ? STRAWBERRY : SOON_BACKGROUND
        );

        // Set text color.
        button.setForeground(
            ready ? Color.WHITE : SOON_TEXT
        );

        // ===== IF GAME IS READY =====

        if (ready) {

            // Show hand cursor when hovering over a playable game.
            button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            // What happens when the user clicks the button?
            button.addActionListener(e -> {

                try {

                    // Run the selected game's launch action.
                    game.action.run();

                } catch (Throwable ex) {

                    // Show error if the game cannot open.
                    JOptionPane.showMessageDialog(
                        button,
                        "Could not open " + game.title + ":\n" + ex,
                        "Tiny Wins",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            });

        } else {

            // If the game is not ready, show Coming Soon message.
            button.addActionListener(e ->
                JOptionPane.showMessageDialog(
                    button,
                    game.title + " is coming soon!",
                    "Tiny Wins",
                    JOptionPane.INFORMATION_MESSAGE
                )
            );
        }

        // Return the completed button.
        return button;
    }
}
