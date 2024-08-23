/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;

/**
 * A mutable Minesweeper board with a specified width and height where each tile
 * is in one of the following states: -untouched state, a tile is untouched. -
 * flagged state, a tile has a flag on it. - dugged state, a tile without bomb
 * under it and without flag on it.
 * 
 * Action that can be performed on the tiles: 1. Dig a tile in the untouched
 * state, and change its state to dug state 2. Add a flag on a tile in the
 * untouched state, and change its state to flagged state. 3. Remove a flag from
 * a tile in a flagged state, and change its state to untouched state.
 */
public class Board {
    // fields
    private final int width;
    private final int height;
    private final Set<Integer> tilesContainingBombs;
    private final Map<Integer, Tile> tiles;
    private final Set<Integer> tilesContainingFlags;

    // Abstraction function
    // AF(sizeX, sizeY, tiles, flags, bombs): A Minesweeper board of width SizeX and
    // height SizeY containing SizeX * SizeY tiles. Every tile in the tiles
    // collection is on the board. Tiles in the bombs collection have a bomb
    // underneath them, and those in the flags collection have a flag on them.

    // Representation invariant
    // 1. A Board of SizeX and SizeY should contain SizeX * SizeY unique tiles.
    // 2. If a tile is at (x,y) position on the board then its tile number should be
    // x * SizeX + y.
    // 3. An untouched tile may contain a bomb under it or have a flag on it.
    // 4. A dug tile will not have a flag and bomb on it.
    // 5. If a tile has bombCount bombs in its neighborhood then it must have
    // exactly bombCount bombs in its neighborhood.

    // Safety from representation exposure
    // 1. All the fields are private and final.
    // 2. All the observers, creators, and mutators don't reveal internal
    // representation to the client.

    // Thread safety argument
    // 1. All the fields are private and final.
    // 2. digAt method is a synchronized method, only one user will be able to dig
    // on any tile at a given time by acquiring the lock associated with the
    // board instance.
    // 3. addFlag & removeFlag methods will acquire the lock associated with the
    // tile instance. Thus, multiple users can add flag to or remove flag from tiles
    // at different locations but only one user will be able to add flag to or
    // remove flag from the same tile.
    // 4. All the access to tilesContainingFlags happen inside addFlag & removeFlag
    // method and it is guarded by the tiles lock. Thus, multiple users can
    // access tilesContiningFlags but each method is dealing with a different tile
    // 5. All the access to tilesContainingBombs happen inside digAt methods and it
    // is guarded by the board's lock. Thus, only one user will be able to access it
    // at a time.
    // 6. Observers like getDugTiles, getFlaggedTiles, getTilesWithBomb,
    // toString, and hashCode & hashCode are synchronized methods and only one user
    // will be able to observer the current status of the board at a time.
    // 7. Observers like isUntouched, containsBomb, and isFlagged deal with a single
    // tile and all the access are guarded by the tile's lock. Thus, multiple users
    // can check the status of different tiles concurrently only one user will be
    // able to check the status of a the same tile at a time.

    // Constructor
    public Board(int width, int height) {
	this.width = width;
	this.height = height;
	this.tilesContainingBombs = new HashSet<>();
	this.tiles = new HashMap<>();
	this.tilesContainingFlags = new HashSet<>();
	for (int positionX = 0; positionX < width; positionX++) {
	    for (int positionY = 0; positionY < height; positionY++) {
		Integer tileNumber = convertTo1DPosition(positionX, positionY);
		if (Math.random() == 0.25) {
		    tilesContainingBombs.add(tileNumber);
		    tiles.put(tileNumber, new Tile(positionX, positionY, true, false, false, true, 0));
		} else {
		    tiles.put(tileNumber, new Tile(positionX, positionY, true, false, false, false, 0));
		}
	    }
	}

	for (Integer tileNumber : tilesContainingBombs) {
	    List<Integer> bombCoordinates = convertTo2DPosition(tileNumber);
	    increaseBombCountOfNeighbor(bombCoordinates.get(0), bombCoordinates.get(1));
	}
    }

