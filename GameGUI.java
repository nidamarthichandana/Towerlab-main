import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Main game window - this handles all the UI stuff for the game
public class GameGUI extends JFrame {
    private final GameSettings settings;
    private GameBoard board;
    private BoardPanel boardPanel;
    private javax.swing.Timer gameLoop;
    private long lastTick;
    private boolean paused = false;
    private double autoStartDelay = 3.0;

    // What the player is doing right now
    private String selectedTowerType = "Basic";
    private int selectedCol = -1;   // selected existing tower
    private int selectedRow = -1;
    private int hoverCol = -1;
    private int hoverRow = -1;

    // Used to notice things that just happened (wave started, life lost...)
    private boolean prevWaveActive = false;
    private int prevLives;
    private boolean resultShown = false;

    // Little messages and effects drawn on the board
    private String toastText = "";
    private Color toastColor = Theme.TEXT;
    private double toastTime = 0;
    private double damageFlash = 0;

    private static final String[] TOWER_TYPES = {"Basic", "Sniper", "Frost", "Cannon"};
    private static final String[] TOWER_DESC = {
            "Balanced damage and speed",
            "Huge damage, long range",
            "Slows enemies for 2 seconds",
            "Splash damage to groups"};

    // HUD
    private StatChip waveChip, moneyChip, livesChip, scoreChip;
    // side panel
    private TowerCard[] towerCards;
    private JLabel infoTitle, infoStats;
    private Theme.RoundButton upgradeButton, sellButton, startWaveButton, pauseButton;
    private ResultCard resultCard;
    private JPanel buttonRow;

    // Board colors
    private static final Color GRASS = new Color(92, 148, 82);
    private static final Color GRASS_DARK = new Color(85, 139, 76);
    private static final Color PATH_COLOR = new Color(214, 190, 142);
    private static final Color PATH_EDGE = new Color(176, 150, 104);

