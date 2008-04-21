/* *********************************************************************** *
 * project: org.matsim.*
 * Link.java
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
import org.matsim.basic.v01.BasicNode;
import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;

public class LinkImpl extends AbstractLink {

	private final static Logger log = Logger.getLogger(LinkImpl.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////


	private double flowCapacity;

	private double freespeedTravelTime;



	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public LinkImpl(IdI id, BasicNode from, BasicNode to, NetworkLayer network, double length, double freespeed, double capacity, double lanes) {
		super(network, id, from, to);
		super.length = length;
		super.freespeed = freespeed;
		super.capacity = capacity;
		super.permlanes = lanes;
		init();
	}

	//////////////////////////////////////////////////////////////////////
	// init methods
	//////////////////////////////////////////////////////////////////////

	private void init() {
		this.euklideanDist = ((Node)this.from).getCoord().calcDistance(((Node)this.to).getCoord());
		calcFlowCapacity();
		this.freespeedTravelTime = this.getLength()
		/ this.getFreespeed(Time.UNDEFINED_TIME);

		// do some semantic checks
		if (this.from.equals(this.to)) { log.warn(this + "[from=to=" + this.to + " link is a loop]"); }
		if (this.freespeed <= 0.0) { Gbl.errorMsg(this+"[freespeed="+this.freespeed+" not allowed]"); }
		if (this.capacity <= 0.0) { Gbl.errorMsg(this+"[capacity="+this.capacity+" not allowed]"); }
		if (this.permlanes < 1) { Gbl.errorMsg(this+"[permlanes="+this.permlanes+" not allowed]"); }

	}

	
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////
	
	private void calcFlowCapacity() {
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
//		log.debug("capacity period: " + capacityPeriod);
			this.flowCapacity = this.capacity / capacityPeriod;
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

	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	
	public double getFreespeedTravelTime(double time) {
		return this.freespeedTravelTime;
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
	
	protected void setFreespeedTravelTime(double freespeedTravelTime) {
		this.freespeedTravelTime = freespeedTravelTime;
	}




}