    public Board(int width, int height, Set<List<Integer>> tilesContainingBombs) {
	System.out.println("width: " + width + " height:" + height);
	this.width = width;
	this.height = height;
	this.tiles = new HashMap<>();
	this.tilesContainingBombs = new HashSet<>();
	this.tilesContainingFlags = new HashSet<>();

	for (int positionX = 0; positionX < width; positionX++) {
	    for (int positionY = 0; positionY < height; positionY++) {
		List<Integer> tileCoordinate = List.of(positionX, positionY);
		Integer tileNumber = convertTo1DPosition(tileCoordinate.get(0), tileCoordinate.get(1));
		if (tilesContainingBombs.contains(tileCoordinate)) {
		    System.out.println("X: " + positionX + " Y:" + positionY);
		    this.tilesContainingBombs.add(tileNumber);
		    tiles.put(tileNumber, new Tile(positionX, positionY, true, false, false, true, 0));
		} else {
		    tiles.put(tileNumber, new Tile(positionX, positionY, true, false, false, false, 0));
		}
	    }
	}
	for (List<Integer> tileCoordinates : tilesContainingBombs) {
	    increaseBombCountOfNeighbor(tileCoordinates.get(0), tileCoordinates.get(1));
	}
    }

    public Board(int width, int height, Set<List<Integer>> duggedTiles, Set<List<Integer>> flaggedTiles,
	    Set<List<Integer>> tilesContainingBomb) {

	for (List<Integer> tile : duggedTiles) {
	    if (flaggedTiles.contains(tile) | tilesContainingBomb.contains(tile)) {
		throw new IllegalArgumentException(
			"Dugged tile should not have a flag on it or contain a bomb under it.");
	    }
	}

	boolean isDug = false;
	boolean hasFlag = false;
	boolean containsBomb = false;
	this.width = width;
	this.height = height;
	this.tilesContainingBombs = new HashSet<>();
	this.tiles = new HashMap<>();
	this.tilesContainingFlags = new HashSet<>();

	for (int positionX = 0; positionX < width; positionX++) {
	    for (int positionY = 0; positionY < height; positionY++) {
		List<Integer> tileCoordinates = List.of(positionX, positionY);
		isDug = duggedTiles.contains(tileCoordinates);
		hasFlag = flaggedTiles.contains(tileCoordinates);
		containsBomb = tilesContainingBomb.contains(tileCoordinates);
		Integer tileNumber = convertTo1DPosition(positionX, positionY);

		if (isDug) {
		    // This tile is dug, without flag and bomb
		    tiles.put(tileNumber, new Tile(positionX, positionY, false, false, isDug, false, 0));
		} else if (hasFlag) {
		    // tile is in flagged state and may contain bomb,
		    tiles.put(tileNumber, new Tile(positionX, positionY, false, hasFlag, false, containsBomb, 0));
		    tilesContainingFlags.add(tileNumber);
		    if (containsBomb) {
			// tile contains bomb add it to tilesContainingBombs
			tilesContainingBombs.add(tileNumber);
		    }
		} else {
		    // tile is in the untouched state, may contain a bomb
		    tiles.put(tileNumber, new Tile(positionX, positionY, true, false, false, containsBomb, 0));
		    if (containsBomb) {
			// tile contains bomb add it to tilesContainingBombs
			tilesContainingBombs.add(tileNumber);
		    }

		}
	    }
	}

	for (List<Integer> bombLocation : tilesContainingBomb) {
	    increaseBombCountOfNeighbor(bombLocation.get(0), bombLocation.get(1));
	}
    }

    /**
     * Returns a list containing all the dug tiles sorted by their x coordinates,
     * and then by their y coordinates if x coordinates are equal.
     * 
     * @return a list of (x,y) coordinates whose tile is already dug.
     */
    public synchronized List<List<Integer>> getDugTiles() {
	List<List<Integer>> duggedTiles = new ArrayList<>();
	for (Entry<Integer, Tile> entry : tiles.entrySet()) {
	    if (entry.getValue().isDug()) {
		duggedTiles.add(convertTo2DPosition(entry.getKey()));
	    }
	}

	return duggedTiles;
    }

    /**
     * Returns a list containing all the flagged tiles sorted by their x
     * coordinates, and then by their y coordinates if x coordinates are equal.
     * 
     * @return a list of (x,y) coordinates whose tile is already dug.
     */
    public synchronized List<List<Integer>> getFlaggedTiles() {
	List<List<Integer>> flaggedTiles = new ArrayList<>();
	List<Integer> flaggedTilesList = new ArrayList<>(tilesContainingFlags);
	flaggedTilesList.sort(null);
	for (Integer tileNumber : flaggedTilesList) {
	    flaggedTiles.add(convertTo2DPosition(tileNumber));
	}
	return flaggedTiles;

    }

