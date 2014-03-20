import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength()).trim();

		// dispatch request handler functions based on the payload's prefix
		
		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload.
					substring("REGISTER".length() + 1, payload.length()).trim());
			return;
		}
		
		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload.
					substring("JOIN".length() + 1, payload.length()).trim());
			return;
		}
		
		if (payload.startsWith("SEND")) {
			onSendRequested(payload.
					substring("SEND".length() + 1, payload.length()).trim());
			return;
		}
		
		if (payload.startsWith("POLL")) {
			onPollRequested(payload.
					substring("POLL".length() + 1, payload.length()).trim());
			return;
		}

		if (payload.startsWith("ACK")) {
			onAckRequested(payload.
					substring("ACK".length() + 1, payload.length()).trim());
			return;
		}

		if (payload.startsWith("SHUTDOWN")) {
			onShutdownRequested();
			return;
		}

		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest();
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}

	// REGISTER the client
	private void onRegisterRequested(String payload) {
		int randClientId = Integer.parseInt(payload);
		
		// get the address of the sender from the rxPacket
		InetAddress address = this.rxPacket.getAddress();
		// get the port of the sender from the rxPacket
		int port = this.rxPacket.getPort();
		
		// see if randClientId is already mapped to a clientEndPoint
		if (!(Server.clientEndPoints.containsKey(randClientId))) { // if not...
			// ...make a new clientEndPoint, and register it!
			Server.clientEndPoints.put(randClientId, new ClientEndPoint(address, port, randClientId));
		}

		// tell client we're OK
		try {
			send("REGISTERED\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// checks to see if the client is registered
	private boolean isRegistered(int randClientId) {
		// see if randClientId is already mapped to a clientEndPoint
		System.out.println(randClientId);
		if (Server.clientEndPoints.containsKey(randClientId)) { // if so...
			return true;
		} else { // if not...		
			// ...the client is not yet registered--tell the client
			try {
				send("This client is not yet registered\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	// JOIN the client to a Group
	private void onJoinRequested(String payload) {
		// parse the payload into myGroupName and randClientId
		Scanner tokenizer = new Scanner(payload);
		String myGroupName = tokenizer.next();
		String randClientIdString = tokenizer.next();
		int randClientId = Integer.parseInt(randClientIdString);
		
		ClientEndPoint myCEP;
		
		// see if randClientId is already mapped to a clientEndPoint
		if (isRegistered(randClientId)) { // if so...
			
			// ...get the clientEndPoint
			myCEP = Server.clientEndPoints.get(randClientId);
			
			// ...and add it to the Group
			// see if the Group already exists
			if (Server.groups.containsKey(myGroupName)) { // if so...
				// ...add the clientEndPoint to the Group
				Server.groups.get(myGroupName).members.add(myCEP);
			} else { // if not...
				// ...make a new Group with the new myGroupName
				Server.groups.put(myGroupName, new Group(myGroupName));
				// ...and add the clientEndPoint to the Group
				Server.groups.get(myGroupName).members.add(myCEP);
			}
			
			try {
				send("JOINED " + myGroupName + "\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// checks to see if the client is part of the Group
	private boolean isJoined(String myGroupName, int randClientId) {
		// see if the Group already exists
		if (Server.groups.containsKey(myGroupName)) { // if so...
			// ...get the clientEndPoint
			ClientEndPoint myCEP = Server.clientEndPoints.get(randClientId);
			// ...and see if the clientEndPoint is in the Group
			if (Server.groups.get(myGroupName).members.contains(myCEP)) {
				return true;
			} else {
				try {
					send("The client is not in " + myGroupName + "\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
			
		} else { // if not...
			try {
				send(myGroupName + " is not an existing Group. The client never joined.\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	// SEND the message to the members of the Group
	private void onSendRequested(String payload) {
		// parse the payload into randClientId, myGroupName, and message 
		Scanner tokenizer = new Scanner(payload);
		String randClientIdString = tokenizer.next();
		int randClientId = Integer.parseInt(randClientIdString);
		String myGroupName = tokenizer.next();
		String message = tokenizer.next();
		
		if (isRegistered(randClientId)) {
			if (isJoined(myGroupName, randClientId)) {
				for (ClientEndPoint clientEndPoint : Server.groups.get(myGroupName).members) {
					clientEndPoint.queue.add(message);
				} 
				try {
					send("Stored " + message + "\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// POLL the client
	private void onPollRequested(String payload) {
		int randClientId = Integer.parseInt(payload);
		
		ClientEndPoint myCEP;
		
		if (isRegistered(randClientId)) {
			myCEP = Server.clientEndPoints.get(randClientId);
			
			if (!(myCEP.queue.isEmpty())) {
				String message = myCEP.queue.get(0);
				try {
					send("MESSAGE: " + "\"" + message + "\"" + "\n", myCEP.address, myCEP.port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					send("No message is available\n", myCEP.address, myCEP.port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// ACK received - remove the head of the queue
	private void onAckRequested(String payload) {
		int randClientId = Integer.parseInt(payload);
		
		if (isRegistered(randClientId)) {
			Server.clientEndPoints.get(randClientId).queue.remove(0);
		}
		// for debugging purposes
		System.out.println("ACKNOWLEDGED!");
	}
	
	// SHUTDOWN
	private void onShutdownRequested() {
		// close the socket
		if (this.rxPacket.getAddress().isAnyLocalAddress()) {
			this.socket.close();
		} else {
			System.out.println("SHUTDOWN not allowed from remote IP");
		}
	}

	// the client sent an invalid request
	private void onBadRequest() {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}