package playground.sergioo.NetworksMatcher.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.kernel.core.Region;

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
