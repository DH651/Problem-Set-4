/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.junit.Test;

import autograder.PublishedTest;
import minesweeper.Board;

/**
 * TODO
 */
public class MinesweeperServerTest extends PublishedTest {

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
	assert false; // make sure assertions are enabled with VM argument: -ea
    }

    public void publishedTest() throws IOException {

	Thread thread = startMinesweeperServer("board_file_6.txt");

	Socket socket1 = connectToMinesweeperServer(thread);
	BufferedReader inFromServerToClient1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
	PrintWriter outFromClient1ToServer = new PrintWriter(socket1.getOutputStream(), true);

	assertTrue("expected HELLO message", inFromServerToClient1.readLine().startsWith("Welcome"));

	Socket socket2 = connectToMinesweeperServer(thread);
	BufferedReader inFromServerToClient2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
	PrintWriter outFromClient2ToServer = new PrintWriter(socket2.getOutputStream(), true);

	assertTrue("expected HELLO message", inFromServerToClient2.readLine().startsWith("Welcome"));

	outFromClient1ToServer.println("look");
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());

	outFromClient2ToServer.println("look");
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());

    }

}
