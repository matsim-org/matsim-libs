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
	private TreeMap<Double, Double> flowCapacityEvents;
	private TreeMap<Double, Double> lanesEvents;
	private TreeMap<Double, Double> freespeedTravelTime;
	
	private PriorityQueue<NetworkChangeEvent> changeEvents;

	/* TODO [MR,GL] instead of these "foreign" TreeMap's, why not use two simple arrays of doubles per
	 * TreeMap, one to store the time ("key"), the other to store the actual values. Then, one could
	 * access the value-array with Arrays.binarySearch(). BinarySearch and TreeMap both use O(log n) for
	 * finding the corresponding value, but for the TreeMap, a multitude of auto-boxing- and -unboxing-
	 * operations need to be executed, making it probably slow. One needs to test/benchmark this first,
	 * but it could be a possibility for further optimizations. -marcel/06may2008 
	 */



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
//		calcFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME);
		// do some semantic checks
		if (this.from.equals(this.to)) { log.warn(this + "[from=to=" + this.to + " link is a loop]"); }
		if (this.freespeed <= 0.0) { Gbl.errorMsg(this+"[freespeed="+freespeed+" not allowed]"); }
		if (this.capacity <= 0.0) { Gbl.errorMsg(this+"[capacity="+capacity+" not allowed]"); }
		if (this.permlanes < 1) { Gbl.errorMsg(this+"[permlanes="+permlanes+" not allowed]"); }
	}

	/**
	 * Removes all NetworkChangeEvents so that the link's attributes will be
	 * reset to their initial values.
	 */
	protected void clearNetworkChangeEvents() {
		if(changeEvents != null) changeEvents.clear();
		if(freespeedEvents != null) initFreespeedEvent();
		if(flowCapacityEvents != null) initFlowCapacityEvent();
		if(lanesEvents != null) initLanesEvent();
	}

	private void initNetworkChangeEvents() {
		
		this.changeEvents = new PriorityQueue<NetworkChangeEvent>();		
		
		initFreespeedEvent();
		initFlowCapacityEvent();
		


	}

	private void initFreespeedEvent() {
		this.freespeedEvents = new TreeMap<Double, Double>();
		this.freespeedEvents.put(-1., this.freespeed); // make sure that freespeed is set to 'default' freespeed as long as no change event occurs
		this.freespeedEvents.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.freespeed); // make sure that freespeed is set to 'default' freespeed as long as no change event occurs

		this.freespeedTravelTime = new TreeMap<Double, Double>();
		this.freespeedTravelTime.put(-1., this.length / this.freespeed);
		this.freespeedTravelTime.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.length / this.freespeed);
		
	}
						   
	private void initFlowCapacityEvent() {
		this.flowCapacityEvents = new TreeMap<Double, Double>();
		
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		this.flowCapacityEvents.put(-1., this.capacity / capacityPeriod); // make sure that flowcapacity is set to 'default' flowcapacity as long as no change event occurs
		this.flowCapacityEvents.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.capacity / capacityPeriod); // make sure that flowcapacity is set to 'default' flowcapacity as long as no change event occurs
		
	}
	
	private void initLanesEvent() {
		this.lanesEvents = new TreeMap<Double, Double>();
		
		this.lanesEvents.put(-1., this.permlanes); // make sure that flowcapacity is set to 'default' flowcapacity as long as no change event occurs
		this.lanesEvents.put(org.matsim.utils.misc.Time.UNDEFINED_TIME, this.permlanes); // make sure that flowcapacity is set to 'default' flowcapacity as long as no change event occurs
		
	}

//	get methods



	/**
	 * This method returns the freespeed for current time
	 * @param time - the current time
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



//	set methods


	public double getFlowCapacity(double time) {
		if (this.flowCapacityEvents == null) {
			rebuildFlowCapacityChange();
		}
		return this.flowCapacityEvents.floorEntry(time).getValue();
	}
	
	
	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	@Override
	public double getCapacity(double time) {
		if (this.flowCapacityEvents == null) {
			rebuildFlowCapacityChange();
		}
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		return this.flowCapacityEvents.floorEntry(time).getValue() * capacityPeriod;
	}


	@Override
	public double getLanes(double time) {
		if (this.lanesEvents == null) {
			rebuildLanesChange();
		}
		return this.lanesEvents.floorEntry(time).getValue();
	}


	@Override
	public int getLanesAsInt(double time) {
		if (this.lanesEvents == null) {
			rebuildLanesChange();
		}
		return Math.round((float)Math.max(this.lanesEvents.floorEntry(time).getValue(),1.0d));
	}


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
	 * This method add a new flow capacity change event. If there already exist an event for the given time, then
	 * the old value will be overwritten.
	 *
	 *  @param time - the time on which the event occurs
	 *  @param flowCapacity - the new flowCapacity
	 */
	private void addFlowCapacityEvent(final double time, final double flowCapacity) {
		this.flowCapacityEvents.put(time, flowCapacity);
	}
	
	/**
	 * This method add a new lanes  change event. If there already exist an event for the given time, then
	 * the old value will be overwritten.
	 *
	 *  @param time - the time on which the event occurs
	 *  @param lanes - the new number of lanes
	 */
	private void addLanesEvent(final double time, final double lanes) {
		this.lanesEvents.put(time, lanes);
	}

	/**
	 * This method applies a new change event to the link.
	 *
	 * @param event
	 */
	protected void applyEvent(NetworkChangeEvent event) {
		
		this.changeEvents.add(event);
		
		if (event.getFreespeedChange() != null) {
			this.freespeedEvents = null;
			this.freespeedTravelTime = null;
		}
		if (event.getFlowCapacityChange() != null) {
			this.flowCapacityEvents = null;
		}
		if (event.getLanesChange() != null) {
			this.lanesEvents = null;
		}

	}




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

			}
		}
	}
	
	private void rebuildFlowCapacityChange() {
		
		
		PriorityQueue<NetworkChangeEvent> events = new PriorityQueue<NetworkChangeEvent>(this.changeEvents);
		this.initFlowCapacityEvent();
		while (events.peek() != null) {
			NetworkChangeEvent event = events.poll();
			if (event.getFlowCapacityChange() != null) {
				ChangeValue flowCapacityChange = event.getFlowCapacityChange();
				double currentFlowCapacity = getFlowCapacity(event.getStartTime());
				if (flowCapacityChange.getType() == NetworkChangeEvent.ChangeType.FACTOR){
					this.addFlowCapacityEvent(event.getStartTime(),currentFlowCapacity * flowCapacityChange.getValue());
				} else {
					this.addFlowCapacityEvent(event.getStartTime(), flowCapacityChange.getValue());
				}

			}
		}
	}
	
	private void rebuildLanesChange() {
		
		
		PriorityQueue<NetworkChangeEvent> events = new PriorityQueue<NetworkChangeEvent>(this.changeEvents);
		this.initLanesEvent();
		while (events.peek() != null) {
			NetworkChangeEvent event = events.poll();
			if (event.getLanesChange() != null) {
				ChangeValue lanesChange = event.getLanesChange();
				double currentLanes = getLanes(event.getStartTime());
				if (lanesChange.getType() == NetworkChangeEvent.ChangeType.FACTOR){
					this.addLanesEvent(event.getStartTime(),currentLanes * lanesChange.getValue());
				} else {
					this.addLanesEvent(event.getStartTime(), lanesChange.getValue());
				}

			}
		}
	}
	
	public void calcFlowCapacity() {
		rebuildFlowCapacityChange();
	}
	
	
//	print methods

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
