package playground.sergioo.NetworksMatcher;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public interface Region {
	
	//Methods
	public boolean isInside(Node node);
	public boolean isInside(Link node);
	
}
