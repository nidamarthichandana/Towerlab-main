import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;

// Main menu screen - where you set up the game before playing
public class MainMenu extends JFrame {
    private final GameSettings settings;
    private String difficulty = GameSettings.NORMAL;
    private boolean autoStart = false;

    private static final String[] DIFFICULTIES = {GameSettings.EASY, GameSettings.NORMAL, GameSettings.HARD};
    private final Theme.RoundButton[] difficultyButtons = new Theme.RoundButton[3];

    public MainMenu() {
        settings = new GameSettings();

        setTitle("TowerLab - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BackgroundPanel root = new BackgroundPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(34, 44, 26, 44));
        setContentPane(root);

        // ---- title ----
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleRow.setOpaque(false);
        titleRow.add(Theme.label("TOWER", Font.BOLD, 54, Theme.TEXT));
        titleRow.add(Theme.label("LAB", Font.BOLD, 54, Theme.ACCENT));
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        root.add(titleRow);

        JLabel subtitle = Theme.label("Build. Upgrade. Defend.", Font.PLAIN, 16, Theme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(24));

        // ---- settings card ----
        Theme.RoundPanel card = new Theme.RoundPanel(Theme.PANEL, 22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 26, 26, 26));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // player name
        card.add(sectionLabel("PLAYER NAME"));
        card.add(Box.createVerticalStrut(6));
        JTextField nameField = new JTextField(settings.getPlayerName());
        nameField.setFont(Theme.font(Font.PLAIN, 16));
        nameField.setBackground(Theme.CARD);
        nameField.setForeground(Theme.TEXT);
        nameField.setCaretColor(Theme.TEXT);
        nameField.setBorder(fieldBorder(Theme.BORDER));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        nameField.setPreferredSize(new Dimension(100, 42));
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { nameField.setBorder(fieldBorder(Theme.ACCENT)); }

            @Override
            public void focusLost(FocusEvent e) { nameField.setBorder(fieldBorder(Theme.BORDER)); }
        });
        card.add(nameField);
        card.add(Box.createVerticalStrut(20));

        // difficulty
        card.add(sectionLabel("DIFFICULTY"));
        card.add(Box.createVerticalStrut(6));
        JPanel diffRow = new JPanel(new GridLayout(1, 3, 10, 0));
        diffRow.setOpaque(false);
        diffRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        diffRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        diffRow.setPreferredSize(new Dimension(100, 64));
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            final String d = DIFFICULTIES[i];
            GameSettings tmp = new GameSettings();
            tmp.setDifficulty(d);
            Theme.RoundButton b = new Theme.RoundButton(
                    d + "\n" + tmp.getStartingLives() + " lives  \u00B7  $" + tmp.getStartingMoney(), Theme.CARD);
            b.setFont(Theme.font(Font.BOLD, 16));
            b.addActionListener(e -> {
                difficulty = d;
                refreshDifficulty();
            });
            difficultyButtons[i] = b;
            diffRow.add(b);
        }
        card.add(diffRow);
        card.add(Box.createVerticalStrut(14));
        refreshDifficulty();

        // auto start toggle
        Theme.RoundButton autoButton = new Theme.RoundButton("Auto-start waves: OFF", Theme.CARD);
        autoButton.setFont(Theme.font(Font.BOLD, 13));
        autoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        autoButton.setPreferredSize(new Dimension(100, 40));
        autoButton.addActionListener(e -> {
            autoStart = !autoStart;
            autoButton.setText("Auto-start waves: " + (autoStart ? "ON" : "OFF"));
            autoButton.setBase(autoStart ? Theme.GOOD : Theme.CARD);
        });
        card.add(autoButton);
        card.add(Box.createVerticalStrut(24));

        // play button
        Theme.RoundButton playButton = new Theme.RoundButton("PLAY", Theme.ACCENT);
        playButton.setFont(Theme.font(Font.BOLD, 22));
        playButton.setForeground(Theme.ACCENT_DARK_TEXT);
        playButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        playButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        playButton.setPreferredSize(new Dimension(100, 56));
        playButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            settings.setPlayerName(name.isEmpty() ? "Commander" : name);
            settings.setDifficulty(difficulty);
            settings.setAutoStartWaves(autoStart);
            dispose();
            new GameGUI(settings);
        });
        card.add(playButton);
        card.add(Box.createVerticalStrut(10));

        // how to play button
        Theme.RoundButton helpButton = new Theme.RoundButton("How to Play", Theme.CARD);
        helpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        helpButton.setPreferredSize(new Dimension(100, 40));
        helpButton.addActionListener(e -> showHelp());
        card.add(helpButton);

        root.add(card);
        root.add(Box.createVerticalStrut(14));

        JLabel footer = Theme.label(GameBoard.MAX_WAVES + " waves  \u00B7  4 towers  \u00B7  3 enemy types",
                Font.PLAIN, 12, Theme.MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(footer);

        setSize(540, 650);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = Theme.label(text, Font.BOLD, 11, Theme.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private javax.swing.border.Border fieldBorder(Color line) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line, 2),
                BorderFactory.createEmptyBorder(4, 12, 4, 12));
    }

    // Colors the selected difficulty button
    private void refreshDifficulty() {
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            Color selectedColor = DIFFICULTIES[i].equals(GameSettings.EASY) ? Theme.GOOD
                    : DIFFICULTIES[i].equals(GameSettings.HARD) ? Theme.BAD : Theme.BLUE;
            difficultyButtons[i].setBase(DIFFICULTIES[i].equals(difficulty) ? selectedColor : Theme.CARD);
        }
    }

    private void showHelp() {
        JDialog dialog = new JDialog(this, "How to Play", true);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        String accent = "#FFB038";
        JLabel text = new JLabel("<html><div style='width:340px'>"
                + "<b style='color:" + accent + "'>GOAL</b><br>"
                + "Stop the enemies before they reach your base. Survive all "
                + GameBoard.MAX_WAVES + " waves to win.<br><br>"
                + "<b style='color:" + accent + "'>HOW TO PLAY</b><br>"
                + "1. Pick a tower on the right (or press 1-4).<br>"
                + "2. Click a grass tile to build it.<br>"
                + "3. Press Start Wave (or Space) to send enemies.<br>"
                + "4. Click a tower to upgrade or sell it.<br><br>"
                + "<b style='color:" + accent + "'>TOWERS</b><br>"
                + "Basic - balanced and cheap<br>"
                + "Sniper - long range, huge damage, slow<br>"
                + "Frost - slows enemies down<br>"
                + "Cannon - splash damage to groups<br><br>"
                + "<b style='color:" + accent + "'>SHORTCUTS</b><br>"
                + "1-4 select tower  \u00B7  Space start wave  \u00B7  P pause"
                + "</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 14));
        text.setForeground(Theme.TEXT);

        Theme.RoundPanel card = new Theme.RoundPanel(Theme.PANEL, 18);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(text, BorderLayout.CENTER);
        content.add(card, BorderLayout.CENTER);

        Theme.RoundButton ok = new Theme.RoundButton("Got it", Theme.ACCENT);
        ok.setForeground(Theme.ACCENT_DARK_TEXT);
        ok.setPreferredSize(new Dimension(100, 40));
        ok.addActionListener(e -> dialog.dispose());
        content.add(ok, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Dark gradient background with a faint winding path drawn on it
    private static class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setPaint(new GradientPaint(0, 0, new Color(30, 37, 56), 0, getHeight(), new Color(14, 17, 26)));
            g.fillRect(0, 0, getWidth(), getHeight());

            int w = getWidth();
            int h = getHeight();
            Path2D path = new Path2D.Double();
            path.moveTo(-20, h * 0.30);
            path.lineTo(w * 0.85, h * 0.30);
            path.lineTo(w * 0.85, h * 0.52);
            path.lineTo(w * 0.12, h * 0.52);
            path.lineTo(w * 0.12, h * 0.80);
            path.lineTo(w + 20, h * 0.80);
            g.setColor(new Color(255, 255, 255, 12));
            g.setStroke(new BasicStroke(46, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);

            g.setColor(new Color(255, 176, 56, 22));
            g.fillOval((int) (w * 0.80), (int) (h * 0.86), 54, 54);
            g.setColor(new Color(70, 130, 220, 24));
            g.fillOval((int) (w * 0.05), (int) (h * 0.62), 54, 54);
            g.dispose();
        }
    }
}
