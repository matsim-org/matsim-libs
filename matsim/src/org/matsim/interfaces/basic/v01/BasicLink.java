/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLinkI.java
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

package org.matsim.interfaces.basic.v01;

import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.misc.Time;

/**
 * A topological representation of a network link.
 */
public interface BasicLink {

	/**
	 * Returns a non-<code>null</code> instance of <code>IdI</code> that
	 * uniquely identifies this object.
	 *
	 * @return this object's identifier
	 */
	public Id getId();

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
	public boolean setFromNode(BasicNode node);

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
	public boolean setToNode(BasicNode node);

	/**
	 * Returns this link's upstream node. Must not return <code>null</code>.
	 *
	 * @return this link's upstream node
	 */
	public BasicNode getFromNode();

	/**
	 * Returns this link's downstream node. Must not return <code>null</code>.
	 *
	 * @return this link's downstream node
	 */
	public BasicNode getToNode();


	public double getLength();

	public double getLanes(double time);

	public int getLanesAsInt(double time);

	public double getFreespeed(final double time);

	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time the time at which the capacity is requested. Use {@link Time#UNDEFINED_TIME} to get the default value.
	 * @return the capacity per network's capperiod timestep
	 * 
	 * @see Network#getCapacityPeriod()
	 */
	public double getCapacity(double time);

	public void setFreespeed(double freespeed);

	public void setLength(double length);

	public void setLanes(double lanes);

	public void setCapacity(double capacity);

}