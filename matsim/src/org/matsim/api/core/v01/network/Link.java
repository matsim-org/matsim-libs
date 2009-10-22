package org.matsim.api.core.v01.network;

import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.world.Location;

/**
 * This interface deliberately does NOT have a back pointer ...
 * ... since, at this level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)
 */
public interface Link extends BasicLink, Location {
	
	/**
	 * @return this link's downstream node
	 */
	public Node getToNode();

	/**
	 * @return this link's upstream node
	 */
	public Node getFromNode();
	
}