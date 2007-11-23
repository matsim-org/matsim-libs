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

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Coord;
import org.matsim.world.Location;

public class BasicLink 
// <L extends BasicLinkI, N extends BasicNodeI > // and then change all BasicLink/BasicNode below into L and N
extends Location 
implements BasicLinkI
{
	protected BasicNode from = null;
	protected BasicNode to = null;

	protected double length = Double.NaN;
	protected double freespeed = Double.NaN;
	protected double capacity = Double.NaN;
	protected int permlanes = Integer.MIN_VALUE;

	// TODO [balmermi] A link exists only iff a to- and a from node is defined.
	// Furthermore: Since a BasicLink is a location, and a location is a geographic
	// object, the BasicLink must contains geographic info. Since this must be defined
	// by the to- and from-node, they HAVE to contain a coordinate. (see also BasicNode)
	// If this is not O.K., then the BasicLink must not extend Location.
	public BasicLink(NetworkLayer network, IdI id, BasicNode from, BasicNode to) {
		super(network,id,0.5*(from.getCoord().getX()+to.getCoord().getX()),
		                 0.5*(from.getCoord().getY()+to.getCoord().getY()));
		this.from = from;
		this.to = to;
	}

	// TODO [balmermi] see above why...
	@Deprecated
	public BasicLink(String id) {
		super(id);
	}

	// TODO [balmermi] see above why...
	@Deprecated
	public BasicLink(NetworkLayer network, String id, Coord coord) {
		super(network, id, coord);
	}

	// TODO [balmermi] For simplicity, we calculate only the distance to the center
	// of that link. A better version is implemented in org.matsim.demandmodeling.network.Link.
	// It would be better to implement the version in Link here and remove the one in Link.
	@Override
	public double calcDistance(CoordI coord) {
		return this.center.calcDistance(coord);
	}
	
	public BasicNodeI getFromNode() {
		return from;
	}

	public BasicNodeI getToNode() {
		return to;
	}

	public boolean setFromNode(BasicNodeI node) {
		this.from = (BasicNode)node;
		return true;
	}

	public boolean setToNode(BasicNodeI node) {
		this.to = (BasicNode)node;
		return true;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public double getFreespeed() {
		return freespeed;
	}

	public void setFreespeed(double freespeed) {
		this.freespeed = freespeed;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public int getLanes() {
		return this.permlanes;
	}

	public void setLanes(int lanes) {
		this.permlanes = lanes;
	}

}
