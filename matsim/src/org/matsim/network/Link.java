package org.matsim.network;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Location;

public interface Link extends BasicLinkI, Location {

	public double calcDistance(final CoordI coord);

	// DS TODO try to remove these and update references
	// (for the time being, they are here because otherwise the returned type is wrong. kai)
	public Node getFromNode();

	public Node getToNode();

	public String getOrigId();

	public String getType();

	public Object getRole(final int idx);

	/** @return Returns the euklidean distance between from- and to-node. */
	public double getEuklideanDistance();

	/**
	 * This method returns the normalized capacity of the link, i.e. the
	 * capacity of vehicles per second. Be aware that it will not consider the
	 * capacity reduction factors set in the config and used in the simulation. If interested
	 * in this values, check the appropriate methods of QueueLink.
	 * @return the flow capacity of this link per second
	 */
	public double getFlowCapacity();

	public void setOrigId(final String id);

	public void setRole(final int idx, final Object role);

	public void setMaxRoleIndex(final int index);

	public double getFreespeedTravelTime();

}