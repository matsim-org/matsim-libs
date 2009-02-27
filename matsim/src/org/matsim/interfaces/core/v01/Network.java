/* *********************************************************************** *
 * project: org.matsim.*
 * Network.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.interfaces.core.v01;

import java.util.Collection;

import org.matsim.interfaces.basic.v01.BasicNetwork;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NetworkFactory;

// FIXME [MR] still imports NetworkFactory from outside interface
public interface Network extends BasicNetwork<Node, Link> {

	/**
	 * @param capPeriod the capacity-period in seconds
	 */
	public void setCapacityPeriod(final double capPeriod);

	public void setEffectiveCellSize(final double effectiveCellSize);

	public void setEffectiveLaneWidth(final double effectiveLaneWidth);

	public int getCapacityPeriod();

	public double getEffectiveCellSize();

	public double getEffectiveLaneWidth();

	public Node getNode(final Id id);

	public Link getLink(final Id linkId);

	/**
	 * Finds the node nearest to <code>coord</code>
	 *
	 * @param coord the coordinate to which the closest node should be found
	 * @return the closest node found, null if none
	 */
	public Node getNearestNode(final Coord coord);

	/**
	 * finds the nodes within distance to <code>coord</code>
	 *
	 * @param coord the coordinate around which nodes should be located
	 * @param distance the maximum distance a node can have to <code>coord</code> to be found
	 * @return all nodes within distance to <code>coord</code>
	 */
	public Collection<Node> getNearestNodes(final Coord coord, final double distance);

	public NetworkFactory getFactory();

	/**
	 * removes a link from the network.<p>
	 *
	 * In case <tt>link</tt> exists, it first unlinks it from the two
	 * incident nodes and then removes it from the link set of the network.
	 *
	 * @param link Link to be removed.
	 * @return <tt>true</tt> if the specified link is part of the network and
	 * is successfully removed.
	 */
	public boolean removeLink(final Link link);

	/**
	 * removes a node from the network.<p>
	 *
	 * In case <tt>node</tt> exists, it first removed all incident links of
	 * <tt>node</tt> and then removes <tt>node</tt> from the link set
	 * and from the <tt>nodeQuadTree</tt>---if instantiated---of the network.<p>
	 *
	 * NOTE: if one of the incident links of <tt>node</tt> cannot be removed
	 * properly, the process crashes.
	 *
	 * @param node Node to be removed.
	 * @return <tt>true</tt> if the specified node is part of the network and
	 * is successfully removed AND all incident links are removed successfully
	 */
	public boolean removeNode(final Node node);

}
