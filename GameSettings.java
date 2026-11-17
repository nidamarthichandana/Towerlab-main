// Simple class to store all the game settings
// Like player name, difficulty, starting money, etc.
public class GameSettings {
    public static final String EASY = "Easy";
    public static final String NORMAL = "Normal";
    public static final String HARD = "Hard";

    private String playerName;
    private String difficulty;
    private boolean autoStartWaves;

    public GameSettings() {
        // Default values
        playerName = "Commander";
        difficulty = NORMAL;
        autoStartWaves = false;
    }

    // Getters and Setters
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isAutoStartWaves() {
        return autoStartWaves;
    }

    public void setAutoStartWaves(boolean autoStart) {
        this.autoStartWaves = autoStart;
    }

    // Values that depend on the difficulty
    public int getStartingMoney() {
        if (difficulty.equals(EASY)) return 250;
        if (difficulty.equals(HARD)) return 120;
        return 175;
    }

    public int getStartingLives() {
        if (difficulty.equals(EASY)) return 30;
        if (difficulty.equals(HARD)) return 10;
        return 20;
    }

    // Enemies get tougher on harder difficulties
    public double getEnemyHealthMultiplier() {
        if (difficulty.equals(EASY)) return 0.8;
        if (difficulty.equals(HARD)) return 1.4;
        return 1.0;
    }
}
