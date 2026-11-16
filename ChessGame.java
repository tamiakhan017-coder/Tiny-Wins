import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class ChessGame {

    static char[][] board = {
        {'r','n','b','q','k','b','n','r'},
        {'p','p','p','p','p','p','p','p'},
        {' ',' ',' ',' ',' ',' ',' ',' '},
        {' ',' ',' ',' ',' ',' ',' ',' '},
        {' ',' ',' ',' ',' ',' ',' ',' '},
        {' ',' ',' ',' ',' ',' ',' ',' '},
        {'P','P','P','P','P','P','P','P'},
        {'R','N','B','Q','K','B','N','R'}
    };

    static JButton[][] squares = new JButton[8][8];
    static JFrame frame;

    static int selectedRow = -1;
    static int selectedCol = -1;

    static boolean whiteTurn = true;
    static boolean gameOver = false;
    static JLabel status;

    // Castling: true means "has moved (or rook was captured)"
    static boolean wKingMoved = false, bKingMoved = false;
    static boolean wRookA = false, wRookH = false;   // a-file / h-file rooks
    static boolean bRookA = false, bRookH = false;

    // En passant target square (the square a pawn skipped over), -1 if none
    static int epRow = -1, epCol = -1;

    // Game timer: starts at the first move, stops when the game ends
    static JLabel timerLabel;
    static javax.swing.Timer clock;
    static long startMillis = 0;
    static long endMillis = 0;

    static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    static long elapsedMillis() {
        if (startMillis == 0) return 0;
        long end = (endMillis != 0) ? endMillis : System.currentTimeMillis();
        return end - startMillis;
    }

    static void refreshTimerLabel() {
        if (timerLabel != null) {
            timerLabel.setText("Time: " + formatTime(elapsedMillis()));
        }
    }

    static void startClock() {
        if (startMillis != 0) return; // already running
        startMillis = System.currentTimeMillis();
        endMillis = 0;
        clock = new javax.swing.Timer(1000, e -> refreshTimerLabel());
        clock.start();
        refreshTimerLabel();
    }

    static void stopClock() {
        if (clock != null) clock.stop();
        if (startMillis != 0 && endMillis == 0) {
            endMillis = System.currentTimeMillis();
        }
        refreshTimerLabel();
    }

    // Draw rules: 50-move counter (in half-moves) and position repetition
    static int halfmoveClock = 0;
    static HashMap<String, Integer> positionCounts = new HashMap<>();

    static {
        positionCounts.put(positionKey(), 1); // starting position counts once
    }

    static Color light = new Color(240, 217, 181);
    static Color dark = new Color(181, 136, 99);

    // Run chess on its own
    public static void main(String[] args) {
        launch(true);
    }

    // Put everything back to a brand-new game (so the menu can open chess again and again)
    static void resetGame() {
        board = new char[][] {
            {'r','n','b','q','k','b','n','r'},
            {'p','p','p','p','p','p','p','p'},
            {' ',' ',' ',' ',' ',' ',' ',' '},
            {' ',' ',' ',' ',' ',' ',' ',' '},
            {' ',' ',' ',' ',' ',' ',' ',' '},
            {' ',' ',' ',' ',' ',' ',' ',' '},
            {'P','P','P','P','P','P','P','P'},
            {'R','N','B','Q','K','B','N','R'}
        };
        selectedRow = -1;
        selectedCol = -1;
        whiteTurn = true;
        gameOver = false;
        wKingMoved = bKingMoved = false;
        wRookA = wRookH = bRookA = bRookH = false;
        epRow = -1;
        epCol = -1;
        halfmoveClock = 0;
        positionCounts.clear();
        positionCounts.put(positionKey(), 1);
        if (clock != null) clock.stop();
        startMillis = 0;
        endMillis = 0;
    }

    // exitOnClose = true  -> closing the window quits the program (standalone)
    // exitOnClose = false -> closing the window only closes chess (used by Tiny Wins)
    public static void launch(boolean exitOnClose) {

        resetGame();

        SwingUtilities.invokeLater(() -> {

            try {
                // Makes button colors show correctly on every OS
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { }

            frame = new JFrame("My Chess Game");
            frame.setDefaultCloseOperation(
                exitOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    if (clock != null) clock.stop(); // don't keep ticking after closing
                }
            });
            frame.setLayout(new BorderLayout());

            JPanel chessBoard = new JPanel(new GridLayout(8, 8));

            status = new JLabel("White's turn");
            status.setFont(new Font("Arial", Font.BOLD, 18));
            status.setHorizontalAlignment(SwingConstants.CENTER);
            status.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {

                    final int r = row;
                    final int c = col;

                    JButton square = new JButton();
                    squares[row][col] = square;

                    square.setFont(new Font("Serif", Font.PLAIN, 42));
                    square.setFocusPainted(false);
                    square.setOpaque(true);
                    square.setContentAreaFilled(true);
                    square.setBorderPainted(true);

                    square.addActionListener(e -> handleClick(r, c));

                    chessBoard.add(square);
                }
            }

            timerLabel = new JLabel("Time: 00:00");
            timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
            timerLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 15));

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.add(status, BorderLayout.CENTER);
            topBar.add(timerLabel, BorderLayout.EAST);
            // Invisible spacer on the left keeps the status text centered
            JLabel spacer = new JLabel(" ");
            spacer.setPreferredSize(timerLabel.getPreferredSize());
            topBar.add(spacer, BorderLayout.WEST);

            frame.add(topBar, BorderLayout.NORTH);
            frame.add(chessBoard, BorderLayout.CENTER);

            updateBoard();

            frame.setSize(680, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // ------------------------------------------------------------
    // Click handling
    // ------------------------------------------------------------

    static void handleClick(int row, int col) {

        if (gameOver) {
            return;
        }

        char clickedPiece = board[row][col];

        // Select a piece
        if (selectedRow == -1) {

            if (clickedPiece == ' ') {
                return;
            }

            if (!isCurrentPlayerPiece(clickedPiece)) {
                status.setText("It's " + currentPlayer() + "'s turn!");
                return;
            }

            selectedRow = row;
            selectedCol = col;

            status.setText(currentPlayer() + ": choose a destination");
            updateBoard();
            return;
        }

        // Click another friendly piece to select it instead
        if (isCurrentPlayerPiece(clickedPiece)) {

            selectedRow = row;
            selectedCol = col;

            status.setText(currentPlayer() + ": choose a destination");
            updateBoard();
            return;
        }

        char piece = board[selectedRow][selectedCol];

        // Attempt the move
        if (isLegalMove(selectedRow, selectedCol, row, col)) {

            int sr = selectedRow;
            int sc = selectedCol;

            updateCastlingRights(sr, sc, row, col, piece);

            // En passant target for the NEXT move
            int newEpRow = -1, newEpCol = -1;
            if (Character.toLowerCase(piece) == 'p' && Math.abs(row - sr) == 2) {
                newEpRow = (row + sr) / 2;
                newEpCol = sc;
            }

            boolean isCapture = board[row][col] != ' ';
            boolean isPawnMove = Character.toLowerCase(piece) == 'p';

            applyMove(board, sr, sc, row, col);

            epRow = newEpRow;
            epCol = newEpCol;

            // 50-move rule counter: resets on any pawn move or capture
            if (isPawnMove || isCapture) {
                halfmoveClock = 0;
            } else {
                halfmoveClock++;
            }

            // Pawn promotion: let the player choose
            if (piece == 'P' && row == 0) {
                board[row][col] = askPromotion(true);
            } else if (piece == 'p' && row == 7) {
                board[row][col] = askPromotion(false);
            }

            whiteTurn = !whiteTurn;
            selectedRow = -1;
            selectedCol = -1;

            // Remember this position for threefold repetition
            positionCounts.merge(positionKey(), 1, Integer::sum);

            // The clock starts with the very first move
            startClock();

            updateGameStatus();

        } else {
            selectedRow = -1;
            selectedCol = -1;

            if (isInCheck(board, whiteTurn)) {
                status.setText("Illegal move! " + currentPlayer() + " is in check.");
            } else {
                status.setText("Illegal move! Try another square.");
            }
        }

        updateBoard();
    }

    static char askPromotion(boolean white) {

        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        char[] pieces = {'Q', 'R', 'B', 'N'};

        int choice = JOptionPane.showOptionDialog(
            frame,
            "Promote pawn to:",
            "Pawn Promotion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice < 0) {
            choice = 0; // closed the dialog -> queen
        }

        char p = pieces[choice];
        return white ? p : Character.toLowerCase(p);
    }

    // After each move: check, checkmate, stalemate, draw by material
    static void updateGameStatus() {

        boolean inCheck = isInCheck(board, whiteTurn);
        boolean hasMove = hasAnyLegalMove(whiteTurn);

        if (!hasMove) {
            gameOver = true;
            if (inCheck) {
                String winner = whiteTurn ? "Black" : "White";
                status.setText("Checkmate! " + winner + " wins!");
            } else {
                status.setText("Stalemate! The game is a draw.");
            }
        } else if (insufficientMaterial()) {
            gameOver = true;
            status.setText("Draw: insufficient material.");
        } else if (halfmoveClock >= 100) {
            gameOver = true;
            status.setText("Draw: 50-move rule.");
        } else if (positionCounts.getOrDefault(positionKey(), 0) >= 3) {
            gameOver = true;
            status.setText("Draw: threefold repetition.");
        } else if (inCheck) {
            status.setText(currentPlayer() + " is in CHECK!");
        } else {
            status.setText(currentPlayer() + "'s turn");
        }

        // Game finished: stop the clock and show how long it took
        if (gameOver) {
            stopClock();
            status.setText(status.getText() + " Game time: " + formatTime(elapsedMillis()));
        }
    }

    // ------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------

    static boolean isCurrentPlayerPiece(char piece) {

        if (piece == ' ') {
            return false;
        }

        if (whiteTurn) {
            return Character.isUpperCase(piece);
        }

        return Character.isLowerCase(piece);
    }

    static String currentPlayer() {
        return whiteTurn ? "White" : "Black";
    }

    // A move is fully legal if the piece can make it AND it does not
    // leave your own king in check.
    static boolean isLegalMove(int sr, int sc, int dr, int dc) {

        char piece = board[sr][sc];

        if (piece == ' ' || !isCurrentPlayerPiece(piece)) {
            return false;
        }

        if (!isPseudoLegal(board, sr, sc, dr, dc)) {
            return false;
        }

        char[][] copy = copyBoard(board);
        applyMove(copy, sr, sc, dr, dc);

        return !isInCheck(copy, Character.isUpperCase(piece));
    }

    // Movement rules only (ignores whether your king ends up in check)
    static boolean isPseudoLegal(char[][] b, int sr, int sc, int dr, int dc) {

        if (sr == dr && sc == dc) {
            return false;
        }

        char piece = b[sr][sc];
        char target = b[dr][dc];

        if (piece == ' ') {
            return false;
        }

        // Cannot capture your own piece
        if (target != ' ' &&
            Character.isUpperCase(piece) == Character.isUpperCase(target)) {
            return false;
        }

        int rowDiff = dr - sr;
        int colDiff = dc - sc;

        int ar = Math.abs(rowDiff);
        int ac = Math.abs(colDiff);

        boolean white = Character.isUpperCase(piece);

        switch (Character.toLowerCase(piece)) {

            case 'p': {

                int direction = white ? -1 : 1;
                int startRow = white ? 6 : 1;

                // Move forward one square
                if (colDiff == 0 && rowDiff == direction && target == ' ') {
                    return true;
                }

                // Move forward two squares on first move
                if (colDiff == 0 &&
                    sr == startRow &&
                    rowDiff == 2 * direction &&
                    target == ' ' &&
                    b[sr + direction][sc] == ' ') {
                    return true;
                }

                // Capture diagonally
                if (ac == 1 && rowDiff == direction && target != ' ') {
                    return true;
                }

                // En passant
                if (ac == 1 && rowDiff == direction && target == ' ' &&
                    dr == epRow && dc == epCol) {
                    return true;
                }

                return false;
            }

            case 'r':
                if (sr != dr && sc != dc) {
                    return false;
                }
                return pathClear(b, sr, sc, dr, dc);

            case 'b':
                if (ar != ac) {
                    return false;
                }
                return pathClear(b, sr, sc, dr, dc);

            case 'q':
                if (sr != dr && sc != dc && ar != ac) {
                    return false;
                }
                return pathClear(b, sr, sc, dr, dc);

            case 'n':
                return (ar == 2 && ac == 1) || (ar == 1 && ac == 2);

            case 'k':
                if (ar <= 1 && ac <= 1) {
                    return true;
                }
                // Castling
                if (ar == 0 && ac == 2) {
                    return canCastle(b, white, dc > sc);
                }
                return false;

            default:
                return false;
        }
    }

    static boolean canCastle(char[][] b, boolean white, boolean kingside) {

        int row = white ? 7 : 0;
        char king = white ? 'K' : 'k';
        char rook = white ? 'R' : 'r';

        boolean kingMoved = white ? wKingMoved : bKingMoved;
        boolean rookMoved = white
            ? (kingside ? wRookH : wRookA)
            : (kingside ? bRookH : bRookA);

        if (kingMoved || rookMoved) {
            return false;
        }

        if (b[row][4] != king) {
            return false;
        }

        int rookCol = kingside ? 7 : 0;
        if (b[row][rookCol] != rook) {
            return false;
        }

        // Squares between king and rook must be empty
        int from = Math.min(4, rookCol) + 1;
        int to = Math.max(4, rookCol) - 1;
        for (int c = from; c <= to; c++) {
            if (b[row][c] != ' ') {
                return false;
            }
        }

        // King may not castle out of, through, or into check
        int step = kingside ? 1 : -1;
        boolean byWhite = !white;
        if (isSquareAttacked(b, row, 4, byWhite) ||
            isSquareAttacked(b, row, 4 + step, byWhite) ||
            isSquareAttacked(b, row, 4 + 2 * step, byWhite)) {
            return false;
        }

        return true;
    }

    // Performs a move on the given board (handles en passant and castling).
    // Promotion is handled separately in handleClick.
    static void applyMove(char[][] b, int sr, int sc, int dr, int dc) {

        char piece = b[sr][sc];

        // En passant capture: pawn moves diagonally onto an empty square
        if (Character.toLowerCase(piece) == 'p' &&
            sc != dc && b[dr][dc] == ' ') {
            b[sr][dc] = ' ';
        }

        // Castling: move the rook too
        if (Character.toLowerCase(piece) == 'k' && Math.abs(dc - sc) == 2) {
            if (dc > sc) {
                b[dr][5] = b[dr][7];
                b[dr][7] = ' ';
            } else {
                b[dr][3] = b[dr][0];
                b[dr][0] = ' ';
            }
        }

        b[dr][dc] = piece;
        b[sr][sc] = ' ';
    }

    static void updateCastlingRights(int sr, int sc, int dr, int dc, char piece) {

        if (piece == 'K') wKingMoved = true;
        if (piece == 'k') bKingMoved = true;

        // Any move from or onto a rook's starting square removes that right
        if ((sr == 7 && sc == 0) || (dr == 7 && dc == 0)) wRookA = true;
        if ((sr == 7 && sc == 7) || (dr == 7 && dc == 7)) wRookH = true;
        if ((sr == 0 && sc == 0) || (dr == 0 && dc == 0)) bRookA = true;
        if ((sr == 0 && sc == 7) || (dr == 0 && dc == 7)) bRookH = true;
    }

    // ------------------------------------------------------------
    // Check detection
    // ------------------------------------------------------------

    static boolean isInCheck(char[][] b, boolean white) {

        char king = white ? 'K' : 'k';

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (b[r][c] == king) {
                    return isSquareAttacked(b, r, c, !white);
                }
            }
        }

        return false;
    }

    // Is square (r,c) attacked by any piece of the given color?
    static boolean isSquareAttacked(char[][] b, int r, int c, boolean byWhite) {

        for (int pr = 0; pr < 8; pr++) {
            for (int pc = 0; pc < 8; pc++) {

                char p = b[pr][pc];

                if (p == ' ' || Character.isUpperCase(p) != byWhite) {
                    continue;
                }

                int rd = r - pr;
                int cd = c - pc;
                int ar = Math.abs(rd);
                int ac = Math.abs(cd);

                switch (Character.toLowerCase(p)) {

                    case 'p': {
                        int dir = byWhite ? -1 : 1;
                        if (rd == dir && ac == 1) return true;
                        break;
                    }

                    case 'n':
                        if ((ar == 2 && ac == 1) || (ar == 1 && ac == 2)) return true;
                        break;

                    case 'k':
                        if (ar <= 1 && ac <= 1 && (ar + ac) > 0) return true;
                        break;

                    case 'r':
                        if ((rd == 0 || cd == 0) && pathClear(b, pr, pc, r, c)) return true;
                        break;

                    case 'b':
                        if (ar == ac && ar > 0 && pathClear(b, pr, pc, r, c)) return true;
                        break;

                    case 'q':
                        if (((rd == 0 || cd == 0) || ar == ac) &&
                            (ar + ac) > 0 && pathClear(b, pr, pc, r, c)) return true;
                        break;
                }
            }
        }

        return false;
    }

    static boolean hasAnyLegalMove(boolean white) {

        for (int sr = 0; sr < 8; sr++) {
            for (int sc = 0; sc < 8; sc++) {

                char p = board[sr][sc];

                if (p == ' ' || Character.isUpperCase(p) != white) {
                    continue;
                }

                for (int dr = 0; dr < 8; dr++) {
                    for (int dc = 0; dc < 8; dc++) {

                        if (!isPseudoLegal(board, sr, sc, dr, dc)) {
                            continue;
                        }

                        char[][] copy = copyBoard(board);
                        applyMove(copy, sr, sc, dr, dc);

                        if (!isInCheck(copy, white)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // Draw when checkmate is impossible:
    //  - King vs King
    //  - King + one Knight vs King
    //  - King + Bishop(s) vs King + Bishop(s), all bishops on the same square color
    static boolean insufficientMaterial() {

        int knights = 0;
        int bishops = 0;
        int bishopColors = 0; // bit 0 = light squares, bit 1 = dark squares

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {

                char p = Character.toLowerCase(board[r][c]);

                if (p == ' ' || p == 'k') {
                    continue;
                }

                if (p == 'n') {
                    knights++;
                } else if (p == 'b') {
                    bishops++;
                    bishopColors |= 1 << ((r + c) % 2);
                } else {
                    return false; // pawn, rook or queen can still mate
                }
            }
        }

        if (knights == 0 && bishops == 0) return true;   // K vs K
        if (knights == 1 && bishops == 0) return true;   // K+N vs K
        if (knights == 0 && bishops >= 1 &&
            (bishopColors == 1 || bishopColors == 2)) {
            return true;                                 // bishops all on one color
        }

        return false;
    }

    // A string that identifies a position for repetition counting:
    // pieces, side to move, castling rights, and en passant (only if it is really possible).
    static String positionKey() {

        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                sb.append(board[r][c]);
            }
        }

        sb.append(whiteTurn ? 'w' : 'b');

        sb.append(!wKingMoved && !wRookH && board[7][4] == 'K' && board[7][7] == 'R' ? 'K' : '-');
        sb.append(!wKingMoved && !wRookA && board[7][4] == 'K' && board[7][0] == 'R' ? 'Q' : '-');
        sb.append(!bKingMoved && !bRookH && board[0][4] == 'k' && board[0][7] == 'r' ? 'k' : '-');
        sb.append(!bKingMoved && !bRookA && board[0][4] == 'k' && board[0][0] == 'r' ? 'q' : '-');

        boolean epPossible = false;

        if (epRow != -1) {
            int dir = whiteTurn ? -1 : 1;
            char pawn = whiteTurn ? 'P' : 'p';
            int pr = epRow - dir; // row of the pawn that would capture

            for (int dc = -1; dc <= 1; dc += 2) {
                int pc = epCol + dc;
                if (pr >= 0 && pr < 8 && pc >= 0 && pc < 8 &&
                    board[pr][pc] == pawn &&
                    isLegalMove(pr, pc, epRow, epCol)) {
                    epPossible = true;
                }
            }
        }

        if (epPossible) {
            sb.append(epRow).append(epCol);
        }

        return sb.toString();
    }

    static char[][] copyBoard(char[][] b) {

        char[][] copy = new char[8][8];

        for (int r = 0; r < 8; r++) {
            copy[r] = b[r].clone();
        }

        return copy;
    }

    static boolean pathClear(char[][] b, int sr, int sc, int dr, int dc) {

        int rowStep = Integer.compare(dr, sr);
        int colStep = Integer.compare(dc, sc);

        int r = sr + rowStep;
        int c = sc + colStep;

        while (r != dr || c != dc) {

            if (b[r][c] != ' ') {
                return false;
            }

            r += rowStep;
            c += colStep;
        }

        return true;
    }

    // ------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------

    static String getSymbol(char piece) {

        switch (piece) {
            case 'K': return "♔";
            case 'Q': return "♕";
            case 'R': return "♖";
            case 'B': return "♗";
            case 'N': return "♘";
            case 'P': return "♙";

            case 'k': return "♚";
            case 'q': return "♛";
            case 'r': return "♜";
            case 'b': return "♝";
            case 'n': return "♞";
            case 'p': return "♟";

            default: return "";
        }
    }

    static void updateBoard() {

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                JButton square = squares[row][col];

                square.setText(getSymbol(board[row][col]));

                if ((row + col) % 2 == 0) {
                    square.setBackground(light);
                } else {
                    square.setBackground(dark);
                }

                if (row == selectedRow && col == selectedCol) {

                    square.setBorder(
                        BorderFactory.createLineBorder(Color.YELLOW, 4)
                    );

                } else if (selectedRow != -1 &&
                           isLegalMove(selectedRow, selectedCol, row, col)) {

                    // Show where the selected piece can legally go
                    square.setBorder(
                        BorderFactory.createLineBorder(new Color(0, 160, 0), 3)
                    );

                } else {

                    square.setBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1)
                    );
                }
            }
        }
    }
}
