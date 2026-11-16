
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class CatchStars extends JPanel implements ActionListener {

    // Window settings
    static final int WIDTH = 640;
    static final int HEIGHT = 520;

    // Boo and basket position
    int booX = 270;
    int booY = 280;

    int basketWidth = 110;
    int basketHeight = 45;
    int basketY = 405;

    // Star position
    int starX;
    int starY;
    final int starSize = 25;

    // Game information
    int score = 0;
    int misses = 0;

    boolean gameOver = false;
    boolean won = false;

    final int WIN_SCORE = 20;
    final int MAX_MISSES = 3;

    Random random = new Random();
    Timer timer;

    // Colors
    Color background = new Color(239, 229, 255);
    Color purple = new Color(145, 105, 195);
    Color darkPurple = new Color(85, 55, 125);

    public CatchStars() {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(background);

        // Keyboard controls
        setFocusable(true);

        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");

        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");

        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ENTER"), "restart");

        getActionMap().put("moveLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                if (!gameOver) {
                    booX -= 25;

                    if (booX < 0) {
                        booX = 0;
                    }

                    repaint();
                }
            }
        });

        getActionMap().put("moveRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                if (!gameOver) {
                    booX += 25;

                    if (booX + basketWidth > WIDTH) {
                        booX = WIDTH - basketWidth;
                    }

                    repaint();
                }
            }
        });

        getActionMap().put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                if (gameOver) {
                    restartGame();
                }
            }
        });

        resetStar();

        timer = new Timer(20, this);
        timer.start();
    }

    // Draw everything
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Smooth drawing
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Background
        g2.setColor(background);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Decorative stars
        drawSmallStar(g2, 80, 120);
        drawSmallStar(g2, 530, 110);
        drawSmallStar(g2, 160, 220);
        drawSmallStar(g2, 570, 260);

        // Game title
        g2.setColor(darkPurple);
        g2.setFont(new Font("SansSerif", Font.BOLD, 27));
        g2.drawString("Catch Stars", 240, 42);

        // Score and Misses
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));

        g2.drawString("Score: " + score + "/20", 30, 80);

        g2.drawString("Misses: " + misses + "/3", 485, 80);

        // Falling star
        drawStar(g2, starX, starY);

        // Boo character
        drawBoo(g2);

        // Basket
        drawBasket(g2);

        // Ending screen
        if (gameOver) {
            drawEndingScreen(g2);
        }
    }

    // Draw Boo
    void drawBoo(Graphics2D g) {

        int x = booX + 10;
        int y = booY;

        // Ghost body
        g.setColor(Color.WHITE);

        Path2D body = new Path2D.Double();

        body.moveTo(x + 15, y + 65);
        body.curveTo(x + 5, y + 45, x + 15, y + 5,
                x + 50, y + 5);

        body.curveTo(x + 85, y + 5, x + 95, y + 45,
                x + 85, y + 65);

        body.curveTo(x + 75, y + 80, x + 65, y + 65,
                x + 50, y + 72);

        body.curveTo(x + 35, y + 65, x + 25, y + 80,
                x + 15, y + 65);

        body.closePath();

        g.fill(body);

        // Ghost outline
        g.setColor(new Color(220, 210, 240));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(body);

        // Eyes
        g.setColor(Color.BLACK);

        g.fillOval(x + 30, y + 35, 11, 20);
        g.fillOval(x + 60, y + 35, 11, 20);

        // Eye highlights
        g.setColor(Color.WHITE);

        g.fillOval(x + 33, y + 38, 4, 5);
        g.fillOval(x + 63, y + 38, 4, 5);

        // Cheeks
        g.setColor(new Color(255, 170, 195));

        g.fillOval(x + 13, y + 55, 17, 10);
        g.fillOval(x + 72, y + 55, 17, 10);

        // Mouth
        g.setColor(new Color(100, 45, 75));

        g.fillOval(x + 43, y + 57, 16, 15);

        // Tongue
        g.setColor(new Color(255, 120, 160));

        g.fillOval(x + 47, y + 65, 9, 7);

        // Small hands
        g.setColor(Color.WHITE);

        g.fillOval(x - 3, y + 70, 24, 23);
        g.fillOval(x + 79, y + 70, 24, 23);

        g.setColor(new Color(220, 210, 240));

        g.drawOval(x - 3, y + 70, 24, 23);
        g.drawOval(x + 79, y + 70, 24, 23);
    }

    // Draw Boo's basket
    void drawBasket(Graphics2D g) {

        int x = booX;

        // Basket handle
        g.setColor(new Color(115, 75, 150));
        g.setStroke(new BasicStroke(5));

        g.drawArc(x + 28, basketY - 25,
                55, 45, 0, 180);

        // Basket body
        g.setColor(new Color(175, 130, 215));

        g.fillRoundRect(
                x, basketY, basketWidth, basketHeight,
                18, 18
        );

        // Basket outline
        g.setColor(darkPurple);
        g.setStroke(new BasicStroke(3));

        g.drawRoundRect(
                x, basketY, basketWidth, basketHeight,
                18, 18
        );

        // Basket horizontal decoration
        g.setColor(new Color(210, 175, 240));
        g.setStroke(new BasicStroke(4));

        g.drawLine(x + 10, basketY + 14,
                x + basketWidth - 10, basketY + 14);

        g.drawLine(x + 10, basketY + 29,
                x + basketWidth - 10, basketY + 29);

        // Vertical basket decoration
        for (int i = 1; i <= 4; i++) {

            int lineX = x + i * 20;

            g.drawLine(lineX, basketY + 5,
                    lineX, basketY + basketHeight - 5);
        }
    }

    // Draw a smooth star
    void drawStar(Graphics2D g, int x, int y) {

        Shape star = createStar(x, y, starSize);

        g.setColor(new Color(255, 195, 55));
        g.fill(star);

        g.setColor(new Color(235, 155, 25));
        g.setStroke(new BasicStroke(2));
        g.draw(star);
    }

    // Create star shape
    Shape createStar(int x, int y, int size) {

        Path2D star = new Path2D.Double();

        double centerX = x + size / 2.0;
        double centerY = y + size / 2.0;

        for (int i = 0; i < 10; i++) {

            double angle = Math.PI * i / 5 - Math.PI / 2;

            double radius = (i % 2 == 0)
                    ? size / 2.0
                    : size / 4.5;

            double pointX = centerX + Math.cos(angle) * radius;
            double pointY = centerY + Math.sin(angle) * radius;

            if (i == 0) {
                star.moveTo(pointX, pointY);
            } else {
                star.lineTo(pointX, pointY);
            }
        }

        star.closePath();

        return star;
    }

    // Decorative stars
    void drawSmallStar(Graphics2D g, int x, int y) {

        g.setColor(new Color(255, 195, 120));

        g.fill(createStar(x, y, 17));
    }

    // Draw winning or losing screen
    void drawEndingScreen(Graphics2D g) {

        g.setColor(new Color(255, 255, 255, 235));

        g.fillRoundRect(110, 145, 420, 230, 30, 30);

        g.setColor(darkPurple);
        g.setFont(new Font("SansSerif", Font.BOLD, 35));

        if (won) {
            g.drawString("BOO WINS!", 220, 205);
        } else {
            g.drawString("GAME OVER!", 190, 205);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 23));

        g.drawString("Final Score: " + score + "/20", 220, 255);

        g.setFont(new Font("SansSerif", Font.PLAIN, 17));

        if (won) {
            g.drawString("Boo collected all 20 stars!", 190, 295);
        } else {
            g.drawString("Boo missed 3 stars!", 210, 295);
        }

        g.drawString("Press ENTER to play again", 190, 335);
    }

    // Move star and check collision
    @Override
    public void actionPerformed(ActionEvent e) {

        if (!gameOver) {

            // Star falls down
            starY += 5;

            // Check if star touches basket
            if (starY + starSize >= basketY &&
                starX + starSize >= booX &&
                starX <= booX + basketWidth) {

                score++;

                if (score >= WIN_SCORE) {

                    won = true;
                    gameOver = true;
                    timer.stop();

                } else {
                    resetStar();
                }
            }

            // Check if star is missed
            else if (starY > HEIGHT) {

                misses++;

                if (misses >= MAX_MISSES) {

                    won = false;
                    gameOver = true;
                    timer.stop();

                } else {
                    resetStar();
                }
            }
        }

        repaint();
    }

    // Place star randomly
    void resetStar() {

        starX = random.nextInt(WIDTH - starSize);
        starY = -starSize;
    }

    // Restart game
    void restartGame() {

        score = 0;
        misses = 0;

        booX = 270;

        gameOver = false;
        won = false;

        resetStar();

        timer.start();

        repaint();
    }

    // Launch game
    public static void launch(boolean exitOnClose) {

        JFrame frame = new JFrame("Catch Stars");

        CatchStars game = new CatchStars();

        frame.add(game);

        frame.pack();

        frame.setLocationRelativeTo(null);

        frame.setResizable(false);

        frame.setDefaultCloseOperation(
                exitOnClose
                        ? JFrame.EXIT_ON_CLOSE
                        : JFrame.DISPOSE_ON_CLOSE
        );

        frame.setVisible(true);

        game.requestFocusInWindow();
    }

    // Run independently
    public static void main(String[] args) {

        launch(true);
    }
}
