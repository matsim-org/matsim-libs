package org.matsim.api.core.v01.network;

import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.world.Location;

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