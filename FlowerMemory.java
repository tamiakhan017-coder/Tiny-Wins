import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Flower Memory - flip cards and find the matching pairs of flowers.
 *
 * Controls: mouse clicks.
 * Goal:     match all 8 pairs (16 cards).
 *
 * Click a card to flip it, then click a second card.
 *  - Same flower  -> the pair stays face up.
 *  - Different    -> both flip back, so try to remember where they were!
 *
 * Everything (flowers, cards, background, title) is drawn with code,
 * so no picture files are needed.
 */
public class FlowerMemory extends JFrame {

    static final int COLS = 4;
    static final int ROWS = 4;
    static final int PAIRS = COLS * ROWS / 2;

    // ===== COLORS =====
    static final Color PINK_TOP     = new Color(250, 128, 178);
    static final Color PINK_BOTTOM  = new Color(236, 96, 158);
    static final Color BERRY        = new Color(120, 28, 78);

    static final Color CARD_FACE    = new Color(190, 226, 247);
    static final Color CARD_FACE_2  = new Color(160, 207, 240);
    static final Color CARD_BORDER  = new Color(86, 140, 204);
    static final Color CARD_BACK    = new Color(96, 150, 214);
    static final Color CARD_BACK_2  = new Color(70, 118, 190);
    static final Color RING         = new Color(52, 96, 164);
    static final Color GOLD         = new Color(255, 196, 58);

    final Card[] cards = new Card[COLS * ROWS];

    Card first = null;      // first card of the pair being tried
    Card second = null;     // second card of the pair being tried
    boolean locked = false; // true while two wrong cards are showing
    boolean started = false;
    boolean won = false;

    int moves = 0;
    int pairsFound = 0;
    long startMillis = 0;
    long endMillis = 0;

    private StatsBar stats;
    private javax.swing.Timer animTimer;   // flips the cards smoothly
    private javax.swing.Timer clockTimer;  // updates the time display
    private javax.swing.Timer hideTimer;   // flips two wrong cards back
    private javax.swing.Timer winTimer;    // shows the "you won" box

    // ---------------------------------------------------------------
    // Starting the game
    // ---------------------------------------------------------------

    public static void main(String[] args) {
        launch(true);
    }

    // exitOnClose = true  -> closing the window quits the program
    // exitOnClose = false -> closing the window only closes this game (used by Tiny Wins)
    public static void launch(boolean exitOnClose) {
        SwingUtilities.invokeLater(() -> new FlowerMemory(exitOnClose));
    }

