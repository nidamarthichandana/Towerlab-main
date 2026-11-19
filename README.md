TowerLab

A tower defense game written in Java (Swing/AWT). Enemies walk along a winding path and the player stops them by building and upgrading towers on the grass tiles beside it. Survive all 15 waves to win; every enemy that reaches the end costs one life.

TowerLab is both a playable game and a small software engineering case study: this document covers not only how to run it, but also how it was specified, designed, tested and how it should evolve.

	
Language / runtime	Java SE 8 or later (no external dependencies)
UI toolkit	Swing + AWT (Graphics2D)
Size	9 source files, about 80 KB, flat default package
Process model	Iterative and Incremental, with light Agile practices
Status	Experimental (see catalog-info.yaml)
Table of contents
Quick start
Development process (SDLC)
Requirements
Architecture and design
Game rules and balance data
Implementation notes
Testing and quality assurance
Maintenance and evolution
Risks
Repository layout
Contributing
1. Quick start

Prerequisites: a JDK (8+) with a display (Swing needs a GUI environment).

bash
# Mac / Linux
javac *.java
java TowerLab
bat
:: Windows (Command Prompt does not expand *.java)
javac TowerLab.java MainMenu.java GameGUI.java GameBoard.java Tower.java Enemy.java Projectile.java GameSettings.java Theme.java
java TowerLab

How to play

Enter your name, choose a difficulty and click Play.
Pick a tower in the side panel and click a free grass tile to build it.
Click Start Wave to send the enemies.
Click a built tower to upgrade (up to level 3) or sell it.
Earn money by killing enemies and clearing waves.
Key	Action
1-4	Select Basic / Sniper / Frost / Cannon
Space	Start the next wave
P	Pause / resume
2. Development process (SDLC)
Chosen model: Iterative and Incremental

TowerLab is developed in small working slices. Each slice passes through a mini lifecycle and ends with a playable build; feedback from playtesting shapes the next slice.

 Plan & Requirements -> Design -> Implement -> Test -> Review & Feedback
          ^                                                    |
          +----------------  next iteration  -----------------+

Why this model fits

