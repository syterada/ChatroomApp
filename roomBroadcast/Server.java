package Broadcast.roomBroadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static userThread[] threads = null;


	public static void main(String args[]) {

		// The default port number.
		int portNumber = 8000;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		
		userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();//creating the connections
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {//if all are busy, tell user that server us busy
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private int maxUsersCount;

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;
		//counting variables for iteration
		int i;
		int count;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */

			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			output_stream = new PrintStream(userSocket.getOutputStream());
		
			userName = input_stream.readLine();//get username
			userName = userName.substring("#join ".length());



			/* Welcome the new user. */
			output_stream.println("#welcome");
			count = 0;
			while (count < maxUsersCount){//notifies other users of the user who just joined

					if(threads[count] == null || threads[count] == this){
						count++;

					}else{
					threads[count].output_stream.println("#newuser " + userName);
					count++;
					}
			}



			/* Start the conversation. */
			while (true) {

				String incoming_message = input_stream.readLine().trim();//get message from users

				if(incoming_message.startsWith("#status ")){
					String user_message = incoming_message.substring("#status ".length()); 
					output_stream.println("#statusPosted");//ack user who sent it
					count = 0;

					while (count < maxUsersCount){//broadcast to rest of users

						if(threads[count] == null || threads[count] == this){//make sure we're sending to a user that is not the user who just posted
							count ++;

						}else{
							threads[count].output_stream.println("#newStatus " + userName + " " + user_message);
							count ++;
						}
					}

				}else if(incoming_message.equals("#Bye")){
					output_stream.println("#Bye");//ack user who is leaving
					String leaving_user = userName;
					count = 0;

					while (count < maxUsersCount){//let other users know that user is leaving

						if(threads[count] == null || threads[count] == this){//make sure we senf to users who did not just leave
							count++;

						}else{
							threads[count].output_stream.println("#Leave " + leaving_user);
							count++;
						}
					}

					break;
				}

				}

			

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
}