    public GameGUI(GameSettings settings) {
        this.settings = settings;
        this.board = new GameBoard(settings);
        this.prevLives = board.getLives();

        setTitle("TowerLab - " + settings.getPlayerName() + " (" + settings.getDifficulty() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        add(buildHud(), BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        JPanel boardHolder = new JPanel(new BorderLayout());
        boardHolder.setBackground(Theme.BG);
        boardHolder.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 6));
        boardHolder.add(boardPanel, BorderLayout.NORTH);   // NORTH keeps the board at its exact size
        add(boardHolder, BorderLayout.CENTER);

        add(buildSidePanel(), BorderLayout.EAST);

        setupKeys();

        // ---- game loop: runs about 60 times a second ----
        lastTick = System.nanoTime();
        gameLoop = new javax.swing.Timer(16, e -> tick());
        gameLoop.start();

        clearSelection();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---------- building the window ----------

    private JPanel buildHud() {
        JPanel hud = new JPanel(new BorderLayout(14, 0));
        hud.setBackground(Theme.BG);
        hud.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        // player name on the left
        JPanel who = new JPanel();
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.setOpaque(false);
        JLabel name = Theme.label(settings.getPlayerName(), Font.BOLD, 17, Theme.TEXT);
        JLabel diff = Theme.label(settings.getDifficulty() + " difficulty", Font.PLAIN, 12, Theme.MUTED);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        diff.setAlignmentX(Component.LEFT_ALIGNMENT);
        who.add(Box.createVerticalGlue());
        who.add(name);
        who.add(diff);
        who.add(Box.createVerticalGlue());
        who.setPreferredSize(new Dimension(190, 50));
        hud.add(who, BorderLayout.WEST);

        // stat chips
        JPanel chips = new JPanel(new GridLayout(1, 4, 10, 0));
        chips.setOpaque(false);
        waveChip = new StatChip("WAVE", 0);
        moneyChip = new StatChip("MONEY", 1);
        livesChip = new StatChip("LIVES", 2);
        scoreChip = new StatChip("SCORE", 3);
        chips.add(waveChip);
        chips.add(moneyChip);
        chips.add(livesChip);
        chips.add(scoreChip);
        hud.add(chips, BorderLayout.CENTER);

        // thin progress bar for the current wave
        WaveBar bar = new WaveBar();
        bar.setPreferredSize(new Dimension(10, 8));
        JPanel barHolder = new JPanel(new BorderLayout());
        barHolder.setBackground(Theme.BG);
        barHolder.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        barHolder.add(bar, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(Theme.BG);
        north.add(hud, BorderLayout.CENTER);
        north.add(barHolder, BorderLayout.SOUTH);
        return north;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.BG);
        side.setBorder(BorderFactory.createEmptyBorder(0, 6, 14, 14));

        JLabel towersLabel = Theme.label("TOWERS", Font.BOLD, 11, Theme.MUTED);
        towersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(towersLabel);
        side.add(Box.createVerticalStrut(6));

        towerCards = new TowerCard[TOWER_TYPES.length];
        for (int i = 0; i < TOWER_TYPES.length; i++) {
            towerCards[i] = new TowerCard(i);
            towerCards[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            towerCards[i].setPreferredSize(new Dimension(240, 54));
            towerCards[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            side.add(towerCards[i]);
            side.add(Box.createVerticalStrut(4));
        }
        side.add(Box.createVerticalStrut(4));

        // info card for the selected tower
        Theme.RoundPanel infoCard = new Theme.RoundPanel(Theme.PANEL, 16);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.setPreferredSize(new Dimension(240, 128));
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        infoTitle = Theme.label(" ", Font.BOLD, 16, Theme.TEXT);
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoStats = Theme.label(" ", Font.PLAIN, 13, Theme.MUTED);
        infoStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoStats.setVerticalAlignment(SwingConstants.TOP);
        infoCard.add(infoTitle);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(infoStats);
        infoCard.add(Box.createVerticalGlue());

        upgradeButton = new Theme.RoundButton("Upgrade", Theme.GOOD);
        sellButton = new Theme.RoundButton("Sell", Theme.BAD);
        upgradeButton.addActionListener(e -> {
            if (selectedCol >= 0) {
                board.upgradeTower(selectedCol, selectedRow);
                refreshTowerInfo();
            }
        });
        sellButton.addActionListener(e -> {
            if (selectedCol >= 0) {
                board.sellTower(selectedCol, selectedRow);
                clearSelection();
            }
        });
        buttonRow = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        buttonRow.setPreferredSize(new Dimension(100, 36));
        buttonRow.add(upgradeButton);
        buttonRow.add(sellButton);
        infoCard.add(buttonRow);
        side.add(infoCard);

        side.add(Box.createVerticalGlue());

        startWaveButton = new Theme.RoundButton("Start Wave 1", Theme.ACCENT);
        startWaveButton.setFont(Theme.font(Font.BOLD, 17));
        startWaveButton.setForeground(Theme.ACCENT_DARK_TEXT);
        startWaveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        startWaveButton.setPreferredSize(new Dimension(240, 46));
        startWaveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        startWaveButton.addActionListener(e -> board.startWave());
        side.add(Box.createVerticalStrut(8));
        side.add(startWaveButton);
        side.add(Box.createVerticalStrut(6));

        JPanel row = new JPanel(new GridLayout(1, 3, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setPreferredSize(new Dimension(240, 32));

        pauseButton = new Theme.RoundButton("Pause", Theme.CARD);
        pauseButton.setFont(Theme.font(Font.BOLD, 12));
        pauseButton.addActionListener(e -> togglePause());
        Theme.RoundButton restartButton = new Theme.RoundButton("Restart", Theme.CARD);
        restartButton.setFont(Theme.font(Font.BOLD, 12));
        restartButton.addActionListener(e -> restart());
        Theme.RoundButton menuButton = new Theme.RoundButton("Menu", Theme.CARD);
        menuButton.setFont(Theme.font(Font.BOLD, 12));
        menuButton.addActionListener(e -> backToMenu());
        row.add(pauseButton);
        row.add(restartButton);
        row.add(menuButton);
        side.add(row);
        side.add(Box.createVerticalStrut(6));

        JLabel hint = Theme.label("1-4 tower  \u00B7  Space wave  \u00B7  P pause", Font.PLAIN, 11, Theme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(hint);
        return side;
    }

    // Keyboard shortcuts
    private void setupKeys() {
        for (int i = 0; i < TOWER_TYPES.length; i++) {
            final int index = i;
            bindKey(KeyEvent.VK_1 + i, () -> selectType(index));
        }
        bindKey(KeyEvent.VK_SPACE, () -> board.startWave());
        bindKey(KeyEvent.VK_P, () -> togglePause());
    }

    private void bindKey(int keyCode, Runnable action) {
        String name = "key" + keyCode;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---------- actions ----------

    private void selectType(int index) {
        selectedTowerType = TOWER_TYPES[index];
        clearSelection();
    }

    private void togglePause() {
        if (board.isGameOver()) return;
        paused = !paused;
        pauseButton.setText(paused ? "Resume" : "Pause");
    }

    private void backToMenu() {
        gameLoop.stop();
        dispose();
        new MainMenu();
    }

    // Start the same game over again
    private void restart() {
        board = new GameBoard(settings);
        paused = false;
        pauseButton.setText("Pause");
        autoStartDelay = 3.0;
        prevWaveActive = false;
        prevLives = board.getLives();
        resultShown = false;
        resultCard.setVisible(false);
        toastTime = 0;
        damageFlash = 0;
        clearSelection();
    }

    private void showToast(String text, Color color) {
        toastText = text;
        toastColor = color;
        toastTime = 2.2;
    }

    // Called by the timer every frame
    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTick) / 1_000_000_000.0;
        lastTick = now;
        dt = Math.min(dt, 0.05);   // avoid huge jumps if the window freezes

        if (!paused) {
            board.update(dt);

            // optional auto start of the next wave
            if (settings.isAutoStartWaves() && !board.isWaveActive() && !board.isGameOver()
                    && board.getWave() < GameBoard.MAX_WAVES) {
                autoStartDelay -= dt;
                if (autoStartDelay <= 0) {
                    board.startWave();
                    autoStartDelay = 3.0;
                }
            }
        }

        // notice what just happened and tell the player
        if (board.isWaveActive() && !prevWaveActive) {
            showToast("Wave " + board.getWave() + " incoming!", Theme.ACCENT);
        } else if (!board.isWaveActive() && prevWaveActive && !board.isGameOver()) {
            showToast("Wave " + board.getWave() + " cleared!  +$" + (25 + board.getWave() * 5) + " bonus", Theme.GOOD);
        }
        prevWaveActive = board.isWaveActive();

        if (board.getLives() < prevLives) {
            damageFlash = 0.4;
        }
        prevLives = board.getLives();

        if (board.isGameOver() && !resultShown) {
            resultShown = true;
            toastTime = 0;
            clearSelection();
            resultCard.update();
            resultCard.setVisible(true);
        }

        toastTime = Math.max(0, toastTime - dt);
        damageFlash = Math.max(0, damageFlash - dt);

        updateLabels();
        boardPanel.repaint();
    }

    private void updateLabels() {
        waveChip.setValue(board.getWave() + " / " + GameBoard.MAX_WAVES);
        moneyChip.setValue("$" + board.getMoney());
        livesChip.setValue(String.valueOf(board.getLives()));
        scoreChip.setValue(String.valueOf(board.getScore()));

        boolean canStart = !board.isWaveActive() && !board.isGameOver()
                && board.getWave() < GameBoard.MAX_WAVES;
        startWaveButton.setEnabled(canStart);
        if (board.isGameOver()) {
            startWaveButton.setText(board.isVictory() ? "All waves cleared" : "Game over");
        } else if (canStart) {
            startWaveButton.setText("Start Wave " + (board.getWave() + 1));
        } else {
            startWaveButton.setText("Wave " + board.getWave() + " - " + board.getEnemiesRemaining() + " left");
        }

        // upgrade button depends on money so keep it fresh
        if (selectedCol >= 0) {
            Tower t = board.getTowerAt(selectedCol, selectedRow);
            if (t == null) {
                clearSelection();
            } else {
                upgradeButton.setEnabled(t.canUpgrade() && board.getMoney() >= t.getUpgradeCost());
            }
        }
        for (TowerCard c : towerCards) c.repaint();
    }

    // ---------- selection helpers ----------

    private void clearSelection() {
        selectedCol = -1;
        selectedRow = -1;
        Tower preview = GameBoard.createTower(selectedTowerType, 0, 0);
        infoTitle.setText(selectedTowerType + " tower");
        infoStats.setText("<html>" + TOWER_DESC[indexOfType(selectedTowerType)] + "<br>"
                + "Damage " + (int) preview.getDamage() + "  \u00B7  Range " + (int) preview.getRange() + "</html>");
        buttonRow.setVisible(false);
    }

    private int indexOfType(String type) {
        for (int i = 0; i < TOWER_TYPES.length; i++) {
            if (TOWER_TYPES[i].equals(type)) return i;
        }
        return 0;
    }

    private void selectTower(int col, int row) {
        selectedCol = col;
        selectedRow = row;
        refreshTowerInfo();
    }

    private void refreshTowerInfo() {
        Tower t = board.getTowerAt(selectedCol, selectedRow);
        if (t == null) {
            clearSelection();
            return;
        }
        infoTitle.setText(t.getName() + " tower  \u00B7  Lv " + t.getLevel());
        infoStats.setText("<html>Damage " + (int) t.getDamage() + "  \u00B7  Range " + (int) t.getRange() + "<br>"
                + "Fire rate " + String.format("%.1f", 1.0 / t.getCooldown()) + " / sec</html>");
        buttonRow.setVisible(true);
        sellButton.setText("Sell +$" + t.getSellValue());
        if (t.canUpgrade()) {
            upgradeButton.setText("Upgrade $" + t.getUpgradeCost());
            upgradeButton.setEnabled(board.getMoney() >= t.getUpgradeCost());
        } else {
            upgradeButton.setText("Max level");
            upgradeButton.setEnabled(false);
        }
    }

    // ---------- small custom components ----------

    // One of the four numbers at the top (wave, money, lives, score)
    private static class StatChip extends JComponent {
        private final String label;
        private final int icon;
        private String value = "0";

        StatChip(String label, int icon) {
            this.label = label;
            this.icon = icon;
            setPreferredSize(new Dimension(130, 52));
        }

        void setValue(String v) {
            if (!v.equals(value)) {
                value = v;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setColor(Theme.PANEL);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);

            int cx = 26;
            int cy = getHeight() / 2;
            if (icon == 0) {            // wave: three rising bars
                g.setColor(Theme.BLUE);
                g.fillRoundRect(cx - 10, cy + 2, 6, 8, 2, 2);
                g.fillRoundRect(cx - 3, cy - 4, 6, 14, 2, 2);
                g.fillRoundRect(cx + 4, cy - 10, 6, 20, 2, 2);
            } else if (icon == 1) {     // money: gold coin
                g.setColor(Theme.ACCENT);
                g.fillOval(cx - 11, cy - 11, 22, 22);
                g.setColor(new Color(200, 130, 20));
                g.drawOval(cx - 11, cy - 11, 22, 22);
                g.setFont(Theme.font(Font.BOLD, 14));
                g.setColor(Theme.ACCENT_DARK_TEXT);
                g.drawString("$", cx - g.getFontMetrics().stringWidth("$") / 2, cy + 5);
            } else if (icon == 2) {     // lives: heart
                Path2D heart = new Path2D.Double();
                heart.moveTo(cx, cy + 10);
                heart.curveTo(cx - 17, cy - 1, cx - 9, cy - 14, cx, cy - 5);
                heart.curveTo(cx + 9, cy - 14, cx + 17, cy - 1, cx, cy + 10);
                heart.closePath();
                g.setColor(Theme.BAD);
                g.fill(heart);
            } else {                    // score: star
                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 12 : 5;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    double px = cx + Math.cos(a) * r;
                    double py = cy + Math.sin(a) * r;
                    if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
                }
                star.closePath();
                g.setColor(new Color(250, 214, 90));
                g.fill(star);
            }

            g.setFont(Theme.font(Font.BOLD, 10));
            g.setColor(Theme.MUTED);
            g.drawString(label, 50, 19);
            g.setFont(Theme.font(Font.BOLD, 20));
            g.setColor(Theme.TEXT);
            g.drawString(value, 50, 40);
            g.dispose();
        }
    }

    // Thin bar showing how much of the current wave is done
    private class WaveBar extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setColor(Theme.PANEL);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            int w = (int) (getWidth() * board.getWaveProgress());
            if (w > 0) {
                g.setColor(board.isWaveActive() ? Theme.ACCENT : Theme.GOOD);
                g.fillRoundRect(0, 0, Math.max(w, getHeight()), getHeight(), getHeight(), getHeight());
            }
            g.dispose();
        }
    }

    // Clickable card for choosing which tower to build
    private class TowerCard extends JComponent {
        private final int index;
        private boolean hover = false;

        TowerCard(int index) {
            this.index = index;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { selectType(index); }

                @Override
                public void mouseEntered(MouseEvent e) { hover = true; }

                @Override
                public void mouseExited(MouseEvent e) { hover = false; }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            String type = TOWER_TYPES[index];
            int cost = GameBoard.getTowerCost(type);
            boolean affordable = board.getMoney() >= cost;
            boolean selected = type.equals(selectedTowerType);
            int w = getWidth();
            int h = getHeight();

            g.setColor(hover ? Theme.CARD_HOVER : Theme.CARD);
            g.fillRoundRect(0, 0, w, h, 28, 28);
            if (selected) {
                g.setColor(Theme.ACCENT);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, w - 3, h - 3, 28, 28);
            }

            // little tower icon
            Color tc = GameBoard.createTower(type, 0, 0).getColor();
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(13, h / 2 - 17, 36, 36);
            g.setColor(tc.darker());
            g.fillOval(12, h / 2 - 19, 36, 36);
            g.setColor(tc);
            g.fillOval(16, h / 2 - 15, 28, 28);
            g.setColor(new Color(255, 255, 255, 90));
            g.fillOval(21, h / 2 - 12, 10, 8);

            g.setFont(Theme.font(Font.BOLD, 15));
            g.setColor(affordable ? Theme.TEXT : Theme.MUTED);
            g.drawString(type, 60, 23);

            g.setFont(Theme.font(Font.BOLD, 14));
            g.setColor(affordable ? Theme.ACCENT : Theme.BAD);
            String costText = "$" + cost;
            g.drawString(costText, w - 14 - g.getFontMetrics().stringWidth(costText), 23);

            g.setFont(Theme.font(Font.PLAIN, 11));
            g.setColor(Theme.MUTED);
            g.drawString(TOWER_DESC[index], 60, 41);

            g.setFont(Theme.font(Font.BOLD, 10));
            g.setColor(Theme.MUTED);
            g.drawString(String.valueOf(index + 1), w - 18, 41);
            g.dispose();
        }
    }

    // The win / lose screen that appears over the board
    private class ResultCard extends Theme.RoundPanel {
        private final JLabel title = new JLabel("", SwingConstants.CENTER);
        private final JLabel subtitle = new JLabel("", SwingConstants.CENTER);
        private final JLabel[] statValues = new JLabel[4];

        ResultCard() {
            super(Theme.PANEL, 24);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));

            title.setFont(Theme.font(Font.BOLD, 44));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            subtitle.setFont(Theme.font(Font.PLAIN, 14));
            subtitle.setForeground(Theme.MUTED);
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(title);
            add(Box.createVerticalStrut(2));
            add(subtitle);
            add(Box.createVerticalStrut(20));

            String[] captions = {"WAVES REACHED", "SCORE", "ENEMIES DEFEATED", "TOWERS BUILT"};
            JPanel stats = new JPanel(new GridLayout(2, 2, 10, 10));
            stats.setOpaque(false);
            stats.setAlignmentX(Component.CENTER_ALIGNMENT);
            stats.setMaximumSize(new Dimension(380, 150));
            stats.setPreferredSize(new Dimension(380, 150));
            for (int i = 0; i < 4; i++) {
                Theme.RoundPanel box = new Theme.RoundPanel(Theme.CARD, 14);
                box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                box.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                JLabel cap = Theme.label(captions[i], Font.BOLD, 10, Theme.MUTED);
                cap.setAlignmentX(Component.LEFT_ALIGNMENT);
                statValues[i] = Theme.label("0", Font.BOLD, 24, Theme.TEXT);
                statValues[i].setAlignmentX(Component.LEFT_ALIGNMENT);
                statValues[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                box.add(cap);
                box.add(Box.createVerticalStrut(2));
                box.add(statValues[i]);
                stats.add(box);
            }
            add(stats);
            add(Box.createVerticalStrut(20));

            JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
            buttons.setOpaque(false);
            buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttons.setMaximumSize(new Dimension(380, 48));
            buttons.setPreferredSize(new Dimension(380, 48));
            Theme.RoundButton again = new Theme.RoundButton("Play Again", Theme.ACCENT);
            again.setFont(Theme.font(Font.BOLD, 16));
            again.setForeground(Theme.ACCENT_DARK_TEXT);
            again.addActionListener(e -> restart());
            Theme.RoundButton menu = new Theme.RoundButton("Main Menu", Theme.CARD);
            menu.setFont(Theme.font(Font.BOLD, 16));
            menu.addActionListener(e -> backToMenu());
            buttons.add(again);
            buttons.add(menu);
            add(buttons);
        }

        // fill in the numbers when the game ends
        void update() {
            boolean win = board.isVictory();
            title.setText(win ? "VICTORY!" : "DEFEATED");
            title.setForeground(win ? Theme.ACCENT : Theme.BAD);
            setBorderColor(win ? Theme.ACCENT : Theme.BAD);
            subtitle.setText(win
                    ? "You held the line through all " + GameBoard.MAX_WAVES + " waves, " + settings.getPlayerName() + "!"
                    : "The enemies broke through on wave " + board.getWave() + ".");
            statValues[0].setText(board.getWave() + " / " + GameBoard.MAX_WAVES);
            statValues[1].setText(String.valueOf(board.getScore()));
            statValues[2].setText(String.valueOf(board.getEnemiesKilled()));
            statValues[3].setText(String.valueOf(board.getTowersBuilt()));

            // the numbers changed size, so lay everything out again
            revalidate();
            repaint();
        }
    }