    /**
     * Returns a list containing all the tiles having bomb sorted by their x
     * coordinates, and then by their y coordinates if x coordinates are equal.
     * 
     * @return a list of (x,y) coordinates whose tile is already dug.
     */
    public synchronized List<List<Integer>> getTilesWithBomb() {
	List<List<Integer>> bombedTiles = new ArrayList<>();
	List<Integer> bombedTilesList = new ArrayList<>(tilesContainingBombs);
	bombedTilesList.sort(null);
	for (Integer tileNumber : bombedTilesList) {
	    bombedTiles.add(convertTo2DPosition(tileNumber));
	}
	return bombedTiles;
    }

    /**
     * Returns the width of this board
     * 
     * @return, the width of this board
     */
    public int getWidth() {
	return width;
    }

    /**
     * Returns the height of this board
     * 
     * @return, the height of this board
     */
    public int getHeight() {
	return height;
    }

    /**
     * Checks whether the tile at the specified (x,y)-coordinates is in the
     * untouched state, returns true if the tile is untouched otherwise, return
     * false.
     * 
     * @param positionX, any x co-ordinate, must be within bound.
     * @param positionY, any y co-ordinate, must be within bound.
     * @return true if this tile is untouched, false otherwise.
     */
    public boolean isUntouched(int positionX, int positionY) {
	// If the tile is not within the bounds then return false
	if (!isWithinBound(positionX, positionY)) {
	    throw new IllegalArgumentException("The tile coordinates are out of bounds.");
	}

	Tile tile = tiles.get(this.convertTo1DPosition(positionX, positionY));
	return tile.isUntouched();
    }

    /**
     * Checks whether this tile at the specified (x,y)-coordinates has a bomb under
     * it, returns true if the tile has a flag on it, otherwise return false
     * 
     * @param positionX, any x coordinate, must be within bound.
     * @param positionY, any y coordinate, must be within bound.
     * @return true if this tile has a bomb under it,otherwise returns false
     */
    public boolean containsBomb(int positionX, int positionY) {
	// If the tile is not within the bounds then return false
	if (!isWithinBound(positionX, positionY)) {
	    throw new IllegalArgumentException("The tile coordinates are out of bounds.");
	}

	Tile tile = tiles.get(this.convertTo1DPosition(positionX, positionY));
	return tile.containsBomb();
    }

    /**
     * Checks whether this tile at the specified (x,y)-coordinates is in the flagged
     * state, returns true if the tile has a flag on it, otherwise return false.
     * 
     * @param positionX, any x coordinate, must be within bound.
     * @param positionY, any y coordinate, must be within bound.
     * @return true if this tile has a flag on it, otherwise returns false
     */
    public boolean isFlagged(int positionX, int positionY) {
	// If the tile is not within the bounds then return false
	if (!isWithinBound(positionX, positionY)) {
	    throw new IllegalArgumentException("The tile coordinates are out of bounds.");
	}

	Tile tile = tiles.get(this.convertTo1DPosition(positionX, positionY));
	return tile.isFlagged();
    }

    /**
     * Returns the string representation of this board that consists a series of
     * newline-separated rows of space-separated characters, thereby giving a grid
     * representation of the board’s state with exactly one char for each square.
     * 
     * The mapping of characters is as follows: “-” for squares with state
     * untouched. “F” for squares with state flagged. “ ” (space) for squares with
     * state dug and 0 neighbors that have a bomb. Integer COUNT in range [1-8] for
     * squares with state dug and COUNT neighbors that have a bomb.
     * 
     * @return string representation of this board
     */
    @Override
    public synchronized String toString() {
	String space = " ";
	String newline = "\n";
	String result = "";

	for (int height = 0; height < this.height; height++) {
	    for (int width = 0; width < this.width; width++) {
		Tile tile = tiles.get(convertTo1DPosition(width, height));
		result += tile.toString();

		if (width == this.width - 1) {
		    continue;
		}

		result += space;
	    }

	    if (height == this.height - 1) {
		continue;
	    }

	    result += newline;
	}

	return result;
    }

