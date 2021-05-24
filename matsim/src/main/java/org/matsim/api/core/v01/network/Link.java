/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.Set;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * This interface deliberately does NOT have a back pointer ...
 * ... since, at this level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)
 */
public interface Link extends BasicLocation, Attributable, Identifiable<Link> {


	/**
	 * Sets this link's non-<code>null</code> upstream node.
	 *
	 * @param node
	 *            the <code>BasicNodeI</code> to be set
	 *
	 * @return <true> if <code>node</code> has been set and <code>false</code>
	 *         otherwise
	 *
	 * @throws IllegalArgumentException
	 *             if <code>node</code> is <code>null</code>
	 */
	public boolean setFromNode(Node node);

	/**
	 * Sets this link's non-<code>null</code> downstream node.
	 *
	 * @param node
	 *            the <code>BasicNodeI</code> to be set
	 *
	 * @return <code>true</code> if <code>node</code> has been set and
	 *         <code>false</code> otherwise
	 *
	 * @throws IllegalArgumentException
	 *             if <code>node</code> is <code>null</code>
	 */
	public boolean setToNode(Node node);

	/**
	 * @return this link's downstream node
	 */
	public Node getToNode();

	/**
	 * @return this link's upstream node
	 */
	public Node getFromNode();


	public double getLength();

	public double getNumberOfLanes();

	public double getNumberOfLanes(double time);

	public double getFreespeed();

	public double getFreespeed(final double time);

	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @return the capacity per network's capperiod timestep
	 *
	 * @see Network#getCapacityPeriod()
	 */
	public double getCapacity();

	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time the time at which the capacity is requested. Use {@link Double#NEGATIVE_INFINITY} to get the default value.
	 * @return the capacity per network's capperiod timestep
	 *
	 * @see Network#getCapacityPeriod()
	 */
	public double getCapacity(double time);

	public void setFreespeed(double freespeed);

	public void setLength(double length);

	public void setNumberOfLanes(double lanes);

	public void setCapacity(double capacity);

	public void setAllowedModes(Set<String> modes);

	/**
	 * @return an <b>immutable</b> set containing all transport modes that are allowed on that link.
	 */
	public Set<String> getAllowedModes();

	double getCapacityPeriod();

	default double getFlowCapacityPerSec() {
		return getCapacity() / getCapacityPeriod();
	}

	default double getFlowCapacityPerSec(double time) {
		return getCapacity(time) / getCapacityPeriod();
	}
}
