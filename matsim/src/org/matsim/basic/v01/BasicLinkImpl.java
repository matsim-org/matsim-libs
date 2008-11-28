/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLink.java
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

package org.matsim.basic.v01;

import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.AbstractLocation;

public class BasicLinkImpl
// <L extends BasicLinkI, N extends BasicNodeI > // and then change all BasicLink/BasicNode below into L and N
extends AbstractLocation
implements BasicLink
{
	protected BasicNode from = null;
	protected BasicNode to = null;

	protected double length = Double.NaN;
	protected double freespeed = Double.NaN;
	protected double capacity = Double.NaN;
	protected double permlanes = Double.NaN;

	// TODO [balmermi] A link exists only iff a to- and a from node is defined.
	// Furthermore: Since a BasicLink is a location, and a location is a geographic
	// object, the BasicLink must contains geographic info. Since this must be defined
	// by the to- and from-node, they HAVE to contain a coordinate. (see also BasicNode)
	// If this is not O.K., then the BasicLink must not extend Location.
	public BasicLinkImpl(final NetworkLayer network, final Id id, final BasicNode from, final BasicNode to) {
		super(network, id, 
				new CoordImpl(0.5*(from.getCoord().getX() + to.getCoord().getX()), 0.5*(from.getCoord().getY() + to.getCoord().getY()))
		);
		this.from = from;
		this.to = to;
	}

	// TODO [balmermi] For simplicity, we calculate only the distance to the center
	// of that link. A better version is implemented in org.matsim.demandmodeling.network.Link.
	// It would be better to implement the version in Link here and remove the one in Link.
	@Override
	public double calcDistance(final Coord coord) {
		return this.center.calcDistance(coord);
	}

	public BasicNode getFromNode() {
		return this.from;
	}

	public BasicNode getToNode() {
		return this.to;
	}

	public boolean setFromNode(final BasicNode node) {
		this.from = node;
		return true;
	}

	public boolean setToNode(final BasicNode node) {
		this.to = node;
		return true;
	}
	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 *
 	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	public double getCapacity(final double time) {
		return this.capacity;
	}

	public void setCapacity(final double capacity) {
		this.capacity = capacity;
	}
	/**
	 * This method returns the freespeed velocity in meter per seconds.
	 *
	 * @param time - the current time
	 * @return freespeed
	 */
	public double getFreespeed(final double time) {
		return this.freespeed;
	}
	/**
	 * Sets the freespeed velocity of the link in meter per seconds.
	 */
	public void setFreespeed(final double freespeed) {
		this.freespeed = freespeed;
	}

	public double getLength() {
		return this.length;
	}

	public void setLength(final double length) {
		this.length = length;
	}

	//TODO permlanes should be refectored to lanes or getLanes() and so on to getPermLanes() [GL]

	public double getLanes(final double time) {
		return this.permlanes;
	}

	public int getLanesAsInt(final double time) {
		return Math.round((float)Math.max(this.permlanes,1.0d));
	}

	public void setLanes(final double lanes) {
		this.permlanes = lanes;
	}

	public LocationType getLocationType() {
		return LocationType.LINK;
	}
}
