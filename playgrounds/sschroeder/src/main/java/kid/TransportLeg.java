/**
 * 
 */
package kid;

import java.util.HashMap;
import java.util.Map;

/**
 * @author stefan
 *
 */
public class TransportLeg {
	
	private int id;
	
	private Map<String, String> chainAttributes = new HashMap<String, String>();

	public int getId() {
		return id;
	}

	public Map<String, String> getAttributes() {
		return chainAttributes;
	}

	public TransportLeg(int id) {
		super();
		this.id = id;
	}
	
	

}
