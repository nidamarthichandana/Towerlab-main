import java.util.List;

// A shot flying from a tower to its target
public class Projectile {
    private double x, y;
    private final Enemy target;
    private final Tower owner;
    private final double speed;
    private boolean done = false;

    public Projectile(double x, double y, Enemy target, Tower owner, double speed) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.owner = owner;
        this.speed = speed;
    }

    public void update(double dt, List<Enemy> allEnemies) {
        // target already gone, so the shot just disappears
        if (target.isDead() || target.hasReachedEnd()) {
            done = true;
            return;
        }

        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double step = speed * dt;

        if (dist <= step) {
            owner.onHit(target, allEnemies);
            done = true;
        } else {
            x += dx / dist * step;
            y += dy / dist * step;
        }
    }

    public boolean isDone() { return done; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Tower getOwner() { return owner; }
}
