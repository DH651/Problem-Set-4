/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper.server;

import static org.junit.Assert.fail;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import minesweeper.Board;

/**
 * Multiplayer Minesweeper server.
 */
public class MinesweeperServer {

    // System thread safety argument
    // 1. This class creates a separate thread for handling of each client
    // connection.
    // 2. Multiple clients can add flag to and/or remove flag from different tiles
    // at a same time. Each client will acquire the lock associated with a
    // particular
    // tile.
    // 3. Only one client will be able to dig at the board at a time, other tiles
    // can add flag and/or remove flag during digging. Each client will acquire the
    // lock associated with the entire board.

    /** Default server port. */
    private static final int DEFAULT_PORT = 4444;
    /** Maximum port number as defined by ServerSocket. */
    private static final int MAXIMUM_PORT = 65535;
    /** Default square board size. */
    private static final int DEFAULT_SIZE = 10;

    /** Socket for receiving incoming connections. */
    private final ServerSocket serverSocket;
    /** True if the server should *not* disconnect a client after a BOOM message. */
    private final boolean debug;

    private final Board board; // initialized in runMinesweeperServer

    private int clientCount;

    // AF(serverSocket, board, debug, clientCount) = A minesweeper server that
    // allows clientCount clients to interact with a single minesweeper board.
    // The server has following properties:
    // 1. It will not terminate the client connection when the client digs at a tile
    // containing a bomb, if debug flag is true.
    // 2. The serverSocket listens to incoming connections from remote clients.

    // Representation Invariant:
    // 1. serverSocket, board, clientCount, and debug should not point to null
    // 2. If the client digs at a tile containing a bomb and debug is off, send
    // BOOM! message to the client and their connection must be disconnected.

    // Representation Safety Argument
    // 1. serverSocket, debug, board are private and final.
    // 2. clientCount is private but not final, it is reassigned only in
    // handleConnection method.
    // 3. Creators of this class don't reveal internal representation
    // 4. All the public methods of the class don't reveal internal representation.

    /**
     * Make a MinesweeperServer that listens for connections on port.
     * 
     * @param port  port number, requires 0 <= port <= 65535
     * @param debug debug mode flag
     * @param board a Minesweeper board
     * @throws IOException if an error occurs opening the server socket
     */
    public MinesweeperServer(int port, boolean debug, Board board) throws IOException {
	serverSocket = new ServerSocket(port);
	this.debug = debug;
	this.board = board;
	this.clientCount = 0;
    }

    /**
     * Run the server, listening for client connections and handling them. Never
     * returns unless an exception is thrown.
     * 
     * @throws IOException if the main server socket is broken (IOExceptions from
     *                     individual clients do *not* terminate serve())
     */
    public void serve() throws IOException {
	while (true) {
	    // block until a client connects.
	    Socket socket = serverSocket.accept();

	    // handle every client in a separate thread.
	    new Thread(new Runnable() {

		public void run() {
		    try {
			System.out.println("Startig a client in a new thread.....");
			handleConnection(socket); // attempts to handle the client connection
		    } catch (IOException ioe) {
			ioe.printStackTrace(); // but don't terminate serve()
		    } finally {
			try {
			    socket.close(); // attempt to close the socket
			} catch (IOException e) {
			    e.printStackTrace();
			}

		    }
		}

	    }).start();

	}
    }

    /**
     * Handle a single client connection. Returns when client disconnects.
     * 
     * @param socket socket where the client is connected
     * @throws IOException if the connection encounters an error or terminates
     *                     unexpectedly
     */
    private void handleConnection(Socket socket) throws IOException {
	System.out.println("Handling client connection.....\n");
	final BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	final PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
	String helloMessage = String.format(
		"Welcome to the Minesweeper Board: It has %s rows and %s columns. "
			+ "There are %s clients including you. Type ‘help’ for help. \\r\\n",
		board.getWidth(), board.getHeight(), clientCount);

	// Send a hello message to the client immediately after its client connection is
	// established
	clientCount += 1;
	outToClient.println(helloMessage);
	outToClient.flush();

	// For rest of the interaction with the client
	try {

	    for (String line = inFromClient.readLine(); line != null; line = inFromClient.readLine()) {

		System.out.println("input from client: " + line);
		String output = handleRequest(line);
		System.out.println("Output to client: \n" + output);
		System.out.println("\n");
		outToClient.println(output);
		outToClient.flush();
		if (output.equals("BOOM!") && !debug) {
		    // Client dug at a tile that had a bomb and debug flag is off, end the client
		    // connection by breaking from this loop
		    break;
		}

	    }

	} catch (IOException exp) {
	    // Client sent "bye" message, end the client connections
	    System.err.println("Client connection is terminated");

	} finally {
	    socket.close();
	    clientCount -= 1;
	    outToClient.close();
	    inFromClient.close();

	}
    }

