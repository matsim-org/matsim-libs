package org.matsim.contrib.map2mapmatching.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.kernel.core.Region;

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