    /**
     * Removes the flag from the tile at the given (x,y) co-ordinate,only if the
     * tile is in the flagged state.
     * 
     * @param positionX, x coordinate of the given tile, must be within bound
     * @param positionY, y coordinate of the given tile, must be within bound
     * @return true, if the flag from the tile at specified co-ordinate was removed,
     *         false, otherwise.
     */
    public boolean removeFlagFrom(int positionX, int positionY) {
	// If the tile is not within bound then return false
	if (!isWithinBound(positionX, positionY)) {
	    return false;
	}

	// If the tile is untouched and has a flag on it, only then remove the flag from
	// it.
	Integer tileNumber = convertTo1DPosition(positionX, positionY);
	Tile tile = tiles.get(tileNumber);
	boolean result = false;
	synchronized (tile) {
	    if (tile.isFlagged()) {
		tile.removeFlag();
		tilesContainingFlags.remove(tileNumber);
		result = true;
	    }
	}
	return result;
    }

    /**
     * Adds a flag on the tile at the given (x,y) co-ordinate, only if the specified
     * tile is in the untouched state.
     * 
     * @param positionX, x coordinate of the given tile, must be within bound
     * @param positionY, y coordinate of the given tile, must be within bound
     * @return true, if the flag is added on the tile at specified co-ordinate,
     *         false, otherwise.
     */
    public boolean addFlagAt(int positionX, int positionY) {
	// If the tile is not within bound then return false
	if (!isWithinBound(positionX, positionY)) {
	    return false;
	}

	// If the tile is untouched and has no flag on it, only then add the flag
	// on it.
	boolean result = false;
	Integer tileNumber = convertTo1DPosition(positionX, positionY);
	Tile tile = tiles.get(tileNumber);
	synchronized (tile) {
	    if (tile.isUntouched()) {
		tile.addFlag();
		tilesContainingFlags.add(tileNumber);
		result = true;
	    }
	}
	return result;
    }

    /**
     * Digs the tile at the given (x,y) coordinate, removing any bomb under the it
     * (if a bomb is present), only if the specified tile is in the untouched state.
     * If the tile is successfully dug, this method will recursively reveal its
     * untouched neighbors, provided none of them contain a bomb.
     * 
     * @param positionX, x co-ordinate of the given tile, must be within bound
     * @param positionY, y co-ordinate of the given tile, must be within bound
     * @return true, if the tile at the specified coordinate was dug succesfully,
     *         false, otherwise.
     */
    public synchronized boolean digAt(int positionX, int positionY) {

	// If the tile is not within the bounds then return false
	if (!isWithinBound(positionX, positionY)) {
	    return false;
	}

	Integer tileNumber = convertTo1DPosition(positionX, positionY);
	Tile tile = tiles.get(tileNumber);
	boolean result = false;
	synchronized (tile) {
	    if (tile.isUntouched()) {
		if (tile.containsBomb()) {

		    // Case1: If the tile is untouched, has no flag on it and contains bomb then dig
		    // the tile and reveal its neighbors.
		    tile.digUp();
		    tilesContainingBombs.remove(tileNumber);
		    decreaseBombCountOfNeighbor(positionX, positionY); // decrease the bomb count of neighboring tiles.
		    revealNeighbors(positionX, positionY); // reveal the neighboring tiles.
		    result = true; // Contained bomb and it was blown off !

		} else {

		    // Case2: If the tile is untouched, has no flag on it and does not contain bomb
		    // then dig the tile and reveal its neighbors.
		    tile.digUp();
		    revealNeighbors(positionX, positionY); // reveal the neighboring tiles.
		}

	    }
	}

	// Case 3: If the tile is already dug or has a flag on it then don't do anything
	return result;
    }

    @Override
    public synchronized boolean equals(Object thatObject) {
	if (thatObject instanceof Board) {
	    Board anotherBoard = (Board) thatObject;
	    return sameValue(anotherBoard);
	}
	return false;
    }

    @Override
    public synchronized int hashCode() {
	int result = 0;
	for (Entry<Integer, Tile> entry : tiles.entrySet()) {
	    result -= entry.getValue().hashCode();
	}
	return result;
    }