    /**
     * Handler for client input, performing requested operations and returning an
     * output message.
     * 
     * @param input message from client
     * @return message to client
     * @throws IOException, if the client connection is terminated.
     */
    private String handleRequest(String input) throws IOException {
	String regex = "(look)|(help)|(bye)|" + "(dig -?\\d+ -?\\d+)|(flag -?\\d+ -?\\d+)|(deflag -?\\d+ -?\\d+)";
	String returnMessage = "";
	String helpMessage = " Use any one of the command and follow "
		+ "the correct syntax: - look, bye, help, dig x y, flag x y, deflag x y";

	if (!input.matches(regex)) {
	    // invalid input: send a help message
	    returnMessage = "This command is not supported." + helpMessage;
	}
	String[] tokens = input.split(" ");
	if (tokens[0].equals("look")) {
	    // 'look' request
	    returnMessage = board.toString();

	} else if (tokens[0].equals("help")) {
	    // 'help' request
	    returnMessage = helpMessage;

	} else if (tokens[0].equals("bye")) {
	    // 'bye' request: end client connection
	    throw new IOException("Terminate the client connection");

	} else {

	    int x = Integer.parseInt(tokens[1]);
	    int y = Integer.parseInt(tokens[2]);

	    if (tokens[0].equals("dig")) {
		// 'dig' request
		boolean isRevealed = board.digAt(x, y);
		if (isRevealed) {
		    returnMessage = "BOOM!";
		} else {
		    returnMessage = board.toString();
		}

	    } else if (tokens[0].equals("flag")) {
		// 'flag' request
		board.addFlagAt(x, y);
		returnMessage = board.toString();

	    } else if (tokens[0].equals("deflag")) {
		// 'deflag' request
		board.removeFlagFrom(x, y);
		returnMessage = board.toString();

	    }
	}

	return returnMessage;
    }

