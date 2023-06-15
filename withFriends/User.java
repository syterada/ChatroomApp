package Broadcast.withFriends;

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
	private static boolean friend_request = false;//flag for whether we have pending friend requests

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
					}else if(userInput.startsWith("@connect ")){//send protocol to make friend request
						String potential_friend = userInput.substring("@connect ".length());
						output_stream.println("#friendme " + potential_friend);

					}else if(userInput.startsWith("@friend ") && friend_request == true){//send protocol to accept friend request
						String new_friend = userInput.substring("@friend ".length());
						output_stream.println("#friends " + new_friend);
						friend_request = false;

					}else if (userInput.startsWith("@deny ") && friend_request == true){//send protocol to rejet friend request
						String rejected_friend = userInput.substring("@deny ".length());
						output_stream.println("#DenyFriendRequest " + rejected_friend);
						friend_request = false;

					}else if(userInput.startsWith("@disconnect ")){//send protocol to unfriend
						String unfriended = userInput.substring("@disconnect ".length());
						output_stream.println("#unfriend " + unfriended);
						

					}else if(userInput.startsWith("@")){//status messages cannot start with @
						System.out.println("User status messages cannot start with '@' character.");

					}else{//rest of status messages
						
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
				if(responseLine.equals("#welcome")){//welcomes user
					System.out.println("welcome, " + user_name + " to our Social Medial App." + '\n' + "To leave, enter Exit on a new line");

				}else if(responseLine.equals("#busy")){//lets user know server is busy
					System.out.println("Server is busy. Please try again later.");
					break;

				}else if(responseLine.equals("#statusPosted")){//sucessful post
					System.out.println("Your status was posted successfully!");

				}else if(responseLine.startsWith("#newuser ")){//lets user know of new user
					System.out.println("New user " + responseLine.substring("#newuser ".length()) + " has joined!!!");

				}else if(responseLine.startsWith("#newStatus ")){//lets user know of new status from a friend
					String entire_message = responseLine;
					String sender = entire_message.substring("#newStatus ".length());
					String user_message = sender.substring(sender.indexOf(" ")+1);
					sender = sender.substring(0,sender.indexOf(" "));//slices string for sender and sender's msg
					System.out.println("<" + sender + ">: " + user_message);

				}else if(responseLine.startsWith("#Leave ")){//lets user know of leaving user
					System.out.println("The user " + responseLine.substring("#Leave ".length()) + " is leaving!!!");

				}else if(responseLine.equals("#Bye")){//ack of successful leaving
					break;
					
				}else if(responseLine.startsWith("#friendme ")){//friend request comes in
					String requesting_friend = responseLine.substring("#friendme ".length());
					friend_request = true;
					System.out.println("You have received a friend request from " + requesting_friend + ". To accept type @friend " + requesting_friend + ", to deny type @deny " + requesting_friend);

				} else if(responseLine.startsWith("#OKfriends ")){//friend request accepted message
					String friend1 = responseLine.substring("#OKfriends ".length());
					String friend2 = friend1.substring(friend1.indexOf(" ") + 1);
					friend1 = friend1.substring(0, friend1.indexOf(" "));//slices string for user1 and user2
					System.out.println(friend1 + " and " + friend2 + " are now friends!");

				}else if (responseLine.startsWith("#FriendRequestDenied ")){//lets user know that their friend request was rejected
					String rejecter = responseLine.substring("#FriendRequestDenied ".length());
					System.out.println(rejecter + " has rejected your friend request.");

				}else if(responseLine.startsWith("#NotFriends ")){//lets user know someone unfriended them
					String friend1 = responseLine.substring("#NotFriends ".length());
					String friend2 = friend1.substring(friend1.indexOf(" ") + 1);
					friend1 = friend1.substring(0, friend1.indexOf(" "));//slices string for users who are no longer firends
					System.out.println(friend1 + " and " + friend2 + " are no longer friends.");
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