Requirements are clear in outline (towers vs. waves) but numbers such as cost, range, damage and enemy health can only be tuned by playing.
The product is naturally incremental: a playable core first, then more towers, enemies, UI and options.
The architecture supports it: new towers and enemies are added by extension, so each increment is low-risk.
It is a small-team project with no contractual scope, so heavyweight plan-driven models (Waterfall, V-Model, Spiral) add overhead without value.
Iteration plan
#	Increment	Main classes	Review focus
1	Core: 15 x 10 grid, snake path, enemy movement, one shooting tower	GameBoard, Enemy, Tower, Projectile	Enemies follow the path; shots hit; a leak costs a life
2	Towers: Sniper, Frost, Cannon; upgrades; selling	Tower and subclasses	Cost and damage balance; upgrade and refund maths
3	Waves: 3 enemy types, 15 scaling waves, rewards, win/lose	GameBoard, Enemy	Difficulty curve; victory at wave 15
4	UI and menu: main menu, difficulty, HUD, hover preview, shortcuts, result screen	MainMenu, GameGUI, Theme, GameSettings	Playable without instructions
5	Extras (planned): maps, flying enemies, targeting modes, sound, high scores	new classes	Regression plus playtest feedback
Working practices
Short iterations, each ending in a runnable build.
A simple backlog (the roadmap in section 8).
Version control with one commit or branch per feature.
Playtesting acts as acceptance testing, because balance can only be judged by playing.
Refactor as you go to protect the separation between model, view and settings.
3. Requirements
3.1 Stakeholders
Stakeholder	Interest
Player	An understandable, fair and fun game
Student / instructor	Clear demonstration of OOP principles
Developer / maintainer	Code that is easy to extend and test
3.2 Functional requirements
ID	Requirement	Priority
FR-01	The game shall display a 15 x 10 grid with a fixed enemy path.	Must
FR-02	The player shall build a tower on any free, non-path tile if they can afford it.	Must
FR-03	The game shall offer four tower types: Basic, Sniper, Frost, Cannon.	Must
FR-04	Towers shall automatically target the enemy furthest along the path within range.	Must
FR-05	The player shall upgrade a tower up to level 3 for a cost, and sell it for 60% of the total spent.	Must
FR-06	The game shall run 15 waves of Grunt, Runner and Brute enemies that scale with the wave number.	Must
FR-07	Each enemy that reaches the path end shall cost one life; at zero lives the game is lost.	Must
FR-08	Clearing wave 15 shall end the game in victory.	Must
FR-09	Killing enemies shall award money and score; clearing a wave shall award a bonus.	Must
FR-10	The player shall pick a difficulty (Easy, Normal, Hard) that changes starting money, lives and enemy health.	Should
FR-11	The player shall enter a name; the result screen shall show final stats and a Play Again option.	Should
FR-12	The game shall support pause and keyboard shortcuts (1-4, Space, P).	Should
FR-13	The UI shall give feedback: range preview, health bars, slowed tint, pop-up messages, wave progress bar, red flash on a leak.	Should
FR-14	Waves shall optionally start automatically after a short delay (auto-start setting).	Could
3.3 Non-functional requirements
ID	Category	Requirement
NFR-01	Performance	Run smoothly at about 60 FPS (16 ms timer); simulation time step capped at 0.05 s so a frozen window cannot cause huge jumps.
NFR-02	Portability	Run on any OS with Java SE 8+ and no third-party libraries.
NFR-03	Maintainability	Game logic shall not depend on Swing; new tower or enemy types shall be addable without changing existing tower classes.
NFR-04	Usability	Playable without a manual: on-screen hints, hover preview and error pop-ups ("Not enough money").
NFR-05	Testability	Game logic shall be deterministic (wave order is seeded by wave number) and runnable without a display.
NFR-06	Readability	Small, cohesive classes with comments explaining intent.
3.4 Constraints, assumptions and out of scope
Constraints: Java and Swing only; single-player desktop; no network or database.
Assumptions: a screen of at least 720 x 480 px for the board plus side panel.
Out of scope (for now): multiplayer, save/load, sound, multiple maps (see roadmap).
4. Architecture and design
4.1 Architectural style

A layered Model / View / Controller-style split with a single-threaded game loop.

Layer	Classes	Responsibility
Model	GameBoard, Tower (+4 subclasses), Enemy, Projectile	All game state and rules; no drawing
View	GameGUI, MainMenu, Theme	Rendering, custom widgets, HUD
Controller	Event handlers and key bindings in GameGUI / MainMenu	Translate input into model calls
Configuration	GameSettings	Player name, difficulty and values derived from it
Entry point	TowerLab	Sets look and feel, opens MainMenu on the Swing thread
4.2 Class diagram
target
owner
TowerLab
MainMenu
GameGUI
Theme
GameSettings
GameBoard
«abstract»
Tower
+onHit(target, enemies)
+update(dt, enemies, projectiles)
+upgrade()
+getSellValue()
BasicTower
SniperTower
FrostTower
CannonTower
Enemy
Projectile
4.3 Game loop (runtime behaviour)

GameGUI runs a javax.swing.Timer every 16 ms. Each tick it measures elapsed time, caps it at 0.05 s, and (unless paused) calls board.update(dt), then repaints.

Inside GameBoard.update(dt) the order of work is fixed:

