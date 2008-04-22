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

import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.utils.collections.gnuclasspath.TreeMap;




public class TimeVariantLinkImpl extends AbstractLink {

	private final static Logger log = Logger.getLogger(TimeVariantLinkImpl.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////



	private TreeMap<Double, Double> freespeedEvents;
	private TreeMap<Double, Double> freespeedTravelTime;
	
	private PriorityQueue<NetworkChangeEvent> changeEvents;

	private double flowCapacity;



	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	public TimeVariantLinkImpl(Id id, BasicNode from, BasicNode to, NetworkLayer network, double length, double freespeed, double capacity, double lanes) {
		super(network, id, from, to);
		super.length = length;
		super.freespeed = freespeed;
		super.capacity = capacity;
		super.permlanes = lanes;
		init();
	}


//	init methods
	public void init() {
		this.euklideanDist = ((Node)this.from).getCoord().calcDistance(((Node)this.to).getCoord());


		initNetworkChangeEvents();
		calcFlowCapacity();
		// do some semantic checks
		if (this.from.equals(this.to)) { log.warn(this + "[from=to=" + this.to + " link is a loop]"); }
		if (this.freespeed <= 0.0) { Gbl.errorMsg(this+"[freespeed="+freespeed+" not allowed]"); }
		if (this.capacity <= 0.0) { Gbl.errorMsg(this+"[capacity="+capacity+" not allowed]"); }
		if (this.permlanes < 1) { Gbl.errorMsg(this+"[permlanes="+permlanes+" not allowed]"); }
	}



	private void initNetworkChangeEvents() {
		
		this.changeEvents = new PriorityQueue<NetworkChangeEvent>();		
		
		initFreespeedEvent();
		


	}

	private void initFreespeedEvent() {
		this.freespeedEvents = new TreeMap<Double, Double>();
		this.freespeedEvents.put(-1., this.freespeed); // make sure that freespeed is set to 'default' freespeed as long as no change event occurs
		this.freespeedEvents.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.freespeed); // make sure that freespeed is set to 'default' freespeed as long as no change event occurs

		this.freespeedTravelTime = new TreeMap<Double, Double>();
		this.freespeedTravelTime.put(-1., this.length / this.freespeed);
		this.freespeedTravelTime.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.length / this.freespeed);
		
	}

//	calc methods







	private void calcFlowCapacity() {
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
//		log.debug("capacity period: " + capacityPeriod);
		this.flowCapacity = this.capacity / capacityPeriod;
	}



//	get methods



	/**
	 * This method returns the freespeed for current time
	 * @param time - the current time
	 * @Override {@link org.matsim.basic.v01.BasicLink.getFreespeed}
	 */
	@Override
	public double getFreespeed(double time) {
		if (this.freespeedEvents == null) {
			rebuildFreespeedChange();
		}
		return this.freespeedEvents.floorEntry(time).getValue();
	}

	public double getFreespeedTravelTime(double time) {
		if (this.freespeedTravelTime == null) {
			rebuildFreespeedChange();
		}
		return this.freespeedTravelTime.floorEntry(time).getValue();
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getFlowCapacity()
	 */
	public final double getFlowCapacity() {
		return this.flowCapacity;
	}


//	set methods


	/**
	 * This method add a new freespeed change event. If there already exist an event for the given time, then
	 * the old value will be overwritten.
	 *
	 *  @param time - the time on which the event occurs
	 *  @param freespeed - the new freespeed
	 */
	private void addFreespeedEvent(final double time, final double freespeed) {
		this.freespeedEvents.put(time, freespeed);
		if (freespeed <= 0) {
			this.freespeedTravelTime.put(time,Double.POSITIVE_INFINITY);
		}else {
			this.freespeedTravelTime.put(time,this.length/freespeed);
		}

	}

	/**
	 * This method applies a new change event to the link.
	 *
	 * @param event
	 */
	public void applyEvent(NetworkChangeEvent event) {
		
		this.changeEvents.add(event);
		
		if (event.getFreespeedChange() != null) {
			this.freespeedEvents = null;
			this.freespeedTravelTime = null;
		}
		if (event.getFlowCapacityChange() != null) {
			throw  new RuntimeException("Flow capacity change capability is not implemented yet!");
		}
		if (event.getLanesChange() != null) {
			throw  new RuntimeException("Lanes change capability is not implemented yet!");
		}

	}

//	print methods


	private void rebuildFreespeedChange() {
		
		
		PriorityQueue<NetworkChangeEvent> events = new PriorityQueue<NetworkChangeEvent>(this.changeEvents);
		this.initFreespeedEvent();
		while (events.peek() != null) {
			NetworkChangeEvent event = events.poll();
			if (event.getFreespeedChange() != null) {
				ChangeValue freespeedChange = event.getFreespeedChange();
				double currentFreeSpeed = getFreespeed(event.getStartTime());
				if (freespeedChange.getType() == NetworkChangeEvent.ChangeType.FACTOR){
					this.addFreespeedEvent(event.getStartTime(),currentFreeSpeed * freespeedChange.getValue());
				} else {
					this.addFreespeedEvent(event.getStartTime(), freespeedChange.getValue());
				}

				if (event.getEndTime() != org.matsim.utils.misc.Time.UNDEFINED_TIME) {
					//TODO this makes trouble with overlapping intervals. for now we throw an exception ... [GL] 
					throw new RuntimeException("at the moment only events without a duration can be handled!");
//					this.addFreespeedEvent(event.getEndTime(), currentFreeSpeed);
				}				
			}
		}
		
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
