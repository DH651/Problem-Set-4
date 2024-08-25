/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper;

import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

/**
 * TODO: Description
 */
public class BoardTest {

    // Testing strategy for the Board
    // Testing strategy for digAt(), addFlagAt(), removeFlagFrom()
    // Partition on tile status: tile is untouched, tile has been dug.
    // Partition on flag status: tile has flag, tile has no flag.
    // Partition on bomb presence: tile has bomb, tile has no bomb.
    // Partition on tile location: tile is on edge, tile is within board,
    // tile is outside the board.

    // Testing strategy for digAt()
    // Partition on number of bombs in the neighborhood: No bombs, at-least one
    // bomb.
    // Partition on number of flags on neighboring tiles: No flags, at-least one
    // flag.
    // Partition on number of untouched tiles in neighborhood: All untouched tiles,
    // some tiles are dug, all the tiles are dug.

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
	assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // Test cases for Board
    // Tile is untouched, no flag on it , no bomb - neighbor of some tile have flag,
    // all neighbor of some tile are untouched, neighbors of some tile have bomb
    @Test
    public void testDig1() {
	String filePath = "ps4/test/test-cases/test-cases-1.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched, no flag on it , but has bomb - all neighbor tiles are
    // untouched
    @Test
    public void testDig2() {
	String filePath = "ps4/test/test-cases/test-cases-2.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched, no flag on it , but has bomb - all neighbor tiles are
    // empty
    @Test
    public void testDig3() {
	String filePath = "ps4/test/test-cases/test-cases-3.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched, no flag on it , but has bomb - some neighbor tiles are
    // empty and rest are untouched
    @Test
    public void testDig4() {
	String filePath = "ps4/test/test-cases/test-cases-4.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched, no flag on it , but has bomb - some neighbor tiles have
    // bomb
    @Test
    public void testDig5() {
	String filePath = "ps4/test/test-cases/test-cases-5.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched, no flag on it , but has bomb - some neighbor tiles have
    // flags on them
    @Test
    public void testDig6() {
	String filePath = "ps4/test/test-cases/test-cases-6.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched and has a flag on it
    @Test
    public void testDig7() {
	String filePath = "ps4/test/test-cases/test-cases-7.json";
	testdigAtHelper(filePath);

    }

    // Tile is dug, has no flag, no bomb
    @Test
    public void testDig8() {
	String filePath = "ps4/test/test-cases/test-cases-8.json";
	testdigAtHelper(filePath);

    }

    // Test cases for Board
    // Tile is untouched, no flag on it , but has no bomb
    @Test
    public void testDig9() {
	String filePath = "ps4/test/test-cases/test-cases-15.json";
	testdigAtHelper(filePath);

    }

    // Test cases for Board
    // Tile is untouched, no flag on it , but has no bomb
    @Test
    public void testDig10() {
	String filePath = "ps4/test/test-cases/test-cases-16.json";
	testdigAtHelper(filePath);

    }

    // Test cases for Board
    // Tile is untouched, no flag on it , but has no bomb
    @Test
    public void testDig11() {
	String filePath = "ps4/test/test-cases/test-cases-17.json";
	testdigAtHelper(filePath);

    }

    // Tile is untouched
    @Test
    public void testAddFlag1() {
	String filePath = "ps4/test/test-cases/test-cases-9.json";
	testAddFlagAtHelper(filePath);

    }

    // Tile has a flag on it
    @Test
    public void testAddFlag2() {
	String filePath = "ps4/test/test-cases/test-cases-10.json";
	testAddFlagAtHelper(filePath);

    }

    // Tiles are dug
    @Test
    public void testAddFlag3() {
	String filePath = "ps4/test/test-cases/test-cases-11.json";
	testAddFlagAtHelper(filePath);

    }

    // Tiles are have flag on them
    @Test
    public void testRemoveFlag1() {
	String filePath = "ps4/test/test-cases/test-cases-12.json";
	testRemoveFlagFromHelper(filePath);

    }

    // Tiles are untouched and no flag on them
    @Test
    public void testRemoveFlag2() {
	String filePath = "ps4/test/test-cases/test-cases-13.json";
	testRemoveFlagFromHelper(filePath);

    }

    // Tiles are dug already before the operation
    @Test
    public void testRemoveFlag3() {
	String filePath = "ps4/test/test-cases/test-cases-14.json";
	testRemoveFlagFromHelper(filePath);
    }

    /**
     * This methods attempts loads the JSON file from the specifed and digs at the
     * mentioned locations and checks if the actual board and expected board are
     * equal
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after digging on co-ordinates
     *         mentioned in the input list
     */
    private void testdigAtHelper(String filePath) {
	Triple<Board, List<List<Integer>>, Board> test = parseJsonFile(filePath);
	Board actualBoard = digAtMultipleLocations(test.boardBefore, test.input);
	Board expectedBoard = test.boardAfter;
	compareThisWithThat(actualBoard, expectedBoard);
    }

    /**
     * This methods attempts loads the JSON file from the specifed and adds flag at
     * the mentioned locations and checks if the actual board and expected board are
     * equal
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after digging on co-ordinates
     *         mentioned in the input list
     */
    private void testAddFlagAtHelper(String filePath) {
	Triple<Board, List<List<Integer>>, Board> test = parseJsonFile(filePath);
	Board actualBoard = addFlagAtMultipleLocations(test.boardBefore, test.input);
	Board expectedBoard = test.boardAfter;
	compareThisWithThat(actualBoard, expectedBoard);
    }

    /**
     * This methods attempts loads the JSON file from the specifed and removes flag
     * at the mentioned locations and checks if the actual board and expected board
     * are equal
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after digging on co-ordinates
     *         mentioned in the input list
     */
    private void testRemoveFlagFromHelper(String filePath) {
	Triple<Board, List<List<Integer>>, Board> test = parseJsonFile(filePath);
	Board actualBoard = removeFlagFromMultipleLocations(test.boardBefore, test.input);
	Board expectedBoard = test.boardAfter;
	compareThisWithThat(actualBoard, expectedBoard);
    }

    private void compareThisWithThat(Board actualBoard, Board expectedBoard) {
	assertTrue(String.format("Actual Board width %s and expectedBoard width %s", actualBoard.getWidth(),
		expectedBoard.getWidth()), actualBoard.getWidth() == expectedBoard.getWidth());
	assertTrue(String.format("Actual Board height %s and expectedBoard height %s", actualBoard.getHeight(),
		expectedBoard.getHeight()), actualBoard.getHeight() == expectedBoard.getHeight());
	assertTrue(String.format("Actual Board dugg tile %s and expectedBoard dugg tile %s", actualBoard.getDugTiles(),
		expectedBoard.getDugTiles()), actualBoard.getDugTiles().equals(expectedBoard.getDugTiles()));
	assertTrue(
		String.format("Actual Board flagged tile %s and expectedBoard flagged tile %s",
			actualBoard.getFlaggedTiles(), expectedBoard.getFlaggedTiles()),
		actualBoard.getFlaggedTiles().equals(expectedBoard.getFlaggedTiles()));
	assertTrue(
		String.format("Actual Board bombed tile %s and expectedBoard bombed tile %s",
			actualBoard.getTilesWithBomb(), expectedBoard.getTilesWithBomb()),
		actualBoard.getTilesWithBomb().equals(expectedBoard.getTilesWithBomb()));
	assertTrue(String.format("Expected %s board but the actual board is %s", actualBoard.toString(),
		expectedBoard.toString()), actualBoard.equals(expectedBoard));
    }

    /**
     * This methods attempts to digs on all the co-ordinates from the specified
     * input list
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after digging on co-ordinates
     *         mentioned in the input list
     */
    private Board digAtMultipleLocations(Board boardBefore, List<List<Integer>> Input) {
	for (List<Integer> coordinate : Input) {
	    boardBefore.digAt(coordinate.get(0), coordinate.get(1));
	}
	return boardBefore;
    }

    /**
     * This methods attempts adds flags on all the co-ordinates from the specified
     * input list
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after adding flag on co-ordinates
     *         mentioned in the input list
     */
    private Board addFlagAtMultipleLocations(Board boardBefore, List<List<Integer>> Input) {
	for (List<Integer> coordinate : Input) {
	    boardBefore.addFlagAt(coordinate.get(0), coordinate.get(1));
	}
	return boardBefore;
    }

    /**
     * This methods attempts remove flags on all the co-ordinates from the specified
     * input list
     * 
     * @param boardBefore, a Minesweeper board
     * @param Input,       a list of coordinates
     * @return the board that was transformed after adding flag on co-ordinates
     *         mentioned in the input list
     */
    private Board removeFlagFromMultipleLocations(Board boardBefore, List<List<Integer>> Input) {
	for (List<Integer> coordinate : Input) {
	    boardBefore.removeFlagFrom(coordinate.get(0), coordinate.get(1));
	}
	return boardBefore;
    }

    /**
     * This method attempts to transforms the data stored in JSON format into Triple
     * containing Minesweeper board before, Input, and Minesweeper board after.
     * 
     * @param file, a file path to a JSON file.
     * @return a triple containing three items which are the board before the
     *         transformation, input co-ordinates for transformation, and the board
     *         after the transformation, otherwise, returns null
     * @exception IOException, If the file which is being transformed fails to load
     *                         or does not exist.
     */
    private Triple<Board, List<List<Integer>>, Board> parseJsonFile(String filePath) {
	try {
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode rootTree = mapper.readTree(new File(filePath));

	    // Parse Before Section
	    Board boardBefore = parseBoard(rootTree.get("Before"));

	    List<List<Integer>> input = parseCoordinatesList(rootTree.get("Input"));

	    // Parse After Section
	    Board boardAfter = parseBoard(rootTree.get("After"));

	    return new Triple<>(boardBefore, input, boardAfter);

	} catch (IOException e) {

	    // handle the exception here
	    fail("Failed to read JSON file: " + e.getMessage());

	}

	return null;

    }

    /**
     * This method extracts the "dug", "flag", and "bombs" coordinate from the Json
     * node and creates a Board object
     * 
     * @param node, a node representing the mapping of "dug", "flag", and "bombs" to
     *              their coordinates
     * @return a Board object created by using the coordinates from "dug", "flag",
     *         and "bombs"
     */
    private Board parseBoard(JsonNode node) {
	int SizeX = node.get("Width").asInt();
	int SizeY = node.get("Height").asInt();
	Set<List<Integer>> duggedTiles = parseCoordinatesSet(node.get("Digged"));
	Set<List<Integer>> flags = parseCoordinatesSet(node.get("Flagged"));
	Set<List<Integer>> bombs = parseCoordinatesSet(node.get("Bombs"));
	return new Board(SizeX, SizeY, duggedTiles, flags, bombs);
    }

    /**
     * This method converts the coordinate data mentioned in JSON into a set of
     * co-ordinates
     * 
     * @param node, a node representing a list of co-ordinates
     * @return a set of co-ordinates
     */
    private Set<List<Integer>> parseCoordinatesSet(JsonNode node) {
	Set<List<Integer>> coordinateSet = new HashSet<>();
	// Create an Iterator to traverse co-ordinates in the list of co-ordinates
	// For every JsonNode representing a co-ordinate list we will create a list
	// containing x and y co-ordinates and add it in the coordinateSet
	Iterator<JsonNode> elements = node.elements(); // JsonNode object representing a list of list
	while (elements.hasNext()) {
	    List<Integer> coordinate = new ArrayList<>();
	    JsonNode element = elements.next(); // JsonNode object representing a coordinate list e.g. (x, y)

	    // Create an Iterator to traverse on the x and y co-ordinates.
	    // For every JsonNode representing an x and y co-ordinate we will add them in
	    // coordinate in order.
	    Iterator<JsonNode> coordElements = element.elements();
	    while (coordElements.hasNext()) {
		coordinate.add(coordElements.next().asInt());
	    }
	    coordinateSet.add(coordinate);
	}
	return coordinateSet;
    }

    /**
     * This method converts the coordinate data mentioned in JSON into a list of
     * co-ordinates
     * 
     * @param node, a node representing a list of co-ordinates
     * @return a list of co-ordinates
     */
    private List<List<Integer>> parseCoordinatesList(JsonNode node) {
	List<List<Integer>> coordinateList = new ArrayList<>();
	// Create an Iterator to traverse co-ordinates in the list of co-ordinates
	// For every JsonNode representing a co-ordinate list we will create a list
	// containing x and y co-ordinates and add it in the coordinateSet
	Iterator<JsonNode> elements = node.elements(); // JsonNode object representing a list of list
	while (elements.hasNext()) {
	    JsonNode element = elements.next();
	    List<Integer> coordinate = new ArrayList<>();
	    Iterator<JsonNode> coordElements = element.elements(); // Iterator for the x and y coordinate in the
								   // coordinate list

	    // Create an Iterator to traverse on the x and y co-ordinates.
	    // For every JsonNode representing an x and y co-ordinate we will add them in
	    // coordinate in order.
	    while (coordElements.hasNext()) {
		coordinate.add(coordElements.next().asInt()); // JsonNode object representing a coordinate list e.g. (x,
							      // y)
	    }
	    coordinateList.add(coordinate);
	}
	return coordinateList;
    }

    /**
     * This is a placeholder class that will be used to return the result of
     * parseBoard method.
     * 
     * @author Admin
     *
     * @param <X> boardBefore, Board before the transformation
     * @param <Y> input, Inputs for transformation
     * @param <Z> boardAfter, Board after the transformation
     */

    class Triple<X, Y, Z> {
	public final X boardBefore;
	public final Y input;
	public final Z boardAfter;

	public Triple(X boardBefore, Y input, Z boardAfter) {
	    this.boardBefore = boardBefore;
	    this.input = input;
	    this.boardAfter = boardAfter;
	}
    }

}
