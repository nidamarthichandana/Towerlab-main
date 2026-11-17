import java.awt.*;
import java.util.List;

// An enemy that walks along the path from start to end
public class Enemy {
    private final String type;
    private final double maxHealth;
    private double health;
    private final double baseSpeed;   // pixels per second
    private final int reward;
    private final Color color;
    private final int radius;

    private double x, y;
    private int nextWaypoint = 1;
    private double slowTimeLeft = 0;
    private double slowFactor = 1.0;
    private boolean reachedEnd = false;
    private double travelled = 0;   // total distance walked so far

    // type is "Grunt", "Runner" or "Brute"
    public Enemy(String type, int wave, double healthMultiplier, List<Point> path) {
        this.type = type;
        double waveBonus = 1.0 + (wave - 1) * 0.18;

        if (type.equals("Runner")) {
            maxHealth = 30 * waveBonus * healthMultiplier;
            baseSpeed = 110;
            reward = 6;
            color = new Color(255, 190, 60);
            radius = 9;
        } else if (type.equals("Brute")) {
            maxHealth = 160 * waveBonus * healthMultiplier;
            baseSpeed = 42;
            reward = 20;
            color = new Color(150, 60, 170);
            radius = 15;
        } else { // Grunt
            maxHealth = 55 * waveBonus * healthMultiplier;
            baseSpeed = 65;
            reward = 8;
            color = new Color(210, 70, 70);
            radius = 11;
        }
        health = maxHealth;

        // start at the first waypoint
        Point start = path.get(0);
        x = GameBoard.cellCenter(start.x);
        y = GameBoard.cellCenter(start.y);
    }

    // Move along the path
    public void update(double dt, List<Point> path) {
        if (slowTimeLeft > 0) {
            slowTimeLeft -= dt;
            if (slowTimeLeft <= 0) slowFactor = 1.0;
        }

        double distanceToMove = baseSpeed * slowFactor * dt;
        while (distanceToMove > 0 && !reachedEnd) {
            Point target = path.get(nextWaypoint);
            double tx = GameBoard.cellCenter(target.x);
            double ty = GameBoard.cellCenter(target.y);
            double dx = tx - x;
            double dy = ty - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist <= distanceToMove) {
                // reached this waypoint, go to the next one
                x = tx;
                y = ty;
                travelled += dist;
                distanceToMove -= dist;
                nextWaypoint++;
                if (nextWaypoint >= path.size()) {
                    reachedEnd = true;
                }
            } else {
                x += dx / dist * distanceToMove;
                y += dy / dist * distanceToMove;
                travelled += distanceToMove;
                distanceToMove = 0;
            }
        }
    }

    public void takeDamage(double amount) {
        health -= amount;
    }

    // Frost towers call this to slow the enemy down
    public void slow(double factor, double seconds) {
        // keep the strongest slow
        if (factor <= slowFactor || slowTimeLeft <= 0) {
            slowFactor = factor;
        }
        slowTimeLeft = Math.max(slowTimeLeft, seconds);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean hasReachedEnd() {
        return reachedEnd;
    }

    public boolean isSlowed() {
        return slowTimeLeft > 0;
    }

    // How far along the path the enemy is (used so towers shoot the leader)
    public double getProgress() {
        return travelled;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public int getReward() { return reward; }
    public Color getColor() { return color; }
    public int getRadius() { return radius; }
    public String getType() { return type; }
}
