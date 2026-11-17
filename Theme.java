import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Shared look and feel for the whole game: colors, fonts and a few
// custom-drawn components so we don't get the plain default Swing look.
public class Theme {
    public static final Color BG = new Color(22, 26, 36);
    public static final Color PANEL = new Color(32, 38, 52);
    public static final Color CARD = new Color(43, 51, 70);
    public static final Color CARD_HOVER = new Color(55, 65, 90);
    public static final Color BORDER = new Color(70, 80, 105);
    public static final Color ACCENT = new Color(255, 176, 56);
    public static final Color ACCENT_DARK_TEXT = new Color(40, 28, 6);
    public static final Color TEXT = new Color(236, 240, 247);
    public static final Color MUTED = new Color(150, 160, 182);
    public static final Color GOOD = new Color(70, 180, 110);
    public static final Color BAD = new Color(225, 80, 80);
    public static final Color BLUE = new Color(70, 130, 220);

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static Color brighten(Color c, int amount) {
        return new Color(Math.min(255, c.getRed() + amount),
                Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }

    public static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t),
                (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    // A flat rounded button with hover and pressed states.
    // Text can have several lines separated by \n (first line is bigger).
    public static class RoundButton extends JButton {
        private Color base;
        private boolean hover = false;
        private boolean pressed = false;
        private final int radius = 12;

        public RoundButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);   // so keyboard shortcuts keep working
            setOpaque(false);
            setForeground(TEXT);
            setFont(font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }

                @Override
                public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }

                @Override
                public void mousePressed(MouseEvent e) { pressed = true; repaint(); }

                @Override
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        public void setBase(Color c) {
            this.base = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);

            Color c = base;
            if (!isEnabled()) c = blend(base, BG, 0.6);
            else if (pressed) c = base.darker();
            else if (hover) c = brighten(base, 22);

            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);

            Color fg = isEnabled() ? getForeground() : MUTED;
            String[] lines = getText().split("\n");
            Font main = getFont();
            Font small = font(Font.PLAIN, Math.max(10, main.getSize() - 3));

            int totalHeight = 0;
            for (int i = 0; i < lines.length; i++) {
                totalHeight += g.getFontMetrics(i == 0 ? main : small).getHeight();
            }
            int y = (getHeight() - totalHeight) / 2;
            for (int i = 0; i < lines.length; i++) {
                Font f = (i == 0) ? main : small;
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics();
                g.setColor(i == 0 ? fg : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 190));
                g.drawString(lines[i], (getWidth() - fm.stringWidth(lines[i])) / 2, y + fm.getAscent());
                y += fm.getHeight();
            }
            g.dispose();
        }
    }

    // A panel with rounded corners and an optional border
    public static class RoundPanel extends JPanel {
        private Color fill;
        private Color borderColor = null;
        private final int radius;

        public RoundPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        public void setFill(Color c) {
            this.fill = c;
            repaint();
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            smooth(g);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            if (borderColor != null) {
                g.setColor(borderColor);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius * 2, radius * 2);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
