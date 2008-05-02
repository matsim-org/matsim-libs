package org.matsim.network;

import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Location;

public interface Link extends BasicLink, Location {

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
	 * @param time
	 * @return the flow capacity of this link per second
	 */
	public double getFlowCapacity(double time);

	public void setRole(final int idx, final Object role);
	
	public void setMaxRoleIndex(final int index);
	/**
	 * Get the the freespeed travel time on this links in seconds.
	 * @param time
	 * @return the freespeed travel time on this links in seconds
	 */
	public double getFreespeedTravelTime(double time);

	public void setType(String type);

	public void setOrigId(String origid);
	
	public void calcFlowCapacity();

}