/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.junit.Test;

/**
 * A class to test the interaction of Minesweeper server with more than one
 * clients.
 */
public class MinesweeperServerTest {

    // Testing strategy
    // Partition on Number of clients: one, more than one
    // Partition on type of board creation: randomly generated, parsed from file
    // Partition on type message sent by client: flag, deflag, dig, look, help, bye
    // Partition on type message sent by server: Board message, Boom message, Hello
    // message

    private static final String LOCALHOST = "127.0.0.1";
    private static final int PORT = 4000 + new Random().nextInt(1 << 15);

    private static final int MAX_CONNECTION_ATTEMPTS = 10;

    private static final String BOARDS_PKG = "autograder/boards/";

    /**
     * Start a MinesweeperServer in debug mode with a board file from BOARDS_PKG.
     * 
     * @param boardFile board to load
     * @return thread running the server
     * @throws IOException if the board file cannot be found
     */
    private Thread startMinesweeperServer(String boardFile) throws IOException {

	final URL boardURL = ClassLoader.getSystemClassLoader().getResource(BOARDS_PKG + boardFile);
	if (boardURL == null) {
	    throw new IOException("Failed to locate resource " + boardFile);
	}
	final String boardPath;
	try {
	    boardPath = new File(boardURL.toURI()).getAbsolutePath();
	} catch (URISyntaxException urise) {
	    throw new IOException("Invalid URL " + boardURL, urise);
	}
	final String[] args = new String[] { "--debug", "--port", Integer.toString(PORT), "--file", boardPath };
	Thread serverThread = new Thread(() -> MinesweeperServer.main(args));
	serverThread.start();
	return serverThread;
    }

    /**
     * Connect to a MinesweeperServer and return the connected socket.
     * 
     * @param server abort connection attempts if the server thread dies
     * @return socket connected to the server
     * @throws IOException if the connection fails
     */
    private Socket connectToMinesweeperServer(Thread server) throws IOException {
	int attempts = 0;
	while (true) {
	    try {
		System.out.println("Attempting to connect... Attempt #" + (attempts + 1));
		Socket socket = new Socket(LOCALHOST, PORT);
		socket.setSoTimeout(10000);
		System.out.println("Connection successful.");
		return socket;
	    } catch (ConnectException ce) {
		System.out.println("Connection failed: " + ce.getMessage());
		if (!server.isAlive()) {
		    throw new IOException("Server thread not running");
		}
		if (++attempts > MAX_CONNECTION_ATTEMPTS) {
		    throw new IOException("Exceeded max connection attempts", ce);
		}
		try {
		    Thread.sleep(attempts * 10);
		} catch (InterruptedException ie) {
		}
	    }
	}
    }

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
	assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // more than one clients, parsed from file,
    // flag, deflag, dig, look, help, bye sent by client
    // Board message, Boom message, Hello message
    @Test(timeout = 10000)
    public void gameSimulation() throws IOException {

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
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());

	outFromClient1ToServer.println("dig 4 5");
	assertEquals("BOOM!", inFromServerToClient1.readLine());

	outFromClient1ToServer.println("look"); // debug mode on
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - - -", inFromServerToClient1.readLine());
	assertEquals("- - - 2 1", inFromServerToClient1.readLine());
	assertEquals("- - 2 1  ", inFromServerToClient1.readLine());
	assertEquals("- - 1    ", inFromServerToClient1.readLine());
	assertEquals("- - 1    ", inFromServerToClient1.readLine());

	outFromClient2ToServer.println("dig 0 5");
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - 2 1", inFromServerToClient2.readLine());
	assertEquals("- - 2 1  ", inFromServerToClient2.readLine());
	assertEquals("- - 1    ", inFromServerToClient2.readLine());
	assertEquals("1 - 1    ", inFromServerToClient2.readLine());

	outFromClient2ToServer.println("dig 2 2");
	assertEquals("BOOM!", inFromServerToClient2.readLine());

	outFromClient2ToServer.println("dig 0 5"); // debug mode on
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - - - -", inFromServerToClient2.readLine());
	assertEquals("- - 1 1 1", inFromServerToClient2.readLine());
	assertEquals("- - 1    ", inFromServerToClient2.readLine());
	assertEquals("- - 1    ", inFromServerToClient2.readLine());
	assertEquals("1 - 1    ", inFromServerToClient2.readLine());

	Socket socket3 = connectToMinesweeperServer(thread);
	BufferedReader inFromServerToClient3 = new BufferedReader(new InputStreamReader(socket3.getInputStream()));
	PrintWriter outFromClient3ToServer = new PrintWriter(socket3.getOutputStream(), true);

	assertTrue("expected HELLO message", inFromServerToClient3.readLine().startsWith("Welcome"));

	outFromClient3ToServer.println("dig 0 2"); // debug mode on
	assertEquals("- - - - -", inFromServerToClient3.readLine());
	assertEquals("1 1 1 - -", inFromServerToClient3.readLine());
	assertEquals("    1 1 1", inFromServerToClient3.readLine());
	assertEquals("1 1 1    ", inFromServerToClient3.readLine());
	assertEquals("- - 1    ", inFromServerToClient3.readLine());
	assertEquals("1 - 1    ", inFromServerToClient3.readLine());

	outFromClient3ToServer.println("dig 0 0"); // debug mode on
	assertEquals("BOOM!", inFromServerToClient3.readLine());

	outFromClient3ToServer.println("look"); // debug mode on
	assertEquals("    1 - -", inFromServerToClient3.readLine());
	assertEquals("    1 - -", inFromServerToClient3.readLine());
	assertEquals("    1 1 1", inFromServerToClient3.readLine());
	assertEquals("1 1 1    ", inFromServerToClient3.readLine());
	assertEquals("- - 1    ", inFromServerToClient3.readLine());
	assertEquals("1 - 1    ", inFromServerToClient3.readLine());

	Socket socket4 = connectToMinesweeperServer(thread);
	BufferedReader inFromServerToClient4 = new BufferedReader(new InputStreamReader(socket4.getInputStream()));
	PrintWriter outFromClient4ToServer = new PrintWriter(socket4.getOutputStream(), true);

	assertTrue("expected HELLO message", inFromServerToClient4.readLine().startsWith("Welcome"));

	outFromClient4ToServer.println("flag 3 1"); // debug mode on
	assertEquals("    1 - -", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("- - 1    ", inFromServerToClient4.readLine());
	assertEquals("1 - 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("flag 1 4"); // debug mode on
	assertEquals("    1 - -", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("- F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 - 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("dig 0 4"); // debug mode on
	assertEquals("    1 - -", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("1 F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 - 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("dig 1 5"); // debug mode on
	assertEquals("    1 - -", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("1 F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("dig 4 0"); // debug mode on
	assertEquals("    1 - 1", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("1 F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("dig 3 0"); // debug mode on
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("    1 F -", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("1 F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());

	outFromClient4ToServer.println("dig 4 1"); // debug mode on
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("    1 F 1", inFromServerToClient4.readLine());
	assertEquals("    1 1 1", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());
	assertEquals("1 F 1    ", inFromServerToClient4.readLine());
	assertEquals("1 1 1    ", inFromServerToClient4.readLine());

	Socket socket5 = connectToMinesweeperServer(thread);
	BufferedReader inFromServerToClient5 = new BufferedReader(new InputStreamReader(socket5.getInputStream()));
	PrintWriter outFromClient5ToServer = new PrintWriter(socket5.getOutputStream(), true);

	assertTrue("expected HELLO message", inFromServerToClient5.readLine().startsWith("Welcome"));

	outFromClient5ToServer.println("deflag 3 1"); // debug mode on
	assertEquals("    1 1 1", inFromServerToClient5.readLine());
	assertEquals("    1 - 1", inFromServerToClient5.readLine());
	assertEquals("    1 1 1", inFromServerToClient5.readLine());
	assertEquals("1 1 1    ", inFromServerToClient5.readLine());
	assertEquals("1 F 1    ", inFromServerToClient5.readLine());
	assertEquals("1 1 1    ", inFromServerToClient5.readLine());

	outFromClient1ToServer.println("bye");
	socket1.close();

	outFromClient2ToServer.println("bye");
	socket2.close();

	outFromClient3ToServer.println("bye");
	socket3.close();

	outFromClient4ToServer.println("bye");
	socket4.close();

	outFromClient5ToServer.println("bye");
	socket5.close();

    }

}
