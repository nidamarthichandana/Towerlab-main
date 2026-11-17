import java.awt.Point;
import java.util.*;

// GameBoard class - stores the game state (grid, path, towers, enemies)
// and updates everything each tick. It knows nothing about drawing.
public class GameBoard {
    public static final int COLS = 15;
    public static final int ROWS = 10;
    public static final int CELL = 48;
    public static final int MAX_WAVES = 15;

    private final GameSettings settings;
    private final List<Point> path;            // corner points of the enemy path
    private final boolean[][] pathCells;       // true if a cell is part of the path
    private final Tower[][] towerGrid;

    private final List<Tower> towers = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final LinkedList<String> spawnQueue = new LinkedList<>();

    private int money;
    private int lives;
    private int wave = 0;
    private boolean waveActive = false;
    private double spawnTimer = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    private int score = 0;
    private int waveTotal = 0;        // enemies in the current wave
    private int enemiesKilled = 0;
    private int towersBuilt = 0;

    public GameBoard(GameSettings settings) {
        this.settings = settings;
        this.money = settings.getStartingMoney();
        this.lives = settings.getStartingLives();

        // Corner points of the snake-like path (col, row)
        path = new ArrayList<>();
        path.add(new Point(0, 1));
        path.add(new Point(12, 1));
        path.add(new Point(12, 4));
        path.add(new Point(2, 4));
        path.add(new Point(2, 7));
        path.add(new Point(14, 7));

        pathCells = new boolean[COLS][ROWS];
        towerGrid = new Tower[COLS][ROWS];
        markPathCells();
    }

    // Marks every cell between the corner points as path
    private void markPathCells() {
        for (int i = 0; i < path.size() - 1; i++) {
            Point a = path.get(i);
            Point b = path.get(i + 1);
            int dc = Integer.compare(b.x, a.x);
            int dr = Integer.compare(b.y, a.y);
            int c = a.x, r = a.y;
            pathCells[c][r] = true;
            while (c != b.x || r != b.y) {
                c += dc;
                r += dr;
                pathCells[c][r] = true;
            }
        }
    }

    public static double cellCenter(int index) {
        return index * CELL + CELL / 2.0;
    }

    // ---------- Building ----------

    public boolean canBuild(int col, int row) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return false;
        return !pathCells[col][row] && towerGrid[col][row] == null;
    }

    public static Tower createTower(String type, int col, int row) {
        switch (type) {
            case "Sniper": return new SniperTower(col, row);
            case "Frost":  return new FrostTower(col, row);
            case "Cannon": return new CannonTower(col, row);
            default:       return new BasicTower(col, row);
        }
    }

    public static int getTowerCost(String type) {
        switch (type) {
            case "Sniper": return SniperTower.COST;
            case "Frost":  return FrostTower.COST;
            case "Cannon": return CannonTower.COST;
            default:       return BasicTower.COST;
        }
    }

    // Returns true if the tower was placed
    public boolean placeTower(String type, int col, int row) {
        if (gameOver || !canBuild(col, row)) return false;
        int cost = getTowerCost(type);
        if (money < cost) return false;

        Tower t = createTower(type, col, row);
        towers.add(t);
        towerGrid[col][row] = t;
        money -= cost;
        towersBuilt++;
        return true;
    }

    public boolean upgradeTower(int col, int row) {
        Tower t = getTowerAt(col, row);
        if (t == null || !t.canUpgrade() || money < t.getUpgradeCost()) return false;
        money -= t.getUpgradeCost();
        t.upgrade();
        return true;
    }

    public boolean sellTower(int col, int row) {
        Tower t = getTowerAt(col, row);
        if (t == null) return false;
        money += t.getSellValue();
        towers.remove(t);
        towerGrid[col][row] = null;
        return true;
    }

    public Tower getTowerAt(int col, int row) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return null;
        return towerGrid[col][row];
    }

    // ---------- Waves ----------

    public void startWave() {
        if (waveActive || gameOver || wave >= MAX_WAVES) return;
        wave++;
        waveActive = true;

        // Work out which enemies are in this wave
        List<String> types = new ArrayList<>();
        for (int i = 0; i < 5 + wave * 2; i++) types.add("Grunt");
        if (wave >= 2) for (int i = 0; i < wave * 2; i++) types.add("Runner");
        if (wave >= 4) for (int i = 0; i < (wave - 2) / 2; i++) types.add("Brute");

        // shuffle with the wave number as seed so waves are repeatable
        Collections.shuffle(types, new Random(wave));
        spawnQueue.addAll(types);
        waveTotal = types.size();
        spawnTimer = 0;
    }

    // ---------- Game loop ----------

    // dt = seconds since last update
    public void update(double dt) {
        if (gameOver) return;

        // spawn enemies one at a time
        if (waveActive && !spawnQueue.isEmpty()) {
            spawnTimer -= dt;
            if (spawnTimer <= 0) {
                String type = spawnQueue.removeFirst();
                enemies.add(new Enemy(type, wave, settings.getEnemyHealthMultiplier(), path));
                spawnTimer = Math.max(0.35, 0.9 - wave * 0.04);
            }
        }

        for (Tower t : towers) {
            t.update(dt, enemies, projectiles);
        }

        for (Projectile p : projectiles) {
            p.update(dt, enemies);
        }
        projectiles.removeIf(Projectile::isDone);

        for (Enemy e : enemies) {
            e.update(dt, path);
        }

        // handle dead enemies and enemies that got through
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.isDead()) {
                money += e.getReward();
                score += e.getReward() * 10;
                enemiesKilled++;
                it.remove();
            } else if (e.hasReachedEnd()) {
                lives--;
                it.remove();
            }
        }

        if (lives <= 0) {
            lives = 0;
            gameOver = true;
            return;
        }

        // wave finished?
        if (waveActive && spawnQueue.isEmpty() && enemies.isEmpty()) {
            waveActive = false;
            money += 25 + wave * 5;   // bonus for clearing a wave
            if (wave >= MAX_WAVES) {
                victory = true;
                gameOver = true;
            }
        }
    }

    // ---------- Getters ----------
    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public List<Point> getPath() { return path; }
    public boolean isPathCell(int col, int row) { return pathCells[col][row]; }
    public int getMoney() { return money; }
    public int getLives() { return lives; }
    public int getWave() { return wave; }
    public int getScore() { return score; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public int getTowersBuilt() { return towersBuilt; }

    public int getEnemiesRemaining() {
        return spawnQueue.size() + enemies.size();
    }

    // 0.0 to 1.0 - how much of the current wave is finished
    public double getWaveProgress() {
        if (wave == 0) return 0;
        if (!waveActive) return 1.0;
        if (waveTotal == 0) return 1.0;
        return 1.0 - (double) getEnemiesRemaining() / waveTotal;
    }

    public boolean isWaveActive() { return waveActive; }
    public boolean isGameOver() { return gameOver; }
    public boolean isVictory() { return victory; }
}