    /**
     * Compares this board with another board and returns true, if both have same
     * size, same dug tiles, flags on the same tiles, contain bomb on the same
     * tiles, otherwise, returns false.
     * 
     * @param anotherBoard, another tile on the Minesweeper board
     * @return true both the boards have same size, same dug tiles, flags on same
     *         tiles, contain bomb on the same tiles,and have the same status, false
     *         otherwise.
     */
    private boolean sameValue(Board anotherTile) {
	return this.getWidth() == anotherTile.getWidth() && this.getHeight() == anotherTile.getHeight()
		&& this.getDugTiles().equals(anotherTile.getDugTiles())
		&& this.getFlaggedTiles().equals(anotherTile.getFlaggedTiles())
		&& this.getTilesWithBomb().equals(anotherTile.getTilesWithBomb());
    }

    /**
     * Converts the given (x,y) coordinate to the corresponding row major index.
     * 
     * @param positionX, any x co-ordinate, must be within bound.
     * @param positionY, any y co-ordinate, must be within bound.
     * @return the row major index corresponding to the given (x,y) co-ordinate.
     */
    private Integer convertTo1DPosition(int positionX, int positionY) {
	int tileNumber = positionY * width + positionX;
	return Integer.valueOf(tileNumber);
    }

    /**
     * Converts the given row major index to the corresponding (x,y) coordinate.
     * 
     * @param tileNumber, any index representing a tile, must be within bound, must
     *                    be within bound.
     * @return the (x,y) coordinates corresponding to the given row major index.
     */
    private List<Integer> convertTo2DPosition(int tileNumber) {
	Integer positionY = Math.floorDiv(tileNumber, width);
	Integer positionX = Math.floorMod(tileNumber, width);
	return List.of(positionX, positionY);
    }

