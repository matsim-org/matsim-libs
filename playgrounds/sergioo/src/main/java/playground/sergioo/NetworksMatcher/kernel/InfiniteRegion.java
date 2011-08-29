package playground.sergioo.NetworksMatcher.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class InfiniteRegion implements Region {

	
	//Attributes

	
	//Methods
	
	@Override
	public boolean isInside(Node node) {
		return true;
	}

	@Override
	public boolean isInside(Link link) {
		return true;
	}

	
}