Spawn the next queued enemy if the spawn timer has elapsed.
Update towers (acquire a target, aim, fire a Projectile if off cooldown).
Update projectiles (move toward the target; on arrival call the owner's onHit).
Update enemies (move along the path, tick slow timers).
Resolve deaths (reward money and score) and leaks (lose a life).
If lives reach 0, set game over. If the wave is fully spawned and no enemies remain, award the clear bonus and, on wave 15, set victory.
4.4 Design principles and patterns in use
Principle / pattern	Where	Benefit
Encapsulation	GameBoard owns all state behind getters and action methods (placeTower, upgradeTower, sellTower)	Rules cannot be bypassed by the UI
Abstraction	Tower defines what every tower can do; path and build rules hide behind canBuild()	Callers do not care about tower specifics
Inheritance + polymorphism	Four tower subclasses override stats and onHit() (Frost slows, Cannon splashes)	New behaviour without if/else in the board
Template-method style hook	Tower.update() handles targeting and firing; subclasses customise only onHit()	Shared logic written once
Simple factory	GameBoard.createTower(type, col, row)	One place that maps a type name to a class
Separation of concerns	Model never references Swing components	Logic can be tested headless
Single responsibility	One focused class per concept	Easier to read and change
4.5 Key design decisions
Decision	Rationale	Trade-off
Single-threaded Swing Timer loop	Simple; no synchronisation bugs	Heavy logic would block the UI
Delta-time movement with a 0.05 s cap	Frame-rate independent; safe after freezes	Very low FPS slows game time slightly
Path stored as corner waypoints, expanded to cells	Compact definition; easy to add maps	Only axis-aligned segments supported
Wave contents shuffled with Random(wave)	Repeatable waves, so bugs and balance are reproducible	Waves never vary between plays
Towers always target the enemy furthest along	Predictable, matches player intuition	No player choice (planned targeting modes)
Tower and enemy types identified by String	Quick to write	Typos fail silently; see tech debt
5. Game rules and balance data

All values below are taken from the source and are the starting point for balance tuning.

5.1 Towers
Tower	Cost	Range (px)	Damage	Cooldown (s)	Projectile speed	Special
Basic	$50	120	14	0.55	360	Cheap all-rounder
Sniper	$110	240	70	1.8	700	Long range, hard hitting
Frost	$80	110	4	0.7	360	Slows target for 2 s
Cannon	$130	130	28	1.4	260	Splash radius 55 px

Upgrades (max level 3): cost to upgrade = base cost x current level; each level gives damage x 1.5, range x 1.1 and cooldown x 0.9. Selling returns 60% (rounded down) of all money spent on that tower. Frost slow strength improves with level (speed multiplier 0.45, 0.35, 0.25).

5.2 Enemies
Enemy	Base health	Speed (px/s)	Reward
Grunt	55	65	$8
Runner	30	110	$6
Brute	160	42	$20

Health scales by (1 + 0.18 x (wave - 1)) x difficulty multiplier.

5.3 Waves
Grunts: 5 + 2 x wave
Runners: 2 x wave (from wave 2)
Brutes: (wave - 2) / 2, integer division (from wave 4)
Spawn interval: max(0.35, 0.9 - 0.04 x wave) seconds
Clear bonus: $25 + 5 x wave; score per kill: reward x 10

Example: wave 1 has 7 enemies; wave 15 has 35 Grunts, 30 Runners and 6 Brutes.

5.4 Difficulty
Difficulty	Starting money	Lives	Enemy health
Easy	$250	30	x 0.8
Normal	$175	20	x 1.0
Hard	$120	10	x 1.4
5.5 Map

The grid is 15 columns x 10 rows of 48 px cells. Path corners (col, row): (0,1) -> (12,1) -> (12,4) -> (2,4) -> (2,7) -> (14,7).

6. Implementation notes
Language level: plain Java, no build tool and no dependencies (compile with javac).
Package: all classes are in the default package. Move them to a named package (for example towerlab.model, towerlab.ui) before adding a build tool.
Coding conventions: one top-level concept per file (the four tower subclasses live beside Tower as package-private classes); private fields with getters; comments describe intent rather than restate code.
Ignored files: .gitignore excludes *.class, IDE folders, logs and archives.
Documentation site: mkdocs.yml plus docs/ and catalog-info.yaml (Backstage TechDocs) publish this documentation.
7. Testing and quality assurance
7.1 Current state

The repository contains no automated tests; quality currently relies on manual playtesting. The model layer is separated from Swing and its wave order is deterministic, so it is well suited to unit testing. Adding tests is the highest-value engineering improvement (see roadmap).

7.2 Test strategy
Level	Scope	Approach
Unit	Tower, Enemy, Projectile, GameSettings	JUnit 5, no display needed
Integration	GameBoard with towers, enemies and waves	Drive update(dt) in a loop and assert on state
System / acceptance	Full game via the GUI	Manual playtest checklist below
Regression	All of the above	Run before every merge
7.3 Manual test cases (traceable to requirements)
ID	Req.	Steps	Expected result
TC-01	FR-02	Normal difficulty: build a Basic tower on a grass tile	Tower appears; money goes from $175 to $125
TC-02	FR-02	Try to build on a path tile	Build refused; money unchanged
TC-03	FR-02	Try to build with insufficient money	"Not enough money" message; no tower
TC-04	FR-02	Try to build on an occupied tile	Build refused
TC-05	FR-05	Upgrade a Basic tower to level 2	Costs $50; damage 14 to 21
TC-06	FR-05	Upgrade a tower to level 3, then try again	Second attempt refused (max level)
TC-07	FR-05	Build Basic ($50), upgrade ($50), sell	Refund is $60 (60% of $100)
TC-08	FR-04	Let several enemies be in range of a tower at once	Tower shoots the one furthest along the path
TC-09	FR-03	Hit enemies with a Frost tower	Enemy turns blue and moves slower for 2 s
TC-10	FR-03	Hit a group with a Cannon tower	Enemies within 55 px of the target all take damage
TC-11	FR-06	Start wave 1	7 Grunts spawn; no Runners or Brutes
TC-12	FR-07	Let an enemy reach the end	Lives drop by 1; red flash shown
TC-13	FR-07	Lose all lives	Defeated screen with stats and Play Again
TC-14	FR-08	Clear wave 15	Victory screen
TC-15	FR-09	Kill a Brute, then clear a wave	+$20 reward; clear bonus of $25 + 5 x wave
TC-16	FR-10	Start on Easy, Normal and Hard	Money, lives and enemy health match section 5.4
TC-17	FR-12	Press 1-4, Space, P	Tower selected / wave starts / game pauses and resumes
TC-18	NFR-01	Run the game for several waves	No visible stutter; window drag does not cause jumps
7.4 Example automated tests (JUnit 5)

Save as GameBoardTest.java next to the sources:

java
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class GameBoardTest {
    private GameBoard newBoard() { return new GameBoard(new GameSettings()); } // Normal: $175, 20 lives

    @Test void cannotBuildOnPath() {
        assertFalse(newBoard().canBuild(0, 1));
    }

    @Test void buildingDeductsMoney() {
        GameBoard b = newBoard();
        assertTrue(b.placeTower("Basic", 3, 2));
        assertEquals(175 - 50, b.getMoney());
    }

    @Test void sellingRefundsSixtyPercentOfTotalSpent() {
        GameBoard b = newBoard();
        b.placeTower("Basic", 3, 2);      // -50
        b.upgradeTower(3, 2);             // -50
        b.sellTower(3, 2);                // +60 (60% of 100)
        assertEquals(175 - 50 - 50 + 60, b.getMoney());
    }

    @Test void waveOneHasSevenEnemies() {
        GameBoard b = newBoard();
        b.startWave();
        assertEquals(7, b.getEnemiesRemaining());
    }

    @Test void frostTowerDamagesAndSlows() {
        GameBoard b = newBoard();
        Enemy e = new Enemy("Grunt", 1, 1.0, b.getPath());
        new FrostTower(3, 2).onHit(e, Collections.singletonList(e));
        assertEquals(51.0, e.getHealth(), 1e-9);   // 55 - 4
        assertTrue(e.isSlowed());
    }
}

Run with the JUnit Console Launcher (download junit-platform-console-standalone.jar):

bash
javac -cp junit-platform-console-standalone.jar *.java
java -jar junit-platform-console-standalone.jar --class-path . --scan-class-path
7.5 Quality gates (proposed)
All unit tests pass and the game compiles with no warnings (javac -Xlint:all).
Manual checklist TC-01 to TC-18 passes on at least two operating systems before a release.
No new class over about 400 lines without a documented reason.
8. Maintenance and evolution
8.1 Roadmap (prioritised backlog)
Priority	Item	Type
Must	Add JUnit tests for the model layer (section 7.4)	Quality
Must	Replace String type names with enums (TowerType, EnemyType)	Tech debt
Should	Targeting modes (first, strongest, closest)	Feature
Should	Save high scores to a file	Feature
Should	More maps plus a map selector	Feature
Could	Flying enemies and boss waves	Feature
Could	Sound effects and animations	Feature
Could	Move classes into packages and add Maven or Gradle	Tech debt
8.2 Known technical debt
Issue	Impact	Suggested fix
GameGUI.java is large (about 940 lines) and mixes HUD, input and drawing	Hard to read and change	Split into BoardPanel, SidePanel, ResultCard files
Tower and enemy types are plain strings	Typos fail silently; switch statements spread	Enums plus a registry or factory
Model classes import java.awt.Color and Point	Small leak of UI types into the model	Store an RGB int or a neutral value object; keep colours in Theme
Balance numbers are hard-coded across classes	Tuning requires code edits	Move to a GameConfig class or properties file
Map is hard-coded in the GameBoard constructor	Cannot add maps without editing it	A MapDefinition class
No automated tests	Regressions are found only by playing	See roadmap
Default package	Cannot be used as a library	Named packages
8.3 How to add a new tower type (extension guide)
In Tower.java, create class MyTower extends Tower with a COST constant; set range, damage, cooldown in the constructor and implement getColor().
Override onHit(Enemy, List<Enemy>) only if the tower has special behaviour.
Add the new name to GameBoard.createTower() and GameBoard.getTowerCost().
Add a card for it in the tower list in GameGUI and bind its key.
Add rows to the tower table in this README, a unit test, and manual test cases.
8.4 Versioning and release

Use semantic versioning (MAJOR.MINOR.PATCH) and tag each completed iteration (for example v0.1.0 for the core, v0.4.0 for the UI iteration). Keep a short CHANGELOG entry per tag.

9. Risks
Risk	Likelihood	Impact	Mitigation
Game balance feels unfair or dull	High	Medium	Iterative tuning, deterministic waves, central balance table
Regressions as features are added	Medium	Medium	Automated model tests; regression checklist
GameGUI grows unmanageable	Medium	Medium	Split the class; cap file size
Scope creep from the feature wishlist	Medium	Medium	Fixed iteration length; prioritised backlog
Environment differences (fonts, DPI, look and feel)	Low	Low	Test on two OSes; fallback to default look and feel (already handled in TowerLab)
10. Repository layout
towerlab/
├── TowerLab.java        Entry point
├── MainMenu.java        Welcome screen and game configuration
├── GameGUI.java         Game window, HUD, drawing, result screen, game loop
├── Theme.java           Shared colours, fonts, custom rounded buttons and panels
├── GameBoard.java       Game logic: grid, path, waves, building, update loop
├── Tower.java           Abstract Tower plus Basic, Sniper, Frost, Cannon
├── Enemy.java           Enemy movement, health, slowing
├── Projectile.java      Shots flying from towers to enemies
├── GameSettings.java    Player name, difficulty, derived values
├── docs/index.md        TechDocs landing page
├── mkdocs.yml           Documentation site config
├── catalog-info.yaml    Backstage catalog entry (owner: kavyan256)
└── .gitignore
11. Contributing
Pick an item from the roadmap (or open an issue describing the change).
Create a branch per feature, for example feature/targeting-modes.
Follow the existing style: small classes, private fields, intent-revealing comments.
Add or update tests and the relevant README tables.
Compile with javac -Xlint:all *.java, run the tests and the manual checklist.
Open a pull request describing the change and how it was tested. Use short imperative commit messages ("Add targeting mode enum").
License

Open source, available for educational purposes.
