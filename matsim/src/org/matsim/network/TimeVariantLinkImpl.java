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

import java.util.LinkedList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.utils.collections.gnuclasspath.TreeMap;

/**
 * @author laemmel
 * @author illenberger
 *
 */
public class TimeVariantLinkImpl extends LinkImpl {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private List<NetworkChangeEvent> changeEvents;
	
	private TreeMap<Double, Double> freespeedValues;
	
	private TreeMap<Double, Double> flowCapacityValues;
	
	private TreeMap<Double, Double> lanesValues;
	
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
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}

	/**
	 * Applies a new change event to the link.
	 *
	 * @param event a network change event.
	 */
	protected void applyEvent(NetworkChangeEvent event) {
		if(changeEvents == null)
			this.changeEvents = new LinkedList<NetworkChangeEvent>();
		
		this.changeEvents.add(event);
		/*
		 * Invalidate all value maps, so that they will be re-initialized on
		 * next access.
		 */
		if (event.getFreespeedChange() != null) {
			this.freespeedValues = null;
		}
		if (event.getFlowCapacityChange() != null) {
			this.flowCapacityValues = null;
		}
		if (event.getLanesChange() != null) {
			this.lanesValues = null;
		}
	}

	/**
	 * Removes all NetworkChangeEvents so that the link's attributes will be
	 * reset to their initial values.
	 */
	protected void clearEvents() {
		if(changeEvents != null)
			this.changeEvents.clear();
	
		freespeedValues = null;
		flowCapacityValues = null;
		lanesValues = null;
	}
	
	/**
	 * 
	 * @param time - the time in seconds.
	 * @return the freespeed at time <tt>time</tt>.
	 */
	@Override
	public double getFreespeed(double time) {
		if (freespeedValues == null)
			initFreespeedValueMap();
		return freespeedValues.floorEntry(time).getValue();
	}

	/**
	 * @param time - the time in seconds.
	 * @return the freespeed travel time at time <tt>time</tt>.
	 */
	public double getFreespeedTravelTime(double time) {
		return getLength()/getFreespeed(time);
	}

	/**
	 * @param time - the time in seconds.
	 * @return the flow capacity at time <tt>time</tt>.
	 */
	public double getFlowCapacity(double time) {
		if (this.flowCapacityValues == null)
			initFlowCapacityValueMap();

		return this.flowCapacityValues.floorEntry(time).getValue();
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
		if (this.flowCapacityValues == null) {
			initFlowCapacityValueMap();
		}
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		return this.flowCapacityValues.floorEntry(time).getValue() * capacityPeriod;
	}


	/**
	 * @param time - the time in seconds.
	 * @return the number of lanes at time <tt>time</tt>.
	 */
	/*
	 * Under what circumstances do we have lanes that are not integers? joh
	 * 10may2008
	 * in the case of pedestrian simulation. We use the information of the double lanes to
	 * encode the (min) width of the link to calculate the flow capacity - [GL] 13may08
	 */
	@Override
	public double getLanes(double time) {
		if (this.lanesValues == null)
			initLanesValueMap();
		
		return this.lanesValues.floorEntry(time).getValue();
	}

	/**
	 * @param time - the time in seconds.
	 * @return the number of lanes at time <tt>time</tt>.
	 */
	/*
	 * I do not see any reason for this method! See above... joh 10may2008
	 * this method is just for being backward compatible ... Might be we
	 * could mark this method as deprecated [GL] - 13may2008   
	 */
	@Override
	public int getLanesAsInt(double time) {
		if (this.lanesValues == null)
			initLanesValueMap();
		
		return Math.round((float)Math.max(this.lanesValues.floorEntry(time).getValue(),1.0d));
	}

	private void initFreespeedValueMap() {
		freespeedValues = new TreeMap<Double, Double>();
		/*
		 * Make sure that there is at least the initial value in the map. Use
		 * Double.NEGATIVE_INFINITY to be sure that the initial value is ALWAYS
		 * associated with the lowest key!
		 */
		freespeedValues.put(Double.NEGATIVE_INFINITY, this.freespeed);
		
		if (changeEvents != null) {
			for (NetworkChangeEvent event : changeEvents) {
				ChangeValue value = event.getFreespeedChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = getFreespeed(event.getStartTime());
						this.freespeedValues.put(event.getStartTime(),
								currentValue * value.getValue());
					} else {
						this.freespeedValues.put(event.getStartTime(), value
								.getValue());
					}
				}
			}
		}
	}
	
	private void initFlowCapacityValueMap() {
		flowCapacityValues = new TreeMap<Double, Double>();
		/*
		 * Make sure that there is at least the initial value in the map. Use
		 * Double.NEGATIVE_INFINITY to be sure that the initial value is ALWAYS
		 * associated with the lowest key!
		 */

		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		flowCapacityValues.put(Double.NEGATIVE_INFINITY, capacity / capacityPeriod);
		
		if (changeEvents != null) {
			for (NetworkChangeEvent event : changeEvents) {
				ChangeValue value = event.getFlowCapacityChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = getFlowCapacity(event.getStartTime());
						this.flowCapacityValues.put(event.getStartTime(),
								currentValue * value.getValue());
					} else {
						this.flowCapacityValues.put(event.getStartTime(), value
								.getValue());
					}
				}
			}
		}
	}
	
	private void initLanesValueMap() {
		lanesValues = new TreeMap<Double, Double>();
		/*
		 * Make sure that there is at least the initial value in the map. Use
		 * Double.NEGATIVE_INFINITY to be sure that the initial value is ALWAYS
		 * associated with the lowest key!
		 */
		lanesValues.put(Double.NEGATIVE_INFINITY, permlanes);
		
		if (changeEvents != null) {
			for (NetworkChangeEvent event : changeEvents) {
				ChangeValue value = event.getLanesChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = getLanes(event.getStartTime());
						this.lanesValues.put(event.getStartTime(),
								currentValue * value.getValue());
					} else {
						this.lanesValues.put(event.getStartTime(), value
								.getValue());
					}
				}
			}
		}
	}
		
	/**
	 * @deprecated
	 */
	public void calcFlowCapacity() {
		initFlowCapacityValueMap();
	}
}
