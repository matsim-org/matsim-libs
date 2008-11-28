/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLink.java
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

package org.matsim.network;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.utils.geometry.Coord;

public class LinkImpl extends BasicLinkImpl implements Link {

	private final static Logger log = Logger.getLogger(LinkImpl.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double flowCapacity;

	private final double freespeedTravelTime;

	protected String type = null;

	protected String origid = null;

	protected double euklideanDist;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public LinkImpl(final Id id, final BasicNode from, final BasicNode to,
			final NetworkLayer network, final double length, final double freespeed, final double capacity, final double lanes) {
		super(network, id, from, to);

		super.length = length;
		super.freespeed = freespeed;
		super.capacity = capacity;
		super.permlanes = lanes;

		this.freespeedTravelTime = length / freespeed;
		this.flowCapacity = this.capacity / ((NetworkLayer)this.getLayer()).getCapacityPeriod();

		this.euklideanDist = this.from.getCoord().calcDistance(this.to.getCoord());

		// do some semantic checks
		if (this.from.equals(this.to)) { log.warn("[from=to=" + this.to + " link is a loop]"); }
		/*
		 * I see no reason why a freespeed and a capacity of zero should not be
		 * allowed! joh 9may2008
		 */
		if (this.freespeed <= 0.0) { log.warn("[freespeed="+this.freespeed+" not allowed]"); }
		if (this.capacity <= 0.0) { log.warn("[capacity="+this.capacity+" not allowed]"); }
		if (this.permlanes < 1) { log.warn("[permlanes="+this.permlanes+" not allowed]"); }
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final double calcDistance(final Coord coord) {
		Coord fc = this.from.getCoord();
		Coord tc =  this.to.getCoord();
		double tx = tc.getX();    double ty = tc.getY();
		double fx = fc.getX();    double fy = fc.getY();
		double zx = coord.getX(); double zy = coord.getY();
		double ax = tx-fx;        double ay = ty-fy;
		double bx = zx-fx;        double by = zy-fy;
		double la2 = ax*ax + ay*ay;
		double lb2 = bx*bx + by*by;
		if (la2 == 0.0) {  // from == to
			return Math.sqrt(lb2);
		}
		double xla = ax*bx+ay*by; // scalar product
		if (xla <= 0.0) {
			return Math.sqrt(lb2);
		}
		if (xla >= la2) {
			double cx = zx-tx;
			double cy = zy-ty;
			return Math.sqrt(cx*cx+cy*cy);
		}
		// lb2-xla*xla/la2 = lb*lb-x*x
		double tmp = xla*xla;
		tmp = tmp/la2;
		tmp = lb2 - tmp;
		// tmp can be slightly negativ, likely due to rounding errors (coord lies on the link!). Therefore, use at least 0.0
		tmp = Math.max(0.0, tmp);
		return Math.sqrt(tmp);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final Node getFromNode() {
		return (Node)this.from;
	}

	@Override
	public final Node getToNode() {
		return (Node)this.to;
	}

	public double getFreespeedTravelTime(final double time) {
		return this.freespeedTravelTime;
	}

	public double getFlowCapacity(final double time) {
		return this.flowCapacity;
	}

	public final String getOrigId() {
		return this.origid;
	}

	public final String getType() {
		return this.type;
	}

	public final double getEuklideanDistance() {
		return this.euklideanDist;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigId(final String id) {
		this.origid = id;
	}

	public void setType(final String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return super.toString() +
		"[from_id=" + this.from.getId() + "]" +
		"[to_id=" + this.to.getId() + "]" +
		"[length=" + this.length + "]" +
		"[freespeed=" + this.freespeed + "]" +
		"[capacity=" + this.capacity + "]" +
		"[permlanes=" + this.permlanes + "]" +
		"[origid=" + this.origid + "]" +
		"[type=" + this.type + "]";
	}
}
