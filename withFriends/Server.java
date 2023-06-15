package Broadcast.withFriends;

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
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
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
	boolean[] friends;//array that gives a T/F value of whether a user is friends with another

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

		friends =new boolean[maxUsersCount];
		int user_number = -1;//gives index of this user in threads and friends array

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */

			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			output_stream = new PrintStream(userSocket.getOutputStream());
		
			userName = input_stream.readLine();
			userName = userName.substring("#join ".length());



			/* Welcome the new user. */
			output_stream.println("#welcome");
			count = 0;
			while (count < maxUsersCount){
					if(threads[count] == null || threads[count] == this){
						count++;
					}else{
					threads[count].output_stream.println("#newuser " + userName);
					count++;
					}
			}
			for(i = 0; i < maxUsersCount; i++){
				if(threads[i] == this){
					user_number = i;
					break;
				}
			}



			/* Start the conversation. */
			while (true) {
				String incoming_message = input_stream.readLine().trim();
				if(incoming_message.startsWith("#status ")){
					String user_from = userName;
					String user_message = incoming_message.substring("#status ".length());//slice string for msg
					output_stream.println("#statusPosted");//send status posted to user who sent
					count = 0;
					while (count < maxUsersCount){
						if(threads[count] == null || threads[count] == this || !friends[count]){//same as part 2 but add friends condition
							count++;
						}else{
							threads[count].output_stream.println("#newStatus " + user_from + " " + user_message);//send msg to friends
							count ++;
						}
					}
				}else if(incoming_message.equals("#Bye")){
					output_stream.println("#Bye");
					String leaving_user = userName;
					output_stream.println("#Bye");
					count = 0;
					while (count < maxUsersCount){
						if(threads[count] == null || threads[count] == this){
							count++;
						}else{
							threads[count].output_stream.println("#Leave " + leaving_user);
							count++;
						}
					}
					break;
				}else if(incoming_message.startsWith("#friendme ")){//protocol for friend request
					String requester = this.userName;//requester username
					String requestee = incoming_message.substring("#friendme ".length());//requestee username
					for(i = 0; i < maxUsersCount; i++){	
						if(threads[i] == null ||!(threads[i].userName.equals(requestee))){//we want to ignore if these are true
						}else if(threads[i].userName.equals(requestee) && i != user_number){//send if this is true
							threads[i].output_stream.println("#friendme " + requester);
							break;//break out of for loop
						}
					}
				}else if(incoming_message.startsWith("#friends ")){//friend request accept protocol
					String friend1 = this.userName;
					String friend2 = incoming_message.substring("#friends ".length());
					for(i = 0; i < maxUsersCount; i++){	
						 if(threads[i] != null && threads[i].userName.equals(friend2)){//send accept protocol to both users
							threads[i].output_stream.println("#OKfriends " + friend1 + " " + friend2);
							output_stream.println("#OKfriends " + friend1 + " " + friend2);
							friends[i] = true;
							threads[i].friends[user_number] = true;
							break;
						}
						
					}
				}else if(incoming_message.startsWith("#DenyFriendRequest ")){//send denied protocol to rejected user
					String rejected = incoming_message.substring("#DenyFriendRequest ".length());
					for(i = 0; i < maxUsersCount; i++){
						if(threads[i] != null && threads[i].userName.equals(rejected)){
							threads[i].output_stream.println("#FriendRequestDenied " + userName);
							break;
						}
					}

				}else if(incoming_message.startsWith("#unfriend ")){//send unfriend protocol to user to unfriend
					String unfriended = incoming_message.substring("#unfriend ".length());
					for(i = 0; i < maxUsersCount; i++){
						if(threads[i] != null && threads[i].userName.equals(unfriended)){
							threads[i].output_stream.println("#NotFriends " + unfriended + " " + userName);
							threads[i].friends[user_number] = false;//reset friends arrays
							friends[i] = false;
							break;
						}
					}
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
