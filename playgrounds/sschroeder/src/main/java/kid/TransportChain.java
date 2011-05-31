package kid;

import java.util.HashMap;
import java.util.Map;

public class TransportChain {
	
	private int id;
	
	private Map<String, String> chainAttributes = new HashMap<String, String>();

	public int getId() {
		return id;
	}

	public TransportChain(int transportChainId) {
		super();
		this.id = transportChainId;
	}

	public Map<String, String> getAttributes() {
		return chainAttributes;
	}

}
