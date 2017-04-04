package edu.mit.d54.plugins.shooter;

import java.awt.Color;
import java.util.Random;
import java.util.Arrays;

/**
 * This class implements the board for the space invaders shooter game.
 * It keeps track of the locations of the ships and the defender, and is
 * able to return the correct colors to display to the shooter plugin.
 */
public class ShooterBoard {
  // Map ship levels to colors.
  private final int maxHitPoints = 6;
  private int[][] colors = new int[maxHitPoints + 1][3];
  private final int width;
  private final int height;
  private final int scale; // how wide each object is in pixels
  private int[][] ships; // each entry is a possible ship location
  private int defender; // column position in {0, 1, 2, 3}
  private int[] defenderColor;
  // Color map.
  // First index increases left to right, second index increases top to bottom,
  // to stay consistent with graphics conventions.
  private int[][][] rgb;

  public ShooterBoard(int _width, int _height, int _scale) {
    width = _width;
    height = _height;
    scale = _scale;
    ships = new int[width][height];
    rgb = new int[width * scale][height][3];
    int MAX_WIDTH = 9;
    int MAX_HEIGHT = 17;
    assert width * scale <= MAX_WIDTH;
    assert height <= MAX_HEIGHT;
    initColors();
  }

  // Assign different colors to different difficulties of invading ships.
  private void initColors() {
    defenderColor = new int[] {0, 255, 0};
    // Zeroeth index must be black because empty square should be black.
    Color[] COLORS = new Color[]{Color.black, Color.blue, Color.cyan,
                                 Color.magenta, Color.pink, Color.red,
                                 Color.yellow};
    assert COLORS.length >= colors.length;
    for (int c = 0; c < colors.length; c++) {
      colors[c][0] = COLORS[c].getRed();
      colors[c][1] = COLORS[c].getGreen();
      colors[c][2] = COLORS[c].getBlue();
    }
  }

  private int randomColumn() {
    return randInt(4);
  }

  private int randInt(int n) {
    return (int) (Math.random() * n);
  }

  private void updateDefender(int col) {
    // Clear old squares.
    for (int k = 0; k < 3; k++) {
      rgb[2*defender][height - 1][k] = 0;
      rgb[2*defender + 1][height - 1][k] = 0;
    }
    // Update column and color in the correct entries in rgb.
    defender = col;
    for (int k = 0; k < 3; k++) {
      rgb[2*defender][height - 1][k] = defenderColor[k];
      rgb[2*defender + 1][height - 1][k] = defenderColor[k];
    }
  }

  // Updates this.ships and this.rgb to reflect the new value at ships[i][j].
  private void updateShip(int i, int j, int val) {
    ships[i][j] = val;
    // Update rgb.
    for (int k = 0; k < 3; k++) {
      rgb[2*i][j][k] = colors[val][k];
      rgb[2*i+1][j][k] = colors[val][k];
    }
  }

  public void startGame() {
    updateDefender(randomColumn());
  }

  public void endGame() {
    // Clear ships and rgb.
    for (int i = 0; i < width; i ++) {
      for (int j = 0; j < height; j++) {
        ships[i][j] = 0;
      }
    }
    for (int i = 0; i < width * scale; i++) {
      for (int j = 0; j < height; j++) {
        for (int k = 0; k < 3; k++) {
          rgb[i][j][k] = 0;
        }
      }
    }
  }

  // Adds a ship in a random column.
  // Always add to the top row.
  public void addShip(int level) {
    int col = randomColumn();
    // Hitpoints is number of shots it takes to kill this ship.
    int hitpoints = Math.min(maxHitPoints, Math.max(1, randInt(level + 1)));
    updateShip(col, 0, hitpoints);
  }

  // Shifts the board down by one because one time step passed.
  // Returns true if game over (if a ship reaches the last row).
  public boolean shiftDown() {
    boolean gameover = false;
    // Start at bottom (highest y index) and shift.
    for (int i = 0; i < 4; i++) {
      for (int j = height - 2; j > -1; j--) {
        if (ships[i][j] > 0) {
          updateShip(i, j+1, ships[i][j]);
          updateShip(i, j, 0);
        }
      }
      if (ships[i][height - 1] > 0) {
        gameover = true;
      }
    }
    return gameover;
  }

  // Shift defender left or right.
  public void moveDefender(char b) {
    switch (b) {
    case 'L':
      updateDefender(Math.max(0, defender - 1));
      break;
    case 'R':
      updateDefender(Math.min(3, defender + 1));
      break;
    default:
      System.out.println("impossible");
      break;
    }
  }

  // Shoot in a column. Returns true if a ship was hit and destroyed.
  public boolean shoot() {
    // Depending on where defender is, remove one hit point from the ship
    // in that column that is closest to the bottom.
    boolean hit = false;
    for (int j = height - 1; j > -1; j--) {
      if (ships[defender][j] > 0) {
        updateShip(defender, j, ships[defender][j] - 1);
        // Only counts as a hit if the ship was destroyed.
        hit = (ships[defender][j] == 0);
        break;
      }
    }
    return hit;
  }

  // Caller should not modify rgb, but I am too lazy to make a copy before
  // returning it, so just trust myself not to mess it up.
  public int[][][] getColors() {
    return rgb;
  }
}
