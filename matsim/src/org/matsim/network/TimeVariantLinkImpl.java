/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkImpl.java
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
import org.matsim.basic.v01.BasicLink;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.gnuclasspath.TreeMap;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.misc.ResizableArray;
import org.matsim.utils.misc.Time;



public class TimeVariantLinkImpl extends BasicLink implements Link {

	private final static Logger log = Logger.getLogger(TimeVariantLinkImpl.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected String type = null;
	protected String origid = null;
	private final ResizableArray<Object> roles = new ResizableArray<Object>(5);

	TreeMap<Double, Double> freespeedEvents;

	protected final double euklideanDist;

	private double flowCapacity;

	private final double freespeedTravelTime;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected TimeVariantLinkImpl(final NetworkLayer network, final String id,
            final Node from, final Node to, final String length,
            final String freespeed, final String capacity,
            final String permlanes, final String origid, final String type) {
	super(network,new Id(id),from,to);
	this.length = Double.parseDouble(length);
	this.freespeed = Double.parseDouble(freespeed);
	this.capacity = Double.parseDouble(capacity);
	this.permlanes = Double.parseDouble(permlanes);
	this.origid = origid;
	this.type = type;
	this.euklideanDist = ((Node)this.from).getCoord().calcDistance(((Node)this.to).getCoord());
	this.freespeedTravelTime = this.getLength()
	/ this.getFreespeed(Time.UNDEFINED_TIME);

	initNetworkChangeEvents();
	calcFlowCapacity();
	// do some semantic checks
	if (this.from.equals(this.to)) { log.warn(this + "[from=to=" + this.to + " link is a loop]"); }
	if (this.freespeed <= 0.0) { Gbl.errorMsg(this+"[freespeed="+freespeed+" not allowed]"); }
	if (this.capacity <= 0.0) { Gbl.errorMsg(this+"[capacity="+capacity+" not allowed]"); }
	if (this.permlanes < 1) { Gbl.errorMsg(this+"[permlanes="+permlanes+" not allowed]"); }
}

//////////////////////////////////////////////////////////////////////
// init methods
//////////////////////////////////////////////////////////////////////
	private void initNetworkChangeEvents() {
		this.freespeedEvents = new TreeMap<Double, Double>();
		this.freespeedEvents.put(-1., this.freespeed); // make sure that freespeed is set to 'default' freespeed as long as no change event occurs

	}


//////////////////////////////////////////////////////////////////////
// calc methods
//////////////////////////////////////////////////////////////////////



private void calcFlowCapacity() {
	int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
//	log.debug("capacity period: " + capacityPeriod);
		this.flowCapacity = this.capacity / capacityPeriod;
}

/* (non-Javadoc)
* @see org.matsim.network.Link#calcDistance(org.matsim.utils.geometry.CoordI)
*/
@Override
public final double calcDistance(final CoordI coord) {
	CoordI fc = this.from.getCoord();
	CoordI tc =  this.to.getCoord();
	double tx = tc.getX();    double ty = tc.getY();
	double fx = fc.getX();    double fy = fc.getY();
	double zx = coord.getX(); double zy = coord.getY();
	double ax = tx-fx;        double ay = ty-fy;
	double bx = zx-fx;        double by = zy-fy;
	double la2 = ax*ax + ay*ay;
	double lb2 = bx*bx + by*by;
	if (la2 == 0.0) { return Math.sqrt(lb2); } // from == to
	double xla = ax*bx+ay*by; // scalar product
	if (xla < 0.0) { return Math.sqrt(lb2); }
	else if (xla > la2) { double cx = zx-tx; double cy = zy-ty; return Math.sqrt(cx*cx+cy*cy); }
	else { return Math.sqrt(lb2-xla*xla/la2); // lb2-xla*xla/la2 = lb*lb-x*x
	}

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


/**
 * This method returns the freespeed for current time
 * @param time - the current time
 * @Override {@link org.matsim.basic.v01.BasicLink.getFreespeed}
 */
@Override
public double getFreespeed(double time) {
	return this.freespeedEvents.floorEntry(time).getValue();
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

/* (non-Javadoc)
* @see org.matsim.network.Link#getFlowCapacity()
*/
public final double getFlowCapacity() {
	return this.flowCapacity;
}

//////////////////////////////////////////////////////////////////////
// set methods
//////////////////////////////////////////////////////////////////////

/**
 * This method add a new freespeed change event. If there already exist an event for the given time, then
 * the old value will be overwritten.
 *
 *  @param time - the time on which the event occurs
 *  @param freespeed - the new freespeed
 */
public void addFreespeedEvent(final double time, final double freespeed) {
	this.freespeedEvents.put(time, freespeed);
}

/* (non-Javadoc)
* @see org.matsim.network.Link#setOrigId(java.lang.String)
*/
public final void setOrigId(final String id) {
	Gbl.errorMsg("location id=" + id + ": there is no orig_id anymore!");
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

//////////////////////////////////////////////////////////////////////
// print methods
//////////////////////////////////////////////////////////////////////

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

public double getFreespeedTravelTime() {
	return this.freespeedTravelTime;
}
}