    /**
     * Start a MinesweeperServer using the given arguments.
     * 
     * <br>
     * Usage: MinesweeperServer [--debug | --no-debug] [--port PORT] [--size
     * SIZE_X,SIZE_Y | --file FILE]
     * 
     * <br>
     * The --debug argument means the server should run in debug mode. The server
     * should disconnect a client after a BOOM message if and only if the --debug
     * flag was NOT given. Using --no-debug is the same as using no flag at all.
     * <br>
     * E.g. "MinesweeperServer --debug" starts the server in debug mode.
     * 
     * <br>
     * PORT is an optional integer in the range 0 to 65535 inclusive, specifying the
     * port the server should be listening on for incoming connections. <br>
     * E.g. "MinesweeperServer --port 1234" starts the server listening on port
     * 1234.
     * 
     * <br>
     * SIZE_X and SIZE_Y are optional positive integer arguments, specifying that a
     * random board of size SIZE_X*SIZE_Y should be generated. <br>
     * E.g. "MinesweeperServer --size 42,58" starts the server initialized with a
     * random board of size 42*58.
     * 
     * <br>
     * FILE is an optional argument specifying a file pathname where a board has
     * been stored. If this argument is given, the stored board should be loaded as
     * the starting board. <br>
     * E.g. "MinesweeperServer --file boardfile.txt" starts the server initialized
     * with the board stored in boardfile.txt.
     * 
     * <br>
     * The board file format, for use with the "--file" option, is specified by the
     * following grammar:
     * 
     * <pre>
     *   FILE ::= BOARD LINE+
     *   BOARD ::= X SPACE Y NEWLINE
     *   LINE ::= (VAL SPACE)* VAL NEWLINE
     *   VAL ::= 0 | 1
     *   X ::= INT
     *   Y ::= INT
     *   SPACE ::= " "
     *   NEWLINE ::= "\n" | "\r" "\n"?
     *   INT ::= [0-9]+
     * </pre>
     * 
     * <br>
     * If neither --file nor --size is given, generate a random board of size 10x10.
     * 
     * <br>
     * Note that --file and --size may not be specified simultaneously.
     * 
     * @param args arguments as described
     */
    public static void main(String[] args) {
	// Command-line argument parsing is provided. Do not change this method.
	boolean debug = false;
	int port = DEFAULT_PORT;
	int sizeX = DEFAULT_SIZE;
	int sizeY = DEFAULT_SIZE;
	Optional<File> file = Optional.empty();

	Queue<String> arguments = new LinkedList<String>(Arrays.asList(args));
	try {
	    while (!arguments.isEmpty()) {
		String flag = arguments.remove();
		try {
		    if (flag.equals("--debug")) {
			debug = true;
		    } else if (flag.equals("--no-debug")) {
			debug = false;
		    } else if (flag.equals("--port")) {
			port = Integer.parseInt(arguments.remove());
			if (port < 0 || port > MAXIMUM_PORT) {
			    throw new IllegalArgumentException("port " + port + " out of range");
			}
		    } else if (flag.equals("--size")) {
			String[] sizes = arguments.remove().split(",");
			sizeX = Integer.parseInt(sizes[0]);
			sizeY = Integer.parseInt(sizes[1]);
			file = Optional.empty();
		    } else if (flag.equals("--file")) {
			sizeX = -1;
			sizeY = -1;
			file = Optional.of(new File(arguments.remove()));
			if (!file.get().isFile()) {
			    throw new IllegalArgumentException("file not found: \"" + file.get() + "\"");
			}
		    } else {
			throw new IllegalArgumentException("unknown option: \"" + flag + "\"");
		    }
		} catch (NoSuchElementException nsee) {
		    throw new IllegalArgumentException("missing argument for " + flag);
		} catch (NumberFormatException nfe) {
		    throw new IllegalArgumentException("unable to parse number for " + flag);
		}
	    }
	} catch (IllegalArgumentException iae) {
	    System.err.println(iae.getMessage());
	    System.err.println(
		    "usage: MinesweeperServer [--debug | --no-debug] [--port PORT] [--size SIZE_X,SIZE_Y | --file FILE]");
	    return;
	}

	try {
	    runMinesweeperServer(debug, file, sizeX, sizeY, port);
	} catch (IOException ioe) {
	    throw new RuntimeException(ioe);
	}
    }

    /**
     * Start a MinesweeperServer running on the specified port, with either a random
     * new board or a board loaded from a file.
     * 
     * @param debug The server will disconnect a client after a BOOM message if and
     *              only if debug is false.
     * @param file  If file.isPresent(), start with a board loaded from the
     *              specified file, according to the input file format defined in
     *              the documentation for main(..).
     * @param sizeX If (!file.isPresent()), start with a random board with width
     *              sizeX (and require sizeX > 0).
     * @param sizeY If (!file.isPresent()), start with a random board with height
     *              sizeY (and require sizeY > 0).
     * @param port  The network port on which the server should listen, requires 0
     *              <= port <= 65535.
     * @throws IOException if a network error occurs
     */
    public static void runMinesweeperServer(boolean debug, Optional<File> file, int sizeX, int sizeY, int port)
	    throws IOException {

	Board board = new Board(10, 10);

	if (sizeX > 0 && sizeY > 0) {

	    // 1. If the sizeX and sizeY are both greater than zero, create a random board
	    // of the specified size
	    board = new Board(sizeX, sizeY);

	} else if (file.isPresent()) {

	    try {

		// check if it has correct grammar (if not, throw error),
		// otherwise, create a board by parsing the file.
		String filePath = file.get().getPath();
		if (checkGrammar(filePath)) {
		    board = createBoard(filePath);

		}
	    } catch (RuntimeException exception) {

		throw new IOException("Converted from RuntimeException: " + exception.getMessage(), exception);

	    }

	} else {

	    // 3. otherwise, create a random board of size 10 x 10.
	    board = new Board(10, 10);
	}

	// create a Minesweeper server and run it.
	MinesweeperServer server = new MinesweeperServer(port, debug, board);
	System.out.println("Server thread started.....");
	server.serve();

    }

