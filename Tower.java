import java.awt.*;
import java.util.List;

// Base class for all towers. Each tower type extends this and changes
// its stats and what happens when a shot lands (polymorphism).
public abstract class Tower {
    protected final int col;
    protected final int row;
    protected final String name;
    protected final int cost;
    protected double range;      // in pixels
    protected double damage;
    protected double cooldown;   // seconds between shots
    protected double projectileSpeed = 360;
    protected int level = 1;
    private double timeUntilShot = 0;
    private double aimAngle = -Math.PI / 2;   // direction the barrel points
    private int totalSpent;

    public Tower(String name, int col, int row, int cost) {
        this.name = name;
        this.col = col;
        this.row = row;
        this.cost = cost;
        this.totalSpent = cost;
    }

    public abstract Color getColor();

    // Called when this tower's projectile hits an enemy
    public void onHit(Enemy target, List<Enemy> allEnemies) {
        target.takeDamage(damage);
    }

    // Find a target, aim at it and shoot if ready
    public void update(double dt, List<Enemy> enemies, List<Projectile> projectiles) {
        timeUntilShot -= dt;

        Enemy best = null;
        for (Enemy e : enemies) {
            if (e.isDead()) continue;
            double dx = e.getX() - getX();
            double dy = e.getY() - getY();
            if (Math.sqrt(dx * dx + dy * dy) <= range) {
                // target whichever enemy is furthest along the path
                if (best == null || e.getProgress() > best.getProgress()) {
                    best = e;
                }
            }
        }

        if (best == null) return;
        aimAngle = Math.atan2(best.getY() - getY(), best.getX() - getX());

        if (timeUntilShot <= 0) {
            projectiles.add(new Projectile(getX(), getY(), best, this, projectileSpeed));
            timeUntilShot = cooldown;
        }
    }

    public int getUpgradeCost() {
        return cost * level;
    }

    public boolean canUpgrade() {
        return level < 3;
    }

    public void upgrade() {
        totalSpent += getUpgradeCost();
        level++;
        damage *= 1.5;
        range *= 1.1;
        cooldown *= 0.9;
    }

    // Selling gives back 60% of everything spent
    public int getSellValue() {
        return (int) (totalSpent * 0.6);
    }

    public double getX() { return GameBoard.cellCenter(col); }
    public double getY() { return GameBoard.cellCenter(row); }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public double getRange() { return range; }
    public double getDamage() { return damage; }
    public double getCooldown() { return cooldown; }
    public double getAimAngle() { return aimAngle; }
    public int getLevel() { return level; }
    public int getCost() { return cost; }
    public String getName() { return name; }
}

// Cheap all-rounder
class BasicTower extends Tower {
    public static final int COST = 50;

    public BasicTower(int col, int row) {
        super("Basic", col, row, COST);
        range = 120;
        damage = 14;
        cooldown = 0.55;
    }

    public Color getColor() { return new Color(60, 130, 220); }
}

// Long range, slow, hard hitting
class SniperTower extends Tower {
    public static final int COST = 110;

    public SniperTower(int col, int row) {
        super("Sniper", col, row, COST);
        range = 240;
        damage = 70;
        cooldown = 1.8;
        projectileSpeed = 700;
    }

    public Color getColor() { return new Color(40, 150, 90); }
}

// Slows enemies down instead of doing much damage
class FrostTower extends Tower {
    public static final int COST = 80;

    public FrostTower(int col, int row) {
        super("Frost", col, row, COST);
        range = 110;
        damage = 4;
        cooldown = 0.7;
    }

    @Override
    public void onHit(Enemy target, List<Enemy> allEnemies) {
        target.takeDamage(damage);
        target.slow(Math.max(0.25, 0.55 - 0.1 * level), 2.0);
    }

    public Color getColor() { return new Color(110, 210, 235); }
}

// Splash damage - hits everything near the target
class CannonTower extends Tower {
    public static final int COST = 130;
    private static final double SPLASH_RADIUS = 55;

    public CannonTower(int col, int row) {
        super("Cannon", col, row, COST);
        range = 130;
        damage = 28;
        cooldown = 1.4;
        projectileSpeed = 260;
    }

    @Override
    public void onHit(Enemy target, List<Enemy> allEnemies) {
        for (Enemy e : allEnemies) {
            double dx = e.getX() - target.getX();
            double dy = e.getY() - target.getY();
            if (Math.sqrt(dx * dx + dy * dy) <= SPLASH_RADIUS) {
                e.takeDamage(damage);
            }
        }
    }

    public Color getColor() { return new Color(230, 120, 40); }
}