    /**
     * Checks whether their exist one or more bombs in the neighborhood of a tile at
     * the specified coordinates.
     * 
     * @param positionX, an x-coordinate of a given tile, must be within bound.
     * @param positionY, an y-coordinate of a given tile, must be within bound.
     * @return true if there exist at-least one bomb in the neighbor of specified
     *         tile, false otherwise.
     */
    private boolean neighborContainsBomb(int positionX, int positionY) {

	// Obtain neighbors of this tile and for each tile check if it contains Bomb
	// if yes return true, false otherwise.
	Set<Integer> neighboringTiles = getNeighborsOf(positionX, positionY);

	// removes all the tiles from neighboringTiles that are not in
	// tilesContainingBombs
	for (Integer tileNumber : neighboringTiles) {
	    if (tilesContainingBombs.contains(tileNumber)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Checks whether their exist one or more flags in the neighborhood of a tile at
     * the specified coordinates.
     * 
     * @param positionX, an x-coordinate of a given tile.
     * @param positionY, an y-coordinate of a given tile.
     * @return true if there exist at-least one flag in the neighbor of specified
     *         tile, false otherwise.
     */
    private boolean neighborContainsFlag(int positionX, int positionY) {

	// Obtain neighbors of this tile and for each tile check if it has a flag on it
	// if yes return true, false otherwise.

	Set<Integer> neighboringTiles = getNeighborsOf(positionX, positionY);
	// removes all the tiles from neighboringTiles that are not in
	// tilesContainingBombs
	for (Integer tileNumber : neighboringTiles) {
	    if (tilesContainingFlags.contains(tileNumber)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Checks whether the tile at the given (x,y) co-ordinate has number of bombs
     * equal to the specified bomb count, where x and y co-ordinates
     *
     * @param positionX, x co-ordinate of the given tile.
     * @param positionY, y co-ordinate of the given tile.
     * @param BombCount, number of bombs, must be non-negative
     * @return true if the tile at given (x,y) co-ordinates has the specified number
     *         of bombs in its neighborhood, false otherwise.
     */
    private boolean checkBombCount(int positionX, int positionY, int BombCount) {
	Tile tile = tiles.get(convertTo1DPosition(positionX, positionY));
	return tile.getBombCount() == BombCount;
    }

    /**
     * For each of the untouched neighbors of the given (x, y) tile, change their
     * status to dug, and for only those tiles recursively repeat this procedure.
     * The Neighborhood of tile is defined bas all the tiles whose tile number is in
     * the set returned by revealNeighbors(positionX, positionY).
     * 
     * @param positionX, x co-ordinate of the given tile, must be within bounds.
     * @param positionY, y co-ordinate of the given tile, must be within bounds.
     * @return true, if the tiles in the neighborhood of the specified tile where
     *         dug false, otherwise.
     */
    private boolean revealNeighbors(int positionX, int positionY) {
	// Check if the neighbors have a flag or contain bomb, if yes then don't do
	// anything

	if (neighborContainsBomb(positionX, positionY)) {
	    return false;
	}

	// First, dig all the neighbors, afterwards attempt to reveal each neighbor of
	// the this tile
	Set<Integer> neighboringTiles = getNeighborsOf(positionX, positionY);
	Set<Integer> nextCall = new HashSet<>();

	for (Integer tileNumber : neighboringTiles) {
	    Tile tile = tiles.get(tileNumber);
	    synchronized (tile) {
		if (tile.isUntouched()) {
		    tile.digUp();
		    nextCall.add(tileNumber);
		}
	    }

	}

	// Afterwards, dig all the tiles in the nextCall
	for (Integer tileNumber : nextCall) {
	    List<Integer> tileCoordinates = convertTo2DPosition(tileNumber);
	    revealNeighbors(tileCoordinates.get(0), tileCoordinates.get(1));
	}
	return true;
    }

    /**
     * Returns the tile number of tiles that are within this board and one unit away
     * from the tile at given (x,y) co-ordinate. For the tiles that are on the edge
     * of the board, this method returns all the neighboring tiles that are within
     * the board.
     * 
     * @param positionX, x coordinate of the given tile, must be within bound.
     * @param positionY, y coordinate of the given tile, must be within bound.
     * @return a set of tile numbers that are within the board and one unit away
     *         from the tile at specified coordinate.
     */
    private Set<Integer> getNeighborsOf(int positionX, int positionY) {
	Set<Integer> neighbors = new HashSet<>();

	// A list of offsets of the neighboring tile.
	List<List<Integer>> offsets = List.of(List.of(-1, -1), List.of(0, -1), List.of(1, -1), List.of(-1, 0),
		List.of(1, 0), List.of(-1, 1), List.of(0, 1), List.of(1, 1));

	// For every offset in offset, create a new (x,y) coordinate and add it to the
	// neighbors only if it in within bounds
	for (List<Integer> offset : offsets) {
	    Integer newPositionX = positionX + offset.get(0);
	    Integer newPositionY = positionY + offset.get(1);
	    if (isWithinBound(newPositionX, newPositionY)) {
		neighbors.add(convertTo1DPosition(newPositionX, newPositionY));
	    }
	}
	return neighbors;
    }

    /**
     * Checks if the given (x, y) coordinate is within the board, returns true if it
     * is, otherwise false.
     * 
     * @param positionX, any x coordinate
     * @param positionY, any y coordinate
     * @return true if the specified (x,y) coordinate is within the board, false
     *         otherwise
     */
    private boolean isWithinBound(int positionX, int positionY) {
	return (0 <= positionX && positionX < width) && (0 <= positionY && positionY < height);
    }

    /**
     * Reduces the bomb count of the tiles that are in the neighborhood of the given
     * tile by one.
     * 
     * @param positionX, any x coordinate of tile having a bomb
     * @param positionY, any y coordinate of tile having a bomb
     */
    private void decreaseBombCountOfNeighbor(int positionX, int positionY) {
	Set<Integer> neighborTiles = getNeighborsOf(positionX, positionY);
	for (Integer tileNumber : neighborTiles) {
	    tiles.get(tileNumber).decreaseBombCountBy(1);
	}
    }

    /**
     * Increase the bomb count of the tiles that are in the neighborhood of the
     * given tile by one.
     * 
     * @param positionX, any x coordinate of tile having a bomb,
     * @param positionY, any y coordinate of tile having a bomb
     */
    private void increaseBombCountOfNeighbor(int positionX, int positionY) {
	Set<Integer> neighborTiles = getNeighborsOf(positionX, positionY);
	for (Integer tileNumber : neighborTiles) {
	    tiles.get(tileNumber).increaseBombCountBy(1);
	}
    }

}

/**
 * A mutable tile on a Minesweeper board representing a tile in one of the
 * following states: -untouched state, a tile is untouched. - flagged state, a
 * tile has a flag on it. - dugged state, a tile without bomb under it and
 * without flag on it.
 * 
 * Action that can be performed on the tiles: 1. Dig a tile in the untouched
 * state, and change its state to dug state 2. Add a flag on a tile in the
 * untouched state, and change its state to flagged state. 3. Remove a flag from
 * a tile in a flagged state, and change its state to untouched state.
 * 
 * @author Admin
 *
 */
class Tile {

    // fields
    private final int positionX;
    private final int positionY;
    private boolean dug;
    private boolean untouched;
    private boolean flagged;
    private boolean containsBomb;
    private int bombCount;

    // Abstraction function
    // AF(untouched, flagged, dug, bomb, bombCount) = A tile on a Minesweeper Board
    // with
    // following properties.
    // 1. Tile is at position (position x, position y) where
    // co-ordinate (x,y) start at top-left corner.
    // 2. The tile is untouched, if untouched == true.
    // 3. Tile has a flag, if flag == true.
    // 3. Tile is dug, if dugged == true.
    // 4. Tile contains a bomb, if bomb == true.
    // 5. Tile has bombCount bombs in its neighborhood.

    // Representation invariant
    // 1. A tile will only be in one of the three states - untouched, dug,
    // flagged.
    // 1. An untouched tile may contain a bomb.
    // 2. A flagged tile may contain a bomb.
    // 2. A dug tile will not have a flag or contain a bomb.
    // 3. Bomb count must never be negative.

    // Safety from representation exposure
    // 1. All the fields in this class are immutable.
    // 2. All the fields are declared private and only positionX and positionY are
    // final and remaining fields can be reassigned as follows:
    // - untouched is reassigned only in digUP and in addFlag.
    // - flagged is reassigned only in removeFlag and addFlag.
    // - dug and containsBomb are reassigned only in digUp.
    // - bombCount is reassigned only in decreaseBombCountBy
    // increaseBombCountBy methods.
    // 3. Observers like isUntouched(), containsFlag(), hasBomb(), getBombCount()
    // don't reveal the internal representation.
    // 4. Mutators like decreaseBombCountBy(), increaseBombCountCountBy(),
    // addFlag(), digUp(), removeFlag() don't reveal the internal representation.
    // 5. Creators don't reveal the internal representation.

    // Thread safety argument
    // All the access to the fields of this class are guarded by the Tile's lock

    // Constructor to create an untouched tile at (positionX, positionY) which may
    // contain bomb under it, and bombCount bomb in its neighborhood.
    Tile(int positionX, int positionY, boolean bomb, int bombCount) {
	this.positionX = positionX;
	this.positionY = positionY;
	this.untouched = true;
	this.dug = false;
	this.containsBomb = bomb;
	this.flagged = false;
	this.bombCount = bombCount;
    }

    // Constructor to create a tile at (positionX, positionY) with user defined
    // status.

    Tile(int positionX, int positionY, boolean untouched, boolean flagged, boolean dug, boolean bomb, int bombCount) {
	this.positionX = positionX;
	this.positionY = positionY;
	this.untouched = untouched;
	this.flagged = flagged;
	this.dug = dug;
	this.bombCount = bombCount;
	this.containsBomb = bomb;
    }

    /**
     * Checks whether this tile is in the untouched state, returns true if the tile
     * is untouched otherwise, return false.
     * 
     * @return true if this tile is in the untouched state, false otherwise.
     */
    public synchronized boolean isUntouched() {
	return untouched;
    }

    /**
     * Checks whether this tile is in the dug state, returns true if the tile is dug
     * otherwise, return false.
     * 
     * @return true if this tile is in the dug state, false otherwise.
     */
    public synchronized boolean isDug() {
	return dug;
    }

    /**
     * Checks whether this tile is in the flagged state, returns true if the tile is
     * in the flagged state, otherwise return false.
     * 
     * @return true if this tile has a flag on it, otherwise returns false
     */
    public synchronized boolean isFlagged() {
	return flagged;
    }

    /**
     * Checks whether this tile has a bomb under it, returns true if the tile has a
     * flag on it, otherwise return false
     * 
     * @return true if this tile has a bomb under it,otherwise returns false
     */
    public synchronized boolean containsBomb() {
	return containsBomb;
    }

    /**
     * Returns the string representation of this tile in the following format '-' if
     * the tile is untouched 'F' if the tile has a flag on it 'bombCount' if the
     * tile is dug and has at-least one bomb in its neighbor ' ' if the tile is dug
     * and has no bomb in its neighborhood
     * 
     * @return string representation of this tile
     */
    @Override
    public synchronized String toString() {
	if (untouched) {
	    return "-";
	} else if (flagged) {
	    return "F";
	} else {
	    if (bombCount == 0) {
		return " ";
	    } else {
		return String.valueOf(bombCount);
	    }
	}
    }

    /**
     * Returns the number of bombs in the neighborhood of this tile
     * 
     * @return, the the number of bombs in the neighborhood of this tile
     */
    public synchronized int getBombCount() {
	return bombCount;
    }

    /**
     * Returns the x co-ordinate of this tile
     * 
     * @return, the x co-ordinate of this tile
     */
    public synchronized int getpositionX() {
	return positionX;
    }

    /**
     * Returns the y co-ordinate of this tile
     * 
     * @return, the y co-ordinate of this tile
     */
    public synchronized int getpositionY() {
	return positionY;
    }

    /**
     * Decrements the bomb count of the neighborhood by a specified amount. If the
     * specified amount is greater than the actual bomb count, the bomb count will
     * be reduced to zero.
     * 
     * @param amountToDecrease, amount by which the bomb count will be decreased.
     * @return updated bomb count after the decrement.
     */
    public synchronized int decreaseBombCountBy(int amountToDecrease) {
	int amountToSubtract = Math.min(bombCount, amountToDecrease);
	bombCount -= amountToSubtract;
	return bombCount;
    }

    /**
     * Increments the bomb count of the neighborhood by a specified amount.
     * 
     * @param amountToIncrease, amount by which the bomb count will be increased.
     * @return updated bomb count after the increment.
     */
    public synchronized int increaseBombCountBy(int amountToIncrease) {
	bombCount += amountToIncrease;
	return bombCount;
    }

    /**
     * Adds a flag on this tile, only if it is in the untouched state.
     * 
     * @return true if the specified flag was placed on the tile, false otherwise.
     */
    public synchronized boolean addFlag() {
	if (isUntouched()) {
	    untouched = false;
	    flagged = true;
	    return true;
	}
	return false;
    }

    /**
     * Digs this tile and removes any bomb under it (if a bomb is present), only if
     * it is in the untouched state.
     * 
     * @return true if this tile is dug, false otherwise.
     */
    public synchronized boolean digUp() {
	if (isUntouched()) {
	    untouched = false;
	    dug = true;
	    if (containsBomb) {
		containsBomb = false;
	    }
	    return true;
	}
	return false;
    }

    /**
     * Removes the flag from this tile, only if it is untouched, and already has a
     * flag on it.
     * 
     * @return true if there was a flag on this tile and it was removed, false
     *         otherwise.
     */
    public synchronized boolean removeFlag() {
	if (isFlagged()) {
	    flagged = false;
	    untouched = true;
	    return true;
	}
	return false;
    }

    @Override
    public synchronized boolean equals(Object thatObject) {
	if (thatObject instanceof Tile) {
	    Tile anotherTile = (Tile) thatObject;
	    return this.sameValue(anotherTile);
	}
	return false;
    }

    /**
     * Compares this tile with another tile and returns true if both the tiles have
     * coordinates and have the same status, otherwise returns false.
     * 
     * @param anotherTile, any tile on a minesweeper board
     * @return true both the tiles have coordinates and have the same status, false
     *         otherwise.
     */
    private boolean sameValue(Tile anotherTile) {
	return this.positionX == anotherTile.getpositionX() && this.positionY == anotherTile.getpositionY()
		&& this.isUntouched() == anotherTile.isUntouched() && this.isDug() == anotherTile.isDug()
		&& this.isFlagged() == anotherTile.isFlagged() && this.getBombCount() == anotherTile.getBombCount();
    }

    @Override
    public synchronized int hashCode() {
	return Integer.hashCode(positionX) + Integer.hashCode(positionY) + Integer.hashCode(bombCount)
		+ Boolean.hashCode(containsBomb) + Boolean.hashCode(flagged) + Boolean.hashCode(untouched);
    }

}