    /**
     * Attempts to read the file and verify that its content matches the grammar
     * specified in the documentation of the main() method.
     * 
     * @param filePath, path to a file
     * @return true, if the files' content matches the grammar specified in the
     *         documentation of the main() method.
     * @throws IOException,      if the file does not exist at the given path or a
     *                           network error occurs.
     * @throws RuntimeException, when the files content doesn't conform to the
     *                           grammar specified in the main methods documentation
     */

    private static boolean checkGrammar(String filePath) throws IOException {

	try (BufferedReader readFromFile = new BufferedReader(new FileReader(filePath))) {

	    // Checks if the files' header is in the specified format.
	    String actualHeader = readFromFile.readLine();

	    if (!Pattern.matches("\\d+\\s\\d+", actualHeader)) {
		throw new RuntimeException("The header should contain , it didn't match the expected pattern.");
	    }

	    // parse the expected width and height
	    String[] actualHeaderList = actualHeader.split(" ");
	    int expectedWidth = Integer.valueOf(actualHeaderList[0]);
	    int expectedHeight = Integer.valueOf(actualHeaderList[1]);

	    int actualHeight = 0;
	    // Checks if the files' content matches the specified grammar.
	    Pattern expectedBoardRow = Pattern.compile("((0|1)\\s)*(0|1)");
	    String actualBoardRow = readFromFile.readLine();
	    while (actualBoardRow != null) {

		Matcher matcher = expectedBoardRow.matcher(actualBoardRow);

		if (!matcher.matches()) {
		    throw new RuntimeException(
			    "Check the grammar of the board lines, it didn't match the expected pattern.");
		}

		// Check, if the line has SizeX rows in it, if not throw an error.
		if (actualHeader.split(" ").length == expectedWidth) {
		    throw new RuntimeException("Board width does not match the specified width.");
		}
		actualHeight += 1;

		actualBoardRow = readFromFile.readLine();

	    }

	    // Check, if the board has SizeY rows in it, if not throw an error.
	    if (actualHeight != expectedHeight) {
		throw new RuntimeException("Board height does not match the specified height");
	    }

	    return true;

	} catch (IOException e) {
	    throw e;
	}

    }

    /**
     * Create a Minesweeper board by parsing the file at the given location.
     * 
     * @param filePath, the path to the file that contains a board, files content
     *                  should conform to the grammar specified in the main method's
     *                  documentation.
     * @return a Minesweeper board created by parsing the board in the given file.
     * @throws IOException, if the file does not exist at the given paths or if a
     *                      network error occurs.
     */
    private static Board createBoard(String filePath) throws IOException {

	try (BufferedReader readFromFile = new BufferedReader(new FileReader(filePath))) {

	    // parse the expected width and height
	    String[] actualHeaderList = readFromFile.readLine().split(" ");
	    int Width = Integer.valueOf(actualHeaderList[0]);
	    int Height = Integer.valueOf(actualHeaderList[1]);

	    // get bomb location
	    Set<List<Integer>> bombLocations = new HashSet<>();
	    int actualHeight = 0;
	    String actualBoardRowString = readFromFile.readLine();
	    while (actualBoardRowString != null) {

		String[] actualBoardRow = actualBoardRowString.split(" ");
		int actualWidth = 0;
		for (String val : actualBoardRow) {
		    if (Integer.valueOf(val) == 1) {
			bombLocations.add(List.of(actualWidth, actualHeight));
		    }
		    actualWidth += 1;
		}

		actualHeight += 1;

		actualBoardRowString = readFromFile.readLine();
	    }

	    Board parsedBoard = new Board(Width, Height, bombLocations);
	    return parsedBoard;

	} catch (IOException e) {
	    throw e;
	}

    }

}
