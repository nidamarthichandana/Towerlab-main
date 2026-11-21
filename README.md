# TowerLab - Java OOP Project

A tower defense game written in Java using Swing for the GUI, demonstrating Object-Oriented Programming principles.

## Project Overview

Enemies walk along a winding path and you stop them by building and upgrading towers on the grass tiles beside it. Survive all 15 waves to win. Every enemy that reaches the end of the path costs you a life.

## Features

- **Clean OOP Design**: Separate classes for game logic, GUI, towers, enemies and settings
- **Four Tower Types**: Basic, Sniper, Frost (slows enemies) and Cannon (splash damage)
- **Three Enemy Types**: Grunt, Runner (fast) and Brute (tough), scaling with each wave
- **Upgrades and Selling**: Upgrade towers up to level 3 or sell them for 60% of what you spent
- **Difficulty Levels**: Easy, Normal and Hard change starting money, lives and enemy health
- **Modern Dark UI**: Custom-drawn buttons, cards and HUD instead of default Swing widgets
- **Visual Feedback**:
  - Range preview when you hover a free tile, towers aim at their targets
  - Health bars on enemies, blue tint when slowed, wave progress bar
  - Pop-up messages ("Wave 3 incoming!", "Not enough money", wave cleared bonus)
  - Red flash when an enemy reaches your base
  - Victory / Defeated screen with final stats and Play Again button
- **Keyboard Shortcuts**: `1-4` pick a tower, `Space` start wave, `P` pause

## Object-Oriented Programming Concepts Demonstrated

### 1. **Encapsulation**
- `GameBoard` encapsulates all game state (towers, enemies, money, lives)
- `GameSettings` encapsulates game configuration
- Private fields with public getter/setter methods

### 2. **Abstraction**
- `Tower` is an abstract class that defines what every tower can do
- The board is a grid; path and build rules are hidden behind `canBuild()` and `placeTower()`

### 3. **Separation of Concerns**
- **Model**: `GameBoard`, `Tower`, `Enemy`, `Projectile` - game logic and state
- **View**: `GameGUI`, `MainMenu`, `Theme` - user interface
- **Controller**: Event handlers managing user interactions
- **Data**: `GameSettings` - configuration storage

### 4. **Inheritance**
- `BasicTower`, `SniperTower`, `FrostTower` and `CannonTower` extend `Tower`
- GUI classes extend `JFrame` from Swing

### 5. **Polymorphism**
- Each tower overrides `onHit()` (Frost slows, Cannon does splash damage) and `getColor()`
- The board treats every tower as a `Tower` without caring about its type

## Class Structure

```
TowerLab.java          - Main entry point
MainMenu.java          - Welcome screen and game configuration
GameGUI.java           - Game window, HUD, drawing, result screen and game loop
Theme.java             - Shared colors, fonts and custom rounded buttons/panels
GameBoard.java         - Game logic: grid, path, waves, building
Tower.java             - Abstract Tower plus Basic, Sniper, Frost and Cannon towers
Enemy.java             - Enemy movement, health and slowing
Projectile.java        - Shots flying from towers to enemies
GameSettings.java      - Game configuration storage
```

## How to Compile and Run

```bash
# Mac / Linux
javac *.java
java TowerLab
```

```bat
:: Windows (Command Prompt does not expand *.java)
javac TowerLab.java MainMenu.java GameGUI.java GameBoard.java Tower.java Enemy.java Projectile.java GameSettings.java Theme.java
java TowerLab
```

## How to Play

1. Run the application
2. Enter your name, choose a difficulty and click "Play"
3. Pick a tower type in the side panel and click an empty grass tile to build it
4. Click "Start Wave" to send the enemies
5. Click a built tower to upgrade or sell it
6. Earn money by killing enemies and clearing waves

## Towers

| Tower  | Cost | Role                                   |
|--------|------|----------------------------------------|
| Basic  | $50  | Cheap all-rounder                      |
| Sniper | $110 | Long range, slow and hard hitting      |
| Frost  | $80  | Low damage, slows enemies              |
| Cannon | $130 | Splash damage to groups                |

## Technologies Used

- **Java SE**: Core programming language
- **Swing**: GUI framework for the user interface
- **AWT**: Graphics and events (Graphics2D for drawing)

## Project Structure (OOP Principles)

- **Single Responsibility**: Each class has one clear purpose
- **Loose Coupling**: The model never touches the GUI
- **High Cohesion**: Related functionality grouped within classes
- **Information Hiding**: Internal implementation details kept private

## Future Enhancements

- More maps and a map selector
- Flying enemies and boss waves
- Targeting modes (first, strongest, closest)
- Sound effects and animations
- Save high scores

## License

This project is open source and available for educational purposes.
