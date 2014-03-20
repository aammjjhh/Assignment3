import java.util.List;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;

public class ClientEndPoint {
	protected final InetAddress address;
	protected final int port;
	protected final int clientId;
	protected final List<String> queue; // list of strings
	
	public ClientEndPoint(InetAddress addr, int port, int cId) {
		this.address = addr;
		this.port = port;
		this.clientId = cId;
		this.queue = Collections.synchronizedList(new ArrayList<String>()); 
	}
}