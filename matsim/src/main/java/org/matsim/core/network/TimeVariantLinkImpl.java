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

package org.matsim.core.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * @author laemmel
 * @author illenberger
 *
 */
class TimeVariantLinkImpl extends LinkImpl {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private TreeMap<Double,NetworkChangeEvent> changeEvents;

	private TimeVariantAttribute variableFreespeed = new TimeVariantAttribute();
	
	private int aFlowCapacityEvents = 1;
	private double[] aFlowCapacityValues;
	private double[] aFlowCapacityTimes;

	private int aLanesEvents = 1;
	private double[] aLanesValues;
	private double[] aLanesTimes;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	TimeVariantLinkImpl(final Id<Link> id, final Node from, final Node to, final Network network, final double length, final double freespeed, final double capacity, final double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}

	/**
	 * Applies a new change event to the link.
	 *
	 * @param event a network change event.
	 */
	protected synchronized void applyEvent(final NetworkChangeEvent event) {
		if(this.changeEvents == null)
			this.changeEvents = new TreeMap<>();

		this.changeEvents.put(event.getStartTime(), event);


		/*
		 * Increment the arrays size, so that they will be re-initialized on
		 * next access.
		 */
		if (event.getFreespeedChange() != null) {
			this.variableFreespeed.incChangeEvents();
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
	synchronized void clearEvents() {
		if(this.changeEvents != null)
			this.changeEvents.clear();

		variableFreespeed.clearEvents();
		
		this.aFlowCapacityTimes = null;
		this.aFlowCapacityValues = null;
		this.aLanesTimes = null;
		this.aLanesValues = null;

		this.aFlowCapacityEvents = 1;
		this.aLanesEvents = 1;
	}

	/**
	 *
	 * @param time - the time in seconds.
	 * @return the freespeed at time <tt>time</tt>.
	 */
	@Override
	public synchronized double getFreespeed(final double time) {

		if (variableFreespeed.doRequireRecalc()) {
			initFreespeedEventsArrays();
		}

		return variableFreespeed.getValue(time);
	}

	@Override
	public void setFreespeed(double freespeed) {
		super.setFreespeed(freespeed);
		this.initFreespeedEventsArrays();
	}


	/**
	 * @param time - the time in seconds.
	 * @return the freespeed travel time at time <tt>time</tt>.
	 */
	@Override
	public synchronized double getFreespeedTravelTime(final double time) {
		return getLength()/getFreespeed(time);
	}

	/**
	 * @param time - the time in seconds.
	 * @return the flow capacity at time <tt>time</tt>.
	 */
	@Override
	public synchronized double getFlowCapacity(final double time) {

		if ((this.aFlowCapacityTimes == null) || (this.aFlowCapacityTimes.length != this.aFlowCapacityEvents)) {
			initFlowCapacityEventsArrays();
		}

		int key = Arrays.binarySearch(this.aFlowCapacityTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aFlowCapacityValues[key];

	}
	
	@Override
	public final void setCapacity(double capacityPerNetworkCapcityPeriod){
		super.setCapacity(capacityPerNetworkCapcityPeriod);
		this.initFlowCapacityEventsArrays();
	}


	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	@Override
	public synchronized double getCapacity(final double time) {

		if ((this.aFlowCapacityTimes == null) || (this.aFlowCapacityTimes.length != this.aFlowCapacityEvents)) {
			initFlowCapacityEventsArrays();
		}

		int key = Arrays.binarySearch(this.aFlowCapacityTimes, time);
		key = key >= 0 ? key : -key - 2;

		double capacityPeriod = getCapacityPeriod();
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
	public synchronized double getNumberOfLanes(final double time) {
		if ((this.aLanesTimes == null) || (this.aLanesTimes.length != this.aLanesEvents)) {
			initLanesEventsArrays();
		}

		int key = Arrays.binarySearch(this.aLanesTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aLanesValues[key];
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		super.setNumberOfLanes(lanes);
		this.initLanesEventsArrays();
	}

	
	private synchronized void initFreespeedEventsArrays() {
	    variableFreespeed.recalc(changeEvents, freespeed);
	}

	private synchronized void initFlowCapacityEventsArrays() {

		this.aFlowCapacityTimes = new double [this.aFlowCapacityEvents];
		this.aFlowCapacityValues = new double [this.aFlowCapacityEvents];
		this.aFlowCapacityTimes[0] = Double.NEGATIVE_INFINITY;
		double capacityPeriod = getCapacityPeriod();
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
			throw new RuntimeException("Expected number of change events (" + (this.aFlowCapacityEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
		}

	}

	private synchronized void initLanesEventsArrays() {

		this.aLanesTimes = new double [this.aLanesEvents];
		this.aLanesValues = new double [this.aLanesEvents];
		this.aLanesTimes[0] = Double.NEGATIVE_INFINITY;
		this.aLanesValues[0] = this.nofLanes;

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
			throw new RuntimeException("Expected number of change events (" + (this.aLanesEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
		}

	}

}

