package playground.sergioo.networksMatcher2012.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.networksMatcher2012.kernel.core.Region;

public class NullRegion implements Region {

	
	//Attributes

	
	//Methods

	@Override
	public boolean isInside(Node node) {
		return false;
	}

	@Override
	public boolean isInside(Link link) {
		return false;
	}

	
}
