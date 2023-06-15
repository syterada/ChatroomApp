package Broadcast.roomBroadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;


public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;
	private static String user_name;

	public static void main(String[] args) {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out
			.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {                
				/* Create a thread to read from the server. */
				new Thread(new User()).start();

				
				// Get user name and join the social net
				System.out.println("Enter your name: ");
				user_name = inputLine.readLine();
				output_stream.println("#join "+user_name);
				

				while (!closed) {
					String userMessage = new String();
					String userInput = inputLine.readLine().trim();
					
					// Read user input and send protocol message to server
					if(userInput.equals("Exit")){
						output_stream.println("#Bye"); //send protocol to leave
						break;

					}else{
						output_stream.println("#status " +  userInput); //send protocol to post status

				}
				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine;
		
		try {
			while ((responseLine = input_stream.readLine()) != null) {

				// Display on console based on what protocol message we get from server.
				if(responseLine.equals("#welcome")){//print info to welcome user
					System.out.println("welcome, " + user_name + " to our Social Medial App." + '\n' + "To leave, enter Exit on a new line");

				}else if(responseLine.equals("#busy")){//protocol for busy server
					System.out.println("Server is busy. Please try again later.");
					break;

				}else if(responseLine.equals("#statusPosted")){//protocol for posted status
					System.out.println("Your status was posted successfully!");

				}else if(responseLine.startsWith("#newuser ")){//lets us know about new user
					System.out.println("New user " + responseLine.substring("#newuser ".length()) + " has joined!!!");

				}else if(responseLine.startsWith("#newStatus ")){//protocol for new status from another user
					String sender = responseLine.substring("#newStatus ".length());
					String message = sender.substring(sender.indexOf(" "));
					sender = sender.substring(0,sender.indexOf(" "));//gets sender name and their msg
					System.out.println("<" + sender + ">: " + message);
							
				}else if(responseLine.startsWith("#Leave ")){//lets us know about a leaving user
					System.out.println("The user " + responseLine.substring("#Leave ".length()) + " is leaving!!!");

				}else if(responseLine.equals("#Bye")){//lets us know server successfully ack'd this user leaving
					break;

					
				}


			}
			closed = true;
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}