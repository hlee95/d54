package edu.mit.d54.plugins.shooter;

import edu.mit.d54.ArcadeController;
import edu.mit.d54.ArcadeListener;
import edu.mit.d54.Display2D;
import edu.mit.d54.DisplayPlugin;
import edu.mit.d54.TwitterClient;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;


/**
 * This plugin implements a space invaders shooter game. User input received
 * over a TCP socket on port 12345.
 */

public class ShooterPlugin extends DisplayPlugin implements ArcadeListener {

  private enum State {IDLE, GAME, GAME_END};
  private boolean beginEnd; // true when it's the first loop iteration in GAME_END

  private State gameState;
  private int score;
  private int[] scoreColor;
  private int level;
  private final int scoreRow = 0;
  private final int levelDifference = 10; // 10 hits increases level

  // All time units are in seconds.
  private final double dt;
  private double time;
  private double lastAnimTime;
  private double animStep = .80; // Time between animations, decreases with level
  private double lastShipSpawnTime;
  private double newShipSpawnStep = 2.0; // Time between spawns

  // For scrolling text display.
  private final String idleText = "P L A Y";
  private final String gameoverText = "SCORE: "; // Add their score
  private int textPos;
  private final double scrollStep = 0.05; // Time to slide characters to the left once
  private double lastScrollTime;

  private ArcadeController controller;

  private ShooterBoard board;

  // Pixels we are actually using.
  private final int verticalOffset = 1; // Leave one row empty at the top.
  private final int height = 14;
  private final int width = 8;
  private int[][][] rgb = new int[width][height + verticalOffset][3];

  public ShooterPlugin(Display2D display, double framerate) throws IOException {
    super(display, framerate);
    dt = 1.0 / framerate;
    time = 0.0;
    lastAnimTime = 0.0;
    lastShipSpawnTime = 0.0;
    textPos = -10; // Starting position for text.
    lastScrollTime = 0.0;
    level = 1;

    controller = ArcadeController.getInstance();

    gameState = State.IDLE;

    board = new ShooterBoard(width, height, verticalOffset, 2);

    scoreColor = new int[] {Color.orange.getRed(), Color.orange.getGreen(), Color.orange.getBlue()};

    System.out.println("Game paused until client connect");
    TwitterClient.tweet("Space invaders is now being played on the MIT Green Building! #mittetris");
  }

  @Override
  protected void onStart() {
    controller.setListener(this);
  }

  @Override
  protected void loop() {
    Display2D display = getDisplay(); // For board manipulation.
    Graphics2D gr = display.getGraphics(); // For easy text drawing.
    time += dt;
    boolean update = false;
    boolean draw = false;

    // Game logic, based on current game state.
    switch (gameState) {
      case IDLE:
        // Draw text on the building and just wait.
        if (time - lastScrollTime > scrollStep) {
          gr.drawString(idleText, -textPos, 12);
          textPos++;
          // Reset after we show the whole string and wait a bit.
          if (textPos > 5*idleText.length() + 1.0/scrollStep) {
            textPos = -10;
          }
          lastScrollTime = time;
        }
        break;
      case GAME:
        // Update the game time, decide if ships need to animate.
        // And we might need to add a new ship.
        // Update board state when events happen.
        // Handle gameover.
        draw = true;
        if (time - lastAnimTime > animStep) {
          update = true;
          lastAnimTime = time;
          boolean gameover = board.shiftDown();
          if (gameover) {
            System.out.println("game over, score was " + score);
            TwitterClient.tweet("Someone scored " + score + " playing space invaders on the MIT Green Building! #mittetris");
            beginEnd = true;
            gameState = State.GAME_END;
            break;
          }
        }
        if (time - lastShipSpawnTime > newShipSpawnStep) {
          update = true;
          System.out.println("spawn");
          lastShipSpawnTime = time;
          board.addShip(level);
        }
        break;
      case GAME_END:
        // If we just ended the game, clear up some loose ends like actually
        // ending the game on the board, and sleeping so that the player sees
        // the end state of the game for a couple seconds.
        if (beginEnd) {
          // Do this here and not before so that screen is paused with
          // ships still there, and then the screen clears after the delay below.
          board.endGame();
          draw = true;
          beginEnd = false;
          // Pause for 2.5 seconds so player can see what the board looks like,
          // then go into GAME_END.
          try {
            Thread.sleep(2500);
          } catch (InterruptedException e) {
            System.out.println(e);
            break;
          }
        }
        // Display some sort of end game message for a while
        // and then revert to idle state.
        String text = gameoverText + score;
        if (time - lastScrollTime > scrollStep) {
          gr.drawString(text, -textPos, 12);
          textPos++;
          // Reset after we show the whole string and wait a bit.
          if (textPos > 5*text.length() + 8) {
            try {
              Thread.sleep(1500);
            } catch (InterruptedException e) {
              System.out.println(e);
            }
            textPos = -10;
            gameState = State.IDLE;
          }
          lastScrollTime = time;
        }
        break;
      default:
        break;
    }

    // Display the current state of the game.
    // Only update rgb if there was an actual change in pixels.
    if (update) {
      rgb = board.getColors();
    }
    if (draw) {
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height + verticalOffset; j++) {
          display.setPixelRGB(i, j, rgb[i][j][0], rgb[i][j][1], rgb[i][j][2]);
        }
      }
      drawScore(display);
    }
  }

  // Handle an arcade button event.
  public void arcadeButton(byte b) {
    switch (gameState) {
      case IDLE:
      case GAME_END:
        // Starts game.
        TwitterClient.tweet("Beginning a game of space invaders on the MIT Green Building! #mittetris");
        textPos = -10; // Reset for next time we draw text
        System.out.println("new game starting");
        board.startGame();
        gameState = State.GAME;
        score = 0;
        level = 1;
        break;
      case GAME:
        // Moves defender or shoots.
        switch (b) {
          case 'L':
            board.moveDefender('L');
            break;
          case 'R':
            board.moveDefender('R');
            break;
          case 'U':
            boolean hit = board.shoot();
            if (hit) {
              // TODO: trigger some sort of special animation?
              score += 1;
              level = (score / levelDifference) + 1;
              animStep = (.80 - .1 * level);
              System.out.println("hit, score: " + score + " level: " + level);
            }
            break;
          default:
            break;
        }
      default:
        break;
    }
  }

  private void drawScore(Display2D display) {
    for (int i = 0; i < width; i++) {
      if ((score & 1<<i) > 0) {
        display.setPixelRGB(width - i, scoreRow, scoreColor[0], scoreColor[1], scoreColor[2]);
      }
    }
  }

}