    // ---------- the drawing panel ----------

    private class BoardPanel extends JPanel {
        private final List<int[]> decorations = new ArrayList<>();   // little bushes on the grass

        BoardPanel() {
            setLayout(new GridBagLayout());
            setPreferredSize(new Dimension(GameBoard.COLS * GameBoard.CELL, GameBoard.ROWS * GameBoard.CELL));

            Random rand = new Random(7);
            for (int c = 0; c < GameBoard.COLS; c++) {
                for (int r = 0; r < GameBoard.ROWS; r++) {
                    if (!board.isPathCell(c, r) && rand.nextInt(100) < 35) {
                        decorations.add(new int[]{c * GameBoard.CELL + 6 + rand.nextInt(30),
                                r * GameBoard.CELL + 6 + rand.nextInt(30), 3 + rand.nextInt(4)});
                    }
                }
            }

            resultCard = new ResultCard();
            resultCard.setVisible(false);
            add(resultCard);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleClick(e.getX() / GameBoard.CELL, e.getY() / GameBoard.CELL);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverCol = -1;
                    hoverRow = -1;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverCol = e.getX() / GameBoard.CELL;
                    hoverRow = e.getY() / GameBoard.CELL;
                }
            });
        }

        private void handleClick(int col, int row) {
            if (board.isGameOver() || paused) return;
            if (col < 0 || col >= GameBoard.COLS || row < 0 || row >= GameBoard.ROWS) return;

            if (board.getTowerAt(col, row) != null) {
                selectTower(col, row);
            } else if (board.isPathCell(col, row)) {
                showToast("You can't build on the path", Theme.BAD);
            } else if (board.getMoney() < GameBoard.getTowerCost(selectedTowerType)) {
                showToast("Not enough money for a " + selectedTowerType + " tower", Theme.BAD);
            } else if (board.placeTower(selectedTowerType, col, row)) {
                clearSelection();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            Theme.smooth(g);
            int cell = GameBoard.CELL;

            drawTiles(g, cell);
            drawMarkers(g, cell);

            // hover preview (range circle + tinted tile)
            if (hoverCol >= 0 && hoverCol < GameBoard.COLS && hoverRow >= 0 && hoverRow < GameBoard.ROWS
                    && !board.isGameOver() && !paused) {
                if (board.canBuild(hoverCol, hoverRow)) {
                    boolean affordable = board.getMoney() >= GameBoard.getTowerCost(selectedTowerType);
                    g.setColor(affordable ? new Color(255, 255, 255, 70) : new Color(255, 80, 80, 80));
                    g.fillRoundRect(hoverCol * cell + 2, hoverRow * cell + 2, cell - 4, cell - 4, 10, 10);
                    drawRange(g, GameBoard.createTower(selectedTowerType, hoverCol, hoverRow));
                }
            }

            drawTowers(g);
            drawEnemies(g);

            // projectiles
            for (Projectile p : board.getProjectiles()) {
                g.setColor(p.getOwner().getColor().brighter());
                g.fillOval((int) p.getX() - 4, (int) p.getY() - 4, 8, 8);
                g.setColor(new Color(0, 0, 0, 90));
                g.drawOval((int) p.getX() - 4, (int) p.getY() - 4, 8, 8);
            }

            // red flash when a life is lost
            if (damageFlash > 0) {
                int alpha = (int) (150 * Math.min(1.0, damageFlash / 0.4));
                g.setColor(new Color(235, 40, 40, alpha));
                g.setStroke(new BasicStroke(10));
                g.drawRect(0, 0, getWidth(), getHeight());
                g.setStroke(new BasicStroke(1));
            }

            drawToast(g);

            if (board.isGameOver()) {
                g.setColor(new Color(10, 12, 20, 170));
                g.fillRect(0, 0, getWidth(), getHeight());
            } else if (paused) {
                g.setColor(new Color(10, 12, 20, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.TEXT);
                g.setFont(Theme.font(Font.BOLD, 44));
                String msg = "PAUSED";
                g.drawString(msg, (getWidth() - g.getFontMetrics().stringWidth(msg)) / 2, getHeight() / 2);
                g.setFont(Theme.font(Font.PLAIN, 15));
                g.setColor(Theme.MUTED);
                String sub = "Press P or click Resume";
                g.drawString(sub, (getWidth() - g.getFontMetrics().stringWidth(sub)) / 2, getHeight() / 2 + 30);
            }
        }

        private void drawTiles(Graphics2D g, int cell) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                for (int r = 0; r < GameBoard.ROWS; r++) {
                    g.setColor(board.isPathCell(c, r) ? PATH_COLOR : ((c + r) % 2 == 0 ? GRASS : GRASS_DARK));
                    g.fillRect(c * cell, r * cell, cell, cell);
                }
            }

            // small bushes
            g.setColor(new Color(70, 120, 64));
            for (int[] d : decorations) {
                g.fillOval(d[0], d[1], d[2] * 2, d[2] * 2);
            }

            // shaded edges along the path
            g.setColor(PATH_EDGE);
            for (int c = 0; c < GameBoard.COLS; c++) {
                for (int r = 0; r < GameBoard.ROWS; r++) {
                    if (!board.isPathCell(c, r)) continue;
                    int x = c * cell;
                    int y = r * cell;
                    if (r > 0 && !board.isPathCell(c, r - 1)) g.fillRect(x, y, cell, 3);
                    if (r < GameBoard.ROWS - 1 && !board.isPathCell(c, r + 1)) g.fillRect(x, y + cell - 3, cell, 3);
                    if (c > 0 && !board.isPathCell(c - 1, r)) g.fillRect(x, y, 3, cell);
                    if (c < GameBoard.COLS - 1 && !board.isPathCell(c + 1, r)) g.fillRect(x + cell - 3, y, 3, cell);
                }
            }
        }

        // START and BASE labels at the two ends of the path
        private void drawMarkers(Graphics2D g, int cell) {
            List<Point> path = board.getPath();
            Point start = path.get(0);
            Point end = path.get(path.size() - 1);
            drawMarker(g, start, "START", new Color(70, 180, 110, 150), cell);
            drawMarker(g, end, "BASE", new Color(225, 80, 80, 150), cell);
        }

        private void drawMarker(Graphics2D g, Point p, String text, Color color, int cell) {
            g.setColor(color);
            g.fillRect(p.x * cell, p.y * cell, cell, cell);
            g.setFont(Theme.font(Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(text, p.x * cell + (cell - fm.stringWidth(text)) / 2, p.y * cell + cell / 2 + 4);
        }

        private void drawTowers(Graphics2D g) {
            for (Tower t : board.getTowers()) {
                int cx = (int) t.getX();
                int cy = (int) t.getY();
                Color tc = t.getColor();

                g.setColor(new Color(0, 0, 0, 55));
                g.fillOval(cx - 18, cy - 15, 38, 38);
                g.setColor(new Color(52, 58, 74));
                g.fillOval(cx - 19, cy - 19, 38, 38);
                g.setColor(tc);
                g.fillOval(cx - 15, cy - 15, 30, 30);

                // barrel pointing at the current target
                double a = t.getAimAngle();
                g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(tc.darker().darker());
                g.drawLine(cx, cy, (int) (cx + Math.cos(a) * 19), (int) (cy + Math.sin(a) * 19));
                g.setStroke(new BasicStroke(1));
                g.setColor(tc.darker());
                g.fillOval(cx - 9, cy - 9, 18, 18);
                g.setColor(new Color(255, 255, 255, 80));
                g.fillOval(cx - 5, cy - 6, 6, 5);

                // level pips
                int pipsWidth = t.getLevel() * 6 + (t.getLevel() - 1) * 2;
                int px = cx - pipsWidth / 2;
                for (int i = 0; i < t.getLevel(); i++) {
                    g.setColor(Theme.ACCENT);
                    g.fillOval(px + i * 8, cy + 14, 6, 6);
                    g.setColor(new Color(0, 0, 0, 100));
                    g.drawOval(px + i * 8, cy + 14, 6, 6);
                }

                if (t.getCol() == selectedCol && t.getRow() == selectedRow) {
                    drawRange(g, t);
                    g.setColor(Theme.ACCENT);
                    g.setStroke(new BasicStroke(3));
                    g.drawOval(cx - 22, cy - 22, 44, 44);
                    g.setStroke(new BasicStroke(1));
                }
            }
        }

        private void drawEnemies(Graphics2D g) {
            for (Enemy e : board.getEnemies()) {
                int r = e.getRadius();
                int ex = (int) e.getX();
                int ey = (int) e.getY();

                g.setColor(new Color(0, 0, 0, 55));
                g.fillOval(ex - r, ey - r + 4, r * 2, r * 2);

                g.setColor(e.isSlowed() ? new Color(120, 200, 240) : e.getColor());
                g.fill(new Ellipse2D.Double(ex - r, ey - r, r * 2, r * 2));
                g.setColor(new Color(0, 0, 0, 160));
                g.drawOval(ex - r, ey - r, r * 2, r * 2);
                g.setColor(new Color(255, 255, 255, 90));
                g.fillOval(ex - r / 2, ey - r / 2 - 1, r / 2, r / 3);

                // health bar
                int barWidth = 26;
                int barY = ey - r - 9;
                double pct = Math.max(0, e.getHealth() / e.getMaxHealth());
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(ex - barWidth / 2 - 1, barY - 1, barWidth + 2, 6, 4, 4);
                g.setColor(pct > 0.5 ? Theme.GOOD : pct > 0.25 ? Theme.ACCENT : Theme.BAD);
                g.fillRoundRect(ex - barWidth / 2, barY, (int) (barWidth * pct), 4, 3, 3);
            }
        }

        private void drawToast(Graphics2D g) {
            if (toastTime <= 0) return;
            double alphaFactor = Math.min(1.0, toastTime / 0.4);
            g.setFont(Theme.font(Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(toastText) + 40;
            int h = 36;
            int x = (getWidth() - w) / 2;
            int y = 14;
            g.setColor(new Color(20, 24, 34, (int) (225 * alphaFactor)));
            g.fillRoundRect(x, y, w, h, 18, 18);
            g.setColor(new Color(toastColor.getRed(), toastColor.getGreen(), toastColor.getBlue(), (int) (255 * alphaFactor)));
            g.drawRoundRect(x, y, w, h, 18, 18);
            g.drawString(toastText, x + 20, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        }

        private void drawRange(Graphics2D g, Tower t) {
            int rad = (int) t.getRange();
            int cx = (int) t.getX();
            int cy = (int) t.getY();
            g.setColor(new Color(255, 255, 255, 45));
            g.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
            g.setColor(new Color(255, 255, 255, 140));
            g.drawOval(cx - rad, cy - rad, rad * 2, rad * 2);
        }
    }
}
