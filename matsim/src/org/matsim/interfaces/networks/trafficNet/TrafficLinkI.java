/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficLinkI.java
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

import org.matsim.interfaces.networks.basicNet.BasicLinkI;

/**
 * A network link representation from a traffic engineering point of view.
 */
public interface TrafficLinkI extends BasicLinkI {

    /**
     * Builds this link's internal structure. Requires that the adjacent nodes
     * have been properly registered with this link, as it is ensured by a call
     * to <code>BasicNetworkI.connect()</code>.
     * <p>
     * <strike>
     * ((todo)) Check if this makes sense. There is no connect() in BasicLinkI. author=? date=?
     * <p>
     * connect() is in BasicNetworkI, not in BasicLinkI. kai, nov06
     * </strike>
     */
    public void build();

    /**
     * Sets this link's length in meters.
     * 
     * @param length_m
     *            this link's length in meters
     * 
     * @throws IllegalArgumentException
     *             if <code>length_m</code> is negative
     */
    public void setLength_m(double length_m);

    /**
     * Sets this link's maximal velocity in meters per second.
     * 
     * @param maxVel_m_s
     *            this link's maximal velocity in meters per second
     * 
     * @throws IllegalArgumentException
     *             if <code>maxVel_m_s</code> is negative
     */
    public void setMaxVel_m_s(double maxVel_m_s);

    /**
     * Sets this link's maximal flow in vehicles per second. This refers to the
     * maximal flow through the <em>entire</em> link's profile, not only
     * through a single lane.
     * 
     * @param maxFlow_veh_s
     *            this link's maximal flow in vehicles per second
     * 
     * @throws IllegalArgumentException
     *             if <code>maxFlow_veh_s</code> is negative
     */
    public void setMaxFlow_veh_s(double maxFlow_veh_s);

    /**
     * Sets this link's number of lanes.
     * 
     * @param lanes
     *            this link's number of lanes
     * 
     * @throws IllegalArgumentException
     *             if <code>lanes</code> is negative
     */
    public void setLanes(int lanes);

    /**
     * Returns this link's length in meters.
     * 
     * @return this link's length in meters
     */
    public double getLength_m();

    /**
     * Returns this link's maximal velocity in meters per second.
     * 
     * @return this link's maximal velocity in meters per second
     */
    public double getMaxVel_m_s();

    /**
     * Returns this link's maximal flow in vehicles per second.
     * 
     * @return this link's maximal flow in vehicles per second
     */
    public double getMaxFlow_veh_s();

    /**
     * Returns this link's number of lanes.
     * 
     * @return this link's number of lanes
     */
    public int getLanes();

}