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
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.misc.ResizableArray;
import org.matsim.utils.misc.Time;

/**
 * @author laemmel
 * @author illenberger
 *
 */
public class LinkImpl extends BasicLinkImpl implements Link {
	
	private final static Logger log = Logger.getLogger(LinkImpl.class);
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private double flowCapacity;
	
	private double freespeedTravelTime;
	
	protected String type = null;

	protected String origid = null;
	
	protected double euklideanDist;
	
	private final ResizableArray<Object> roles = new ResizableArray<Object>(5);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public LinkImpl(Id id, BasicNode from, BasicNode to,
			NetworkLayer network, double length, double freespeed, double capacity, double lanes) {
		super(network, id, from, to);
		
		super.length = length;
		super.freespeed = freespeed;
		super.capacity = capacity;
		super.permlanes = lanes;
		
		this.freespeedTravelTime = this.getLength()	/ this.getFreespeed(Time.UNDEFINED_TIME);
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

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#calcDistance(org.matsim.utils.geometry.CoordI)
	 */
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
		if (la2 == 0.0) { return Math.sqrt(lb2); } // from == to
		double xla = ax*bx+ay*by; // scalar product
		if (xla <= 0.0) { return Math.sqrt(lb2); }
		else if (xla >= la2) {
			double cx = zx-tx;
			double cy = zy-ty;
			return Math.sqrt(cx*cx+cy*cy); }
		else { // lb2-xla*xla/la2 = lb*lb-x*x
			double tmp = xla*xla;
			tmp = tmp/la2;
			tmp = lb2 - tmp;
			// tmp can be slightly negativ, likely due to rounding errors (coord lies on the link!). Therefore, use at least 0.0
			tmp = Math.max(0.0, tmp);
			return Math.sqrt(tmp);
		}
	}
	
	/**
	 * @deprecated
	 */
	public void calcFlowCapacity() {
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		
		this.flowCapacity = this.capacity / capacityPeriod;
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	// DS TODO try to remove these and update references
	// (for the time being, they are here because otherwise the returned type is wrong. kai)
	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getFromNode()
	 */
	@Override
	public final Node getFromNode() {
		return (Node)this.from;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getToNode()
	 */
	@Override
	public final Node getToNode() {
		return (Node)this.to;
	}
	
	public double getFreespeedTravelTime(double time) {
		return this.freespeedTravelTime;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getFlowCapacity()
	 */
	public double getFlowCapacity(double time) {
		return this.flowCapacity;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getOrigId()
	 */
	public final String getOrigId() {
		return this.origid;
	}


	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getType()
	 */
	public final String getType() {
		return this.type;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getRole(int)
	 */
	public final Object getRole(final int idx) {
		if (idx < this.roles.size() ) {
			return this.roles.get(idx);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getEuklideanDistance()
	 */
	public final double getEuklideanDistance() {
		return this.euklideanDist;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigId(final String id) {
		this.origid = id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#setRole(int, java.lang.Object)
	 */
	public final void setRole(final int idx, final Object role) {
		if (idx > this.roles.size()) {
			this.roles.resize(idx+1);
		}
		this.roles.set(idx, role);
	}

	public void setMaxRoleIndex(final int index) {
		this.roles.resize(index+1);
	}

	public void setType(String type) {
		this.type = type;
	}
	
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
