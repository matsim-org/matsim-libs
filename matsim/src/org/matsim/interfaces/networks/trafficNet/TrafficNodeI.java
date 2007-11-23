/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficNodeI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.networks.trafficNet;

import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.geometry.CoordI;

/**
 * A network node representation from a traffic engineering point of view.
 */
public interface TrafficNodeI extends BasicNodeI {

    /**
     * Builds this node's internal structure. Requires that the adjacent links
     * have been properly registered with this node, as it is ensured by a call
     * to <code>BasicNetworkI.connect()</code>.
     * <p>
     * TODO: Check if this makes sense. (There is no connect() in BasicNodeI.)
     */
    public void build();

    /**
     * Sets the turning movement priority from <code>inLink</code> to
     * <code>outLink</code>.
     * 
     * @param inLink
     *            the upstream link of this turning movement
     * @param outLink
     *            the downstream link of this turning movement
     * @param priority
     *            the turning movement priority
     * 
     * @throws IllegalArgumentException
     *             if <code>inLink</code> or <code>outLink</code> is
     *             <code>null</code> or not properly connected to this node
     * @throws IllegalArgumentException
     *             if <code>priority</code> is negative
     */
    public void setPriority(TrafficLinkI inLink, TrafficLinkI outLink,
            double priority);

    /**
     * Sets the maximal flow rate (in vehicles per second) from
     * <code>inLink</code> to <code>outLink</code>.
     * 
     * @param inLink
     *            the upstream link of this turning movement
     * @param outLink
     *            the downstream link of this turning movement
     * @param maxFlow_veh_s
     *            the maximal flow in vehicles per second
     * 
     * @throws IllegalArgumentException
     *             if <code>inLink</code> or <code>outLink</code> is
     *             <code>null</code> or not properly connected to this node
     * @throws IllegalArgumentException
     *             if <code>priority</code> is negative
     */
    public void setMaxFlow_veh_s(TrafficLinkI inLink, TrafficLinkI outLink,
            double maxFlow_veh_s);

    /**
     * Returns the priority of the turning movement from <code>inLink</code>
     * to <code>outLink</code>.
     * 
     * @param inLink
     *            the upstream link of this turning movement
     * @param outLink
     *            the downstream link of this turning movement
     * 
     * @return the required turning movement priority
     * 
     * @throws IllegalArgumentException
     *             if <code>inLink</code> or <code>outLink</code> is
     *             <code>null</code> or not properly connected to this node
     */
    public double getPriority(TrafficLinkI inLink, TrafficLinkI outLink);

    /**
     * Returns the maximal flow (in vehicles per second) of the turning movement
     * from <code>inLink</code> to <code>outLink</code>.
     * 
     * @param inLink
     *            the upstream link of this turning movement
     * @param outLink
     *            the downstream link of this turning movement
     * 
     * @return the required maximal flow
     * 
     * @throws IllegalArgumentException
     *             if <code>inLink</code> or <code>outLink</code> is
     *             <code>null</code> or not properly connected to this node
     */
    public double getMaxFlow_veh_s(TrafficLinkI inLink, TrafficLinkI outLink);

    
    /**
     * Sets the coordinate of the node.
     * 
     * @param coord
     * 						the coordinate this node is placed
     */
    public void setCoord(CoordI coord);
    
    /**
     * Returns the coordinate of the node.
     * 
     * @return the coordinate of the node.
     */
    public CoordI getCoord();
}