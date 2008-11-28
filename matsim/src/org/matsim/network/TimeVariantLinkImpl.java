/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.Arrays;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.network.NetworkChangeEvent.ChangeValue;

/**
 * @author laemmel
 * @author illenberger
 *
 */
public class TimeVariantLinkImpl extends LinkImpl {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	
	private TreeMap<Double,NetworkChangeEvent> changeEvents;
	
	
	
	private int aFreespeedEvents = 1;
	private double [] aFreespeedValues;
	private double [] aFreespeedTimes;
	
	private int aFlowCapacityEvents = 1;
	private double [] aFlowCapacityValues;
	private double [] aFlowCapacityTimes;
	
	
	
	private int aLanesEvents = 1;
	private double [] aLanesValues;
	private double [] aLanesTimes;
	
	
	

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public TimeVariantLinkImpl(final Id id, final BasicNode from, final BasicNode to, final NetworkLayer network, final double length, final double freespeed, final double capacity, final double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}

	/**
	 * Applies a new change event to the link.
	 *
	 * @param event a network change event.
	 */
	protected void applyEvent(final NetworkChangeEvent event) {
		if(this.changeEvents == null)
			this.changeEvents = new TreeMap<Double,NetworkChangeEvent>();
		
		this.changeEvents.put(event.getStartTime(), event);
		
		
		/*
		 * Increment the arrays size, so that they will be re-initialized on
		 * next access.
		 */
		if (event.getFreespeedChange() != null) {
			this.aFreespeedEvents++;
		}
		if (event.getFlowCapacityChange() != null) {
			this.aFlowCapacityEvents++;
		}
		if (event.getLanesChange() != null) {
			this.aLanesEvents++;
		}
	}



	/**
	 * Removes all NetworkChangeEvents so that the link's attributes will be
	 * reset to their initial values.
	 */
	protected void clearEvents() {
		if(this.changeEvents != null)
			this.changeEvents.clear();
	
		this.aFreespeedEvents = 1;
		this.aFlowCapacityEvents = 1;
		this.aLanesEvents = 1;
	}
	
	/**
	 * 
	 * @param time - the time in seconds.
	 * @return the freespeed at time <tt>time</tt>.
	 */
	@Override
	public double getFreespeed(final double time) {
		
		if (this.aFreespeedTimes == null || this.aFreespeedTimes.length != this.aFreespeedEvents) {
			initFreespeedEventsArrays();
		}
		
		int key = Arrays.binarySearch(this.aFreespeedTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aFreespeedValues[key];
		
	}



	/**
	 * @param time - the time in seconds.
	 * @return the freespeed travel time at time <tt>time</tt>.
	 */
	@Override
	public double getFreespeedTravelTime(final double time) {
		return getLength()/getFreespeed(time);
	}

	/**
	 * @param time - the time in seconds.
	 * @return the flow capacity at time <tt>time</tt>.
	 */
	@Override
	public double getFlowCapacity(final double time) {
		
		if (this.aFlowCapacityTimes == null || this.aFlowCapacityTimes.length != this.aFlowCapacityEvents) {
			initFlowCapacityEventsArrays();
		}
		
		int key = Arrays.binarySearch(this.aFlowCapacityTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aFlowCapacityValues[key];
		
	}
	
	
	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	@Override
	public double getCapacity(final double time) {
	
		if (this.aFlowCapacityTimes == null || this.aFlowCapacityTimes.length != this.aFlowCapacityEvents) {
			initFlowCapacityEventsArrays();
		}
		
		int key = Arrays.binarySearch(this.aFlowCapacityTimes, time);
		key = key >= 0 ? key : -key - 2;
		
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		return this.aFlowCapacityValues[key] * capacityPeriod;

		
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
	public double getLanes(final double time) {
		
		
		if (this.aLanesTimes == null || this.aLanesTimes.length != this.aLanesEvents) {
			initLanesEventsArrays();
		}
		
		int key = Arrays.binarySearch(this.aLanesTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aLanesValues[key];
		
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
	public int getLanesAsInt(final double time) {
		
		if (this.aLanesTimes == null || this.aLanesTimes.length != this.aLanesEvents) {
			initLanesEventsArrays();
		}
		
		int key = Arrays.binarySearch(this.aLanesTimes, time);
		key = key >= 0 ? key : -key - 2;
		return Math.round((float)Math.max(this.aLanesValues[key],1.0d));
		
	}

	
	
	private void initFreespeedEventsArrays() {
		
		this.aFreespeedTimes = new double [this.aFreespeedEvents];
		this.aFreespeedValues = new double [this.aFreespeedEvents];
		this.aFreespeedTimes[0] = Double.NEGATIVE_INFINITY;
		this.aFreespeedValues[0] = this.freespeed;

		int numEvent = 0;
		if (this.changeEvents != null) {
			for (NetworkChangeEvent event : this.changeEvents.values()) {
				ChangeValue value = event.getFreespeedChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = this.aFreespeedValues[numEvent]; 
						this.aFreespeedValues[++numEvent] = currentValue * value.getValue();
						this.aFreespeedTimes[numEvent] = event.getStartTime();
					} else {
						this.aFreespeedValues[++numEvent] = value.getValue();
						this.aFreespeedTimes[numEvent] = event.getStartTime();
					}
				}
			}
		}
		
		if (numEvent != this.aFreespeedEvents - 1) {
			throw new RuntimeException("Expected number of change events (" + (this.aFreespeedEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
		}
		
	}
	
	private void initFlowCapacityEventsArrays() {
		
		this.aFlowCapacityTimes = new double [this.aFlowCapacityEvents];
		this.aFlowCapacityValues = new double [this.aFlowCapacityEvents];
		this.aFlowCapacityTimes[0] = Double.NEGATIVE_INFINITY;
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		this.aFlowCapacityValues[0] = this.capacity / capacityPeriod;

		int numEvent = 0;
		if (this.changeEvents != null) {
			for (NetworkChangeEvent event : this.changeEvents.values()) {
				ChangeValue value = event.getFlowCapacityChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = this.aFlowCapacityValues[numEvent]; 
						this.aFlowCapacityValues[++numEvent] = currentValue * value.getValue();
						this.aFlowCapacityTimes[numEvent] = event.getStartTime();
					} else {
						this.aFlowCapacityValues[++numEvent] = value.getValue();
						this.aFlowCapacityTimes[numEvent] = event.getStartTime();
					}
				}
			}
		}
		
		if (numEvent != this.aFlowCapacityEvents - 1) {
			throw new RuntimeException("Expected number of change events (" + (this.aFreespeedEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
		}
		
	}
	
	private void initLanesEventsArrays() {
		
		this.aLanesTimes = new double [this.aLanesEvents];
		this.aLanesValues = new double [this.aLanesEvents];
		this.aLanesTimes[0] = Double.NEGATIVE_INFINITY;
		this.aLanesValues[0] = this.permlanes;

		int numEvent = 0;
		if (this.changeEvents != null) {
			for (NetworkChangeEvent event : this.changeEvents.values()) {
				ChangeValue value = event.getLanesChange();
				if (value != null) {
					if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
						double currentValue = this.aLanesValues[numEvent]; 
						this.aLanesValues[++numEvent] = currentValue * value.getValue();
						this.aLanesTimes[numEvent] = event.getStartTime();
					} else {
						this.aLanesValues[++numEvent] = value.getValue();
						this.aLanesTimes[numEvent] = event.getStartTime();
					}
				}
			}
		}
		
		if (numEvent != this.aLanesEvents - 1) {
			throw new RuntimeException("Expected number of change events (" + (this.aFreespeedEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
		}
		
	}

}