    FlowerMemory(boolean exitOnClose) {

        setTitle("Flower Memory");
        setDefaultCloseOperation(
            exitOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE
        );
        setResizable(false);

        animTimer  = new javax.swing.Timer(15, e -> animate());
        clockTimer = new javax.swing.Timer(1000, e -> updateStats());
        hideTimer  = new javax.swing.Timer(900, e -> hideWrongPair());
        hideTimer.setRepeats(false);
        winTimer   = new javax.swing.Timer(700, e -> showWinMessage());
        winTimer.setRepeats(false);

        // Stop all timers when the window closes
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                animTimer.stop();
                clockTimer.stop();
                hideTimer.stop();
                winTimer.stop();
            }
        });

        buildWindow();
        dealCards();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildWindow() {

        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(14, 28, 20, 28));

        // Top: title + statistics
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(new TitleBanner(), BorderLayout.CENTER);

        stats = new StatsBar();
        top.add(stats, BorderLayout.SOUTH);

        // Middle: 4 x 4 cards
        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 10, 10));
        grid.setOpaque(false);

        for (int i = 0; i < cards.length; i++) {
            cards[i] = new Card();
            grid.add(cards[i]);
        }

        JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        gridHolder.setOpaque(false);
        gridHolder.add(grid);

        // Bottom: New Game button
        JButton newGame = new JButton("New Game");
        newGame.setFont(new Font("SansSerif", Font.BOLD, 16));
        newGame.setBackground(Color.WHITE);
        newGame.setForeground(new Color(204, 52, 112));
        newGame.setFocusable(false);
        newGame.setOpaque(true);
        newGame.setContentAreaFilled(true);
        newGame.setBorder(BorderFactory.createLineBorder(new Color(204, 52, 112), 2));
        newGame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newGame.setPreferredSize(new Dimension(150, 40));
        newGame.addActionListener(e -> dealCards());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(newGame);

        root.add(top, BorderLayout.NORTH);
        root.add(gridHolder, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ---------------------------------------------------------------
    // Game rules
    // ---------------------------------------------------------------

    // Shuffle the flowers and put every card face down (also used for "New Game")
    void dealCards() {

        animTimer.stop();
        clockTimer.stop();
        hideTimer.stop();
        winTimer.stop();

        List<Integer> flowers = new ArrayList<>();
        for (int i = 0; i < PAIRS; i++) {
            flowers.add(i);
            flowers.add(i);
        }
        Collections.shuffle(flowers);

        for (int i = 0; i < cards.length; i++) {
            cards[i].flower = flowers.get(i);
            cards[i].faceUp = false;
            cards[i].matched = false;
            cards[i].progress = 0;
            cards[i].repaint();
        }

        first = null;
        second = null;
        locked = false;
        started = false;
        won = false;
        moves = 0;
        pairsFound = 0;
        startMillis = 0;
        endMillis = 0;

        updateStats();
    }

    void cardClicked(Card card) {

        // Ignore clicks while two wrong cards are showing, after winning,
        // or on a card that is already face up
        if (locked || won || card.faceUp || card.matched) {
            return;
        }

        // The clock starts with the very first flip
        if (!started) {
            started = true;
            startMillis = System.currentTimeMillis();
            clockTimer.start();
        }

        card.faceUp = true;
        startAnimation();

        if (first == null) {
            first = card;
            updateStats();
            return;
        }

        // Second card of a try
        second = card;
        moves++;

        if (first.flower == second.flower) {

            first.matched = true;
            second.matched = true;
            first.repaint();
            second.repaint();
            pairsFound++;

            first = null;
            second = null;

            if (pairsFound == PAIRS) {
                finishGame();
            }

        } else {
            locked = true;
            hideTimer.restart(); // flip both back after a short look
        }

        updateStats();
    }

    private void hideWrongPair() {

        if (first != null) first.faceUp = false;
        if (second != null) second.faceUp = false;

        first = null;
        second = null;
        locked = false;

        startAnimation();
    }

    private void finishGame() {
        won = true;
        endMillis = System.currentTimeMillis();
        clockTimer.stop();
        winTimer.restart(); // wait for the last flip to finish
    }

    private void showWinMessage() {

        int choice = JOptionPane.showOptionDialog(
            this,
            "You found all " + PAIRS + " pairs!\n\n" +
            "Moves: " + moves + "\n" +
            "Time:  " + formatTime(elapsedMillis()),
            "You did it!",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new String[] {"Play Again", "Close"},
            "Play Again"
        );

        if (choice == 0) {
            dealCards();
        }
    }

    // ---------------------------------------------------------------
    // Time and status
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

    void updateStats() {
        if (stats != null) {
            stats.setText(
                "Moves: " + moves
                + "     Pairs: " + pairsFound + "/" + PAIRS
                + "     Time: " + formatTime(elapsedMillis())
            );
        }
    }

    // ---------------------------------------------------------------
    // Flip animation
    // ---------------------------------------------------------------

    private void startAnimation() {
        if (!animTimer.isRunning()) {
            animTimer.start();
        }
    }

    private void animate() {

        boolean moving = false;

        for (Card card : cards) {

            double target = card.faceUp ? 1.0 : 0.0;
            boolean changed = false;

            if (card.progress < target) {
                card.progress = Math.min(target, card.progress + 0.12);
                changed = true;
            } else if (card.progress > target) {
                card.progress = Math.max(target, card.progress - 0.12);
                changed = true;
            }

            if (changed) {
                moving = true;
                card.repaint();
            }
        }

        if (!moving) {
            animTimer.stop();
        }
    }

    // ---------------------------------------------------------------
    // Window pieces: background, title, statistics bar
    // ---------------------------------------------------------------

    // Pink background with soft swirls
    static class BackgroundPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g.setPaint(new GradientPaint(0, 0, PINK_TOP, 0, h, PINK_BOTTOM));
            g.fillRect(0, 0, w, h);

            g.setStroke(new BasicStroke(30f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D s1 = new Path2D.Double();
            s1.moveTo(-20, h * 0.16);
            s1.curveTo(w * 0.30, -h * 0.04, w * 0.55, h * 0.34, w + 20, h * 0.12);
            g.setColor(new Color(255, 180, 214, 85));
            g.draw(s1);

            Path2D s2 = new Path2D.Double();
            s2.moveTo(-20, h * 0.58);
            s2.curveTo(w * 0.28, h * 0.40, w * 0.55, h * 0.84, w + 20, h * 0.52);
            g.setColor(new Color(222, 78, 146, 70));
            g.draw(s2);

            Path2D s3 = new Path2D.Double();
            s3.moveTo(-20, h * 0.95);
            s3.curveTo(w * 0.30, h * 0.78, w * 0.62, h * 1.10, w + 20, h * 0.86);
            g.setColor(new Color(255, 180, 214, 85));
            g.draw(s3);

            // A few soft rings
            g.setStroke(new BasicStroke(6f));
            g.setColor(new Color(255, 200, 226, 70));
            g.draw(new Ellipse2D.Double(w * 0.78, h * 0.30, 70, 70));
            g.draw(new Ellipse2D.Double(-24, h * 0.40, 80, 80));

            g.dispose();
        }
    }

    // "MEMORY" in solid white, "FLOWERS" as an outline
    static class TitleBanner extends JComponent {

        TitleBanner() {
            setPreferredSize(new Dimension(460, 62));
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontRenderContext frc = g.getFontRenderContext();

            // A heavy font if the computer has one
            String family = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
            ).contains("Arial Black") ? "Arial Black" : "SansSerif";

            float size = 38f;
            Font font = new Font(family, Font.BOLD, 38);
            TextLayout memory = new TextLayout("MEMORY", font, frc);
            TextLayout flowers = new TextLayout("FLOWERS", font, frc);
            double gap = 14;

            // Shrink the text if it doesn't fit the width
            double total = memory.getAdvance() + gap + flowers.getAdvance();
            double available = getWidth() - 10;
            if (total > available) {
                size = (float) (size * available / total);
                font = font.deriveFont(size);
                memory = new TextLayout("MEMORY", font, frc);
                flowers = new TextLayout("FLOWERS", font, frc);
                total = memory.getAdvance() + gap + flowers.getAdvance();
            }

            double x = (getWidth() - total) / 2;
            double baseline = getHeight() / 2.0 + memory.getAscent() / 2.0 - 2;

            Shape memoryShape = memory.getOutline(AffineTransform.getTranslateInstance(x, baseline));
            Shape flowersShape = flowers.getOutline(
                AffineTransform.getTranslateInstance(x + memory.getAdvance() + gap, baseline)
            );

            // Soft shadow under MEMORY
            g.setColor(new Color(170, 40, 110, 110));
            g.fill(AffineTransform.getTranslateInstance(2, 3).createTransformedShape(memoryShape));

            g.setColor(Color.WHITE);
            g.fill(memoryShape);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(flowersShape);

            g.dispose();
        }
    }

    // A white rounded bar that shows moves, pairs and time
    static class StatsBar extends JComponent {

        private String text = "";

        StatsBar() {
            setPreferredSize(new Dimension(460, 34));
        }

        void setText(String text) {
            this.text = text;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g.setColor(new Color(255, 255, 255, 215));
            g.fill(new RoundRectangle2D.Double(0, 2, w - 1, h - 4, h - 4, h - 4));

            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(BERRY);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f);

            g.dispose();
        }
    }

    // ---------------------------------------------------------------
    // One card on the board
    // ---------------------------------------------------------------

    class Card extends JComponent {

        int flower;          // which flower (0..7) is on this card
        boolean faceUp;      // should it be showing the flower?
        boolean matched;     // already paired up?
        double progress;     // 0 = back showing, 1 = flower showing (used for the flip)

        Card() {
            setPreferredSize(new Dimension(100, 104));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    cardClicked(Card.this);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();

            // Flip effect: squeeze the card to a thin line, then widen it again
            boolean showFace = progress >= 0.5;
            double scaleX = showFace ? (2 * progress - 1) : (1 - 2 * progress);
            scaleX = Math.max(scaleX, 0.03);

            g.translate(w / 2.0, h / 2.0);
            g.scale(scaleX, 1);
            g.translate(-w / 2.0, -h / 2.0);

            RoundRectangle2D shape = new RoundRectangle2D.Double(7, 3, w - 10, h - 6, 14, 14);

            if (showFace) {
                paintFace(g, shape, w, h);
            } else {
                paintBack(g, shape, w, h);
            }

            paintRings(g, h);

            g.dispose();
        }

        // Small "notebook rings" on the left edge of every card
        private void paintRings(Graphics2D g, int h) {
            for (int i = 0; i < 3; i++) {
                double y = h * (0.24 + i * 0.26);
                RoundRectangle2D ring = new RoundRectangle2D.Double(1, y - 5, 15, 10, 8, 8);
                g.setColor(RING);
                g.fill(ring);
                g.setColor(new Color(255, 255, 255, 170));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(ring);
            }
        }

        private void paintFace(Graphics2D g, RoundRectangle2D shape, int w, int h) {

            Color top = matched ? new Color(222, 242, 253) : CARD_FACE;
            Color bottom = matched ? new Color(200, 232, 250) : CARD_FACE_2;

            g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g.fill(shape);

            // thin light line inside the border
            g.setColor(new Color(255, 255, 255, 150));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(
                shape.getX() + 6, shape.getY() + 6, shape.getWidth() - 12, shape.getHeight() - 12, 9, 9));

            drawFlower(g, flower, w / 2.0 + 3, h / 2.0, Math.min(w, h) * 0.29);

            g.setColor(matched ? GOLD : CARD_BORDER);
            g.setStroke(new BasicStroke(matched ? 4.5f : 3.5f));
            g.draw(shape);
        }

        private void paintBack(Graphics2D g, RoundRectangle2D shape, int w, int h) {

            g.setPaint(new GradientPaint(0, 0, CARD_BACK, 0, h, CARD_BACK_2));
            g.fill(shape);

            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                        1f, new float[] {2f, 5f}, 0f));
            g.draw(new RoundRectangle2D.Double(
                shape.getX() + 6, shape.getY() + 6, shape.getWidth() - 12, shape.getHeight() - 12, 9, 9));

            // A little white flower on the back
            double cx = w / 2.0 + 3;
            double cy = h / 2.0;
            double s = 26;

            for (int i = 0; i < 6; i++) {
                petal(g, cx, cy, i * 60, 0.36 * s, 0.32 * s, 0.62 * s, new Color(255, 255, 255, 230), null);
            }
            disc(g, cx, cy, 0.18 * s, new Color(255, 214, 96));

            g.setColor(CARD_BORDER.darker());
            g.setStroke(new BasicStroke(3.5f));
            g.draw(shape);
        }
    }

    // ---------------------------------------------------------------
    // Drawing the flowers (s = size of the flower, cx/cy = its middle)
    // ---------------------------------------------------------------

    static void drawFlower(Graphics2D g, int type, double cx, double cy, double s) {

        switch (type) {

            case 0: { // Pink flower with violet inner petals
                for (int i = 0; i < 6; i++) {
                    petal(g, cx, cy, i * 60, 0.64 * s, 0.50 * s, 0.82 * s,
                          new Color(255, 112, 162), new Color(208, 62, 120));
                }
                for (int i = 0; i < 6; i++) {
                    petal(g, cx, cy, i * 60 + 30, 0.40 * s, 0.32 * s, 0.56 * s,
                          new Color(150, 108, 214), new Color(106, 70, 170));
                }
                disc(g, cx, cy, 0.22 * s, new Color(255, 160, 40));
                disc(g, cx, cy, 0.11 * s, new Color(255, 218, 80));
                break;
            }

            case 1: { // Dark blue leaves on a stem
                Color navy = new Color(40, 78, 152);
                Color navyEdge = new Color(24, 50, 112);
                Color vein = new Color(150, 192, 242);
                line(g, cx, cy + 1.05 * s, cx, cy - 0.15 * s, 0.09 * s, navyEdge);
                leaf(g, cx, cy + 0.80 * s, -52, 0.80 * s, 0.17 * s, navy, navyEdge, vein);
                leaf(g, cx, cy + 0.80 * s, 52, 0.80 * s, 0.17 * s, navy, navyEdge, vein);
                leaf(g, cx, cy + 0.35 * s, -48, 0.80 * s, 0.17 * s, navy, navyEdge, vein);
                leaf(g, cx, cy + 0.35 * s, 48, 0.80 * s, 0.17 * s, navy, navyEdge, vein);
                leaf(g, cx, cy - 0.05 * s, 0, 0.95 * s, 0.19 * s, navy, navyEdge, vein);
                break;
            }

            case 2: { // Orange flower
                for (int i = 0; i < 8; i++) {
                    petal(g, cx, cy, i * 45, 0.62 * s, 0.46 * s, 0.76 * s,
                          new Color(244, 92, 44), new Color(184, 54, 28));
                }
                for (int i = 0; i < 8; i++) {
                    petal(g, cx, cy, i * 45 + 22.5, 0.42 * s, 0.30 * s, 0.50 * s,
                          new Color(255, 152, 52), new Color(214, 100, 24));
                }
                disc(g, cx, cy, 0.27 * s, new Color(226, 112, 28));
                disc(g, cx, cy, 0.20 * s, new Color(255, 206, 54));
                break;
            }

            case 3: { // Yellow flower
                for (int i = 0; i < 10; i++) {
                    petal(g, cx, cy, i * 36, 0.64 * s, 0.34 * s, 0.72 * s,
                          new Color(255, 208, 48), new Color(222, 154, 18));
                }
                for (int i = 0; i < 10; i++) {
                    petal(g, cx, cy, i * 36 + 18, 0.38 * s, 0.22 * s, 0.40 * s,
                          new Color(255, 160, 40), null);
                }
                disc(g, cx, cy, 0.24 * s, new Color(214, 108, 30));
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    disc(g, cx + Math.cos(a) * 0.11 * s, cy + Math.sin(a) * 0.11 * s,
                         0.045 * s, new Color(255, 214, 90));
                }
                break;
            }

            case 4: { // Pink sprig with two small flowers
                Color teal = new Color(40, 148, 128);
                Color tealEdge = new Color(24, 104, 92);
                Color light = new Color(176, 232, 218);

                Path2D stem = new Path2D.Double();
                stem.moveTo(cx, cy + 1.08 * s);
                stem.quadTo(cx + 0.12 * s, cy + 0.25 * s, cx - 0.38 * s, cy - 0.38 * s);
                Path2D stem2 = new Path2D.Double();
                stem2.moveTo(cx + 0.04 * s, cy + 0.55 * s);
                stem2.quadTo(cx + 0.34 * s, cy + 0.42 * s, cx + 0.44 * s, cy - 0.02 * s);

                g.setColor(teal);
                g.setStroke(new BasicStroke((float) (0.08 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(stem);
                g.draw(stem2);

                leaf(g, cx + 0.03 * s, cy + 0.85 * s, -55, 0.62 * s, 0.14 * s, teal, tealEdge, light);
                leaf(g, cx + 0.06 * s, cy + 0.62 * s, 58, 0.62 * s, 0.14 * s, teal, tealEdge, light);

                smallFlower(g, cx - 0.38 * s, cy - 0.42 * s, 0.62 * s, 10);
                smallFlower(g, cx + 0.46 * s, cy - 0.06 * s, 0.50 * s, 40);
                break;
            }

            case 5: { // Light blue leaf with white veins
                Color fill = new Color(112, 186, 238);
                Color edge = new Color(58, 128, 200);
                double baseY = cy + 1.0 * s;
                line(g, cx, baseY + 0.18 * s, cx, baseY, 0.08 * s, edge);
                pointedPetal(g, cx, baseY, 0, 1.95 * s, 0.56 * s, fill, edge);

                Color white = new Color(255, 255, 255, 230);
                line(g, cx, baseY, cx, baseY - 1.62 * s, 0.06 * s, white);
                for (int i = 1; i <= 4; i++) {
                    double t = i * 0.17;
                    double y = baseY - t * 1.9 * s;
                    double reach = 0.34 * s * (1 - t * 0.9);
                    line(g, cx, y, cx - reach, y - 0.24 * s, 0.045 * s, white);
                    line(g, cx, y, cx + reach, y - 0.24 * s, 0.045 * s, white);
                }
                break;
            }

            case 6: { // Orange-yellow flower with pointed petals
                for (int i = 0; i < 12; i++) {
                    pointedPetal(g, cx, cy, i * 30, 1.0 * s, 0.17 * s,
                                 new Color(255, 146, 28), new Color(208, 96, 10));
                }
                for (int i = 0; i < 12; i++) {
                    pointedPetal(g, cx, cy, i * 30 + 15, 0.70 * s, 0.15 * s,
                                 new Color(255, 208, 52), new Color(228, 160, 20));
                }
                disc(g, cx, cy, 0.26 * s, new Color(186, 92, 24));
                disc(g, cx, cy, 0.18 * s, new Color(255, 190, 60));
                break;
            }

            case 7: { // Blue-violet flower
                for (int i = 0; i < 5; i++) {
                    petal(g, cx, cy, i * 72, 0.56 * s, 0.66 * s, 0.74 * s,
                          new Color(124, 138, 232), new Color(78, 90, 190));
                }
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(i * 72);
                    line(g, cx + Math.sin(a) * 0.26 * s, cy - Math.cos(a) * 0.26 * s,
                         cx + Math.sin(a) * 0.62 * s, cy - Math.cos(a) * 0.62 * s,
                         0.03 * s, new Color(200, 208, 250));
                }
                disc(g, cx, cy, 0.27 * s, new Color(255, 255, 255));
                disc(g, cx, cy, 0.17 * s, new Color(255, 214, 96));
                break;
            }
        }
    }

    // A small five-petal pink flower (used in the sprig)
    static void smallFlower(Graphics2D g, double x, double y, double r, double rotation) {
        for (int i = 0; i < 5; i++) {
            petal(g, x, y, rotation + i * 72, 0.50 * r, 0.62 * r, 0.76 * r,
                  new Color(255, 118, 168), new Color(208, 62, 120));
        }
        disc(g, x, y, 0.24 * r, new Color(255, 214, 80));
    }

    // A leaf with a lighter line down the middle
    static void leaf(Graphics2D g, double px, double py, double angleDeg, double len, double wid,
                     Color fill, Color edge, Color vein) {

        pointedPetal(g, px, py, angleDeg, len, wid, fill, edge);

        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(angleDeg), px, py);
        line(g, px, py, px, py - len * 0.80, 1.3, vein);
        g.setTransform(old);
    }

    // A round petal. (px,py) is the middle of the flower, the petal sits 'dist'
    // away from it, and angleDeg turns it around the middle (0 = pointing up).
    static void petal(Graphics2D g, double px, double py, double angleDeg, double dist,
                      double pw, double ph, Color fill, Color edge) {

        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(angleDeg), px, py);

        Ellipse2D shape = new Ellipse2D.Double(px - pw / 2, py - dist - ph / 2, pw, ph);
        g.setColor(fill);
        g.fill(shape);

        if (edge != null) {
            g.setColor(edge);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(shape);
        }

        g.setTransform(old);
    }

    // A pointed petal / leaf that starts at (px,py) and points up before turning
    static void pointedPetal(Graphics2D g, double px, double py, double angleDeg,
                             double len, double wid, Color fill, Color edge) {

        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(angleDeg), px, py);

        Path2D path = new Path2D.Double();
        path.moveTo(px, py);
        path.quadTo(px + wid * 1.6, py - len * 0.55, px, py - len);
        path.quadTo(px - wid * 1.6, py - len * 0.55, px, py);
        path.closePath();

        g.setColor(fill);
        g.fill(path);

        if (edge != null) {
            g.setColor(edge);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(path);
        }

        g.setTransform(old);
    }

    static void disc(Graphics2D g, double cx, double cy, double r, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    static void line(Graphics2D g, double x1, double y1, double x2, double y2,
                     double width, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
}
