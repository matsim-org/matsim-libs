package playground.sergioo.networksMatcher2012.kernel.core;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.Link;

public interface Region {


	//Methods

	public boolean isInside(Node node);

	public boolean isInside(Link link);


}
