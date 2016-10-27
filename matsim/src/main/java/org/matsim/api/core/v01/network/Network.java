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

package org.matsim.api.core.v01.network;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A topological network representation.
 */
public interface Network extends MatsimToplevelContainer, Attributable {

	/**
	 * Returns the builder for network elements
	 */
	@Override
	public NetworkFactory getFactory();

	/**
	 * Returns a set of this network's nodes. This set might be empty, but it
	 * should not be <code>null</code>.
	 *
	 * @return a set of this network's nodes
	 */
	public Map<Id<Node>, ? extends Node> getNodes();

	/**
	 * Returns a set of this network's links. This set might be empty, but it
	 * should not be <code>null</code>.
	 *
	 * @return a set of this network's links
	 */
	public Map<Id<Link>, ? extends Link> getLinks();

	/**
	 * Returns the time period over which
	 * the capacity of the given links has been measured.
	 * The default is given in the dtd.  Currently (may'11) it is 1h = 3600.0 sec.
	 * <p></p>
	 * Notes:<ul>
	 * <li> There is no setter for this value since API-based network generation code should not use anything else but the default.
	 * The default is in the network dtd, but it is an attribute under "links", not under "network".
	 * </ul>
	 * @return the time period in seconds
	 */
	public double getCapacityPeriod();


	/**
	 * Returns the lane width of the network's links. The default is given in the dtd; current (may'11) it is 3.75m.
	 * <p></p>
	 * Notes:<ul>
	 * <li> There is no setter for this value since API-based network generation code should not use anything else but the default.
	 * </ul>
	 * @return the lane width in meters
	 */
	public double getEffectiveLaneWidth();


	public void addNode(Node nn);
	// "void" can still be changed into "boolean" or "Node", see "Collection" or "Map" interface.

	public void addLink(Link ll);

	/** Removes the node with the specified Id from the network, along with all links connected to that node.
	 * The return value corresponds to the behavior of {@link Map#remove(Object)}
	 *
	 * @param nodeId node to be removed
	 * @return the removed node, or <code>null</code> if no such node was found
	 */
	public Node removeNode(final Id<Node> nodeId);

	/** Removes the link with the specified Id from the network and removes it as in- or out-Links of
	 * its to- and from-Node, but does not remove any node this link is connected to.
	 * The return value corresponds to the behavior of {@link Map#remove(Object)}
	 *
	 * @param linkId node to be removed
	 * @return the removed link, or <code>null</code> if no such link was found
	 */
	public Link removeLink(final Id<Link> linkId);

	void setCapacityPeriod(double capPeriod);

	void setEffectiveCellSize(double effectiveCellSize);

	void setEffectiveLaneWidth(double effectiveLaneWidth);

	void setName(String name);

	String getName();

	double getEffectiveCellSize();

}
