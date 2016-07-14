package playground.sergioo.networksMatcher2012.kernel;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.Link;

import playground.sergioo.networksMatcher2012.kernel.core.Region;

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
