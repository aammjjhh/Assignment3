import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Group {
	public Set<ClientEndPoint> members;
	String name;
	
	public Group(String n) {
		this.name = n;
		this.members = Collections.synchronizedSet(new HashSet<ClientEndPoint>());
	}
}