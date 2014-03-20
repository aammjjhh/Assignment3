import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

public class Client {

	String serverAddress;
	int serverPort;
	int randClientId;

	// constructor
	Client(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		Random generator = new Random();
		this.randClientId = generator.nextInt();
	}

	// start up the server
	public void start() {
		DatagramSocket socket = null;
		try {

			InetSocketAddress serverSocketAddress = 
					new InetSocketAddress(serverAddress, serverPort);

			// create a datagram socket, let the OS bind to an ephemeral UDP port. 
			// See http://docs.oracle.com/javase/tutorial/networking/datagrams/ 
			// for details.
			socket = new DatagramSocket();

			// send "REGISTER" to the server
			//
			// create an UDP packet that we'll send to the server
			String command = "REGISTER " + randClientId;
			DatagramPacket txPacket = new DatagramPacket(command.getBytes(),
					command.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(txPacket);

			// receive the server's response
			//
			// create an empty UDP packet
			byte[] buf = new byte[Server.MAX_PACKET_SIZE];
			DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			// call receive (this will populate the packet with the received
			// data, and the other endpoint's info)
			socket.receive(rxPacket);
			// print the payload
			String payload = new String(rxPacket.getData(), 0,
					rxPacket.getLength());
			System.out.println(payload);
			
			// send "JOIN MyGroup " + randClientId to the server
			//
			// create an UDP packet that we'll send to the server
			command = "JOIN MyGroup " + randClientId;
			txPacket = new DatagramPacket(command.getBytes(),
					command.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(txPacket);

			// receive the server's response
			//
			// create an empty UDP packet
			buf = new byte[Server.MAX_PACKET_SIZE];
			rxPacket = new DatagramPacket(buf, buf.length);
			// call receive (this will populate the packet with the received
			// data, and the other endpoint's info)
			socket.receive(rxPacket);
			// print the payload
			payload = new String(rxPacket.getData(), 0,
					rxPacket.getLength());
			System.out.println(payload);

			// send "SEND " + randClientId + " MyGroup Hi" to the server
			//
			command = "SEND " + randClientId + " MyGroup Hi";
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);

			// receive the server's response
			//
			socket.receive(rxPacket);
			// print the payload
			payload = new String(rxPacket.getData(), 0, rxPacket.getLength());
			System.out.println(payload);
			
			// send "POLL" to the server
			//
			command = "POLL " + randClientId;
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);

			// receive the server's response
			//
			socket.receive(rxPacket);
			// print the payload
			payload = new String(rxPacket.getData(), 0, rxPacket.getLength());
			System.out.println(payload);
			// send ACK
			command = "ACK " + randClientId;
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);
			
			// send "SHUTDOWN" to the server
			//
			command = "SHUTDOWN";
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);
			
			/*send user-defined message to the server
			 * have user send "REGISTER," "SEND HELLO TO ALL," or "UNREGISTER"
			 * 
			System.out.println("What would you like to say to the server?");
			Scanner console = new Scanner();
			command = console.next();
			txPacket = new DatagramPacket(command.getBytes(), command.length(),
					serverSocketAddress);
			socket.send(txPacket);

			// receive the server's response
			//
			socket.receive(rxPacket);
			// print the payload
			payload = new String(rxPacket.getData(), 0, rxPacket.getLength());
			System.out.println(payload);
			*/
		} catch (IOException e) {
			// we jump out here if there's an error
			e.printStackTrace();
		} finally {
			// close the socket
			if(socket!=null && !socket.isClosed())
				socket.close();
		}
	}

	// main method
	public static void main(String[] args) {
		int serverPort = Server.DEFAULT_PORT;
		String serverAddress = "localhost";

		// check if server address and port were given as command line arguments
		if (args.length > 0) {
			serverAddress = args[0];
		}

		if (args.length > 1) {
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid serverPort specified: " + args[0]);
				System.out.println("Using default serverPort " + serverPort);
			}
		}

		// instantiate the client
		Client client = new Client(serverAddress, serverPort);

		// start it
		client.start();
	}
}