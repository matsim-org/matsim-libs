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

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;

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

    private final TimeVariantAttribute variableFreespeed;
    private final TimeVariantAttribute variableFlowCapacity;
    private final TimeVariantAttribute variableLanes;
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

    public static TimeVariantLinkImpl createLinkWithVariableIntervalAttributes(final Id<Link> id,
            final Node from, final Node to, final Network network, final double length,
            final double freespeed, final double capacity, final double lanes) {
        return new TimeVariantLinkImpl(id, from, to, network, length, freespeed, capacity, lanes,
                new VariableIntervalTimeVariantAttribute(),
                new VariableIntervalTimeVariantAttribute(),
                new VariableIntervalTimeVariantAttribute());
    }


    public static TimeVariantLinkImpl createLinkWithFixedIntervalAttributes(final Id<Link> id,
            final Node from, final Node to, final Network network, final double length,
            final double freespeed, final double capacity, final double lanes, final int interval,
            final int intervalCount) {
        return new TimeVariantLinkImpl(id, from, to, network, length, freespeed, capacity, lanes,
                new FixedIntervalTimeVariantAttribute(interval, intervalCount),
                new FixedIntervalTimeVariantAttribute(interval, intervalCount),
                new FixedIntervalTimeVariantAttribute(interval, intervalCount));
    }

   
    TimeVariantLinkImpl(final Id<Link> id, final Node from, final Node to, final Network network,
            final double length, final double freespeed, final double capacity, final double lanes,
            TimeVariantAttribute variableFreespeed, TimeVariantAttribute variableFlowCapacity,
            TimeVariantAttribute variableLanes) {
        super(id, from, to, network, length, freespeed, capacity, lanes);
        this.variableFreespeed = variableFreespeed;
        this.variableFlowCapacity = variableFlowCapacity;
        this.variableLanes = variableLanes;
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

		if (event.getFreespeedChange() != null) {
			this.variableFreespeed.incChangeEvents();
		}
		if (event.getFlowCapacityChange() != null) {
			this.variableFlowCapacity.incChangeEvents();
		}
		if (event.getLanesChange() != null) {
			this.variableLanes.incChangeEvents();
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
		variableFlowCapacity.clearEvents();
		variableLanes.clearEvents();
	}

	/**
	 *
	 * @param time - the time in seconds.
	 * @return the freespeed at time <tt>time</tt>.
	 */
	@Override
	public synchronized double getFreespeed(final double time) {

		if (variableFreespeed.isRecalcRequired()) {
			recalcFreespeed();
		}

		return variableFreespeed.getValue(time);
	}

	@Override
	public void setFreespeed(double freespeed) {
		super.setFreespeed(freespeed);
		this.recalcFreespeed();
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

		if (variableFlowCapacity.isRecalcRequired()) {
			recalcFlowCapacity();
		}

		return variableFlowCapacity.getValue(time);
	}
	
	@Override
	public final void setCapacity(double capacityPerNetworkCapcityPeriod){
		super.setCapacity(capacityPerNetworkCapcityPeriod);
		this.recalcFlowCapacity();
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
		return getFlowCapacity() * getCapacityPeriod();
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
		if (variableLanes.isRecalcRequired()) {
			recalcLanes();
		}

		return variableLanes.getValue(time);
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		super.setNumberOfLanes(lanes);
		this.recalcLanes();
	}

	
	private synchronized void recalcFreespeed() {
	    variableFreespeed.recalc(changeEvents, TimeVariantAttribute.FREESPEED_GETTER, freespeed);
	}

	private synchronized void recalcFlowCapacity() {
	    double baseFlowCapacity = this.capacity / getCapacityPeriod();
	    variableFlowCapacity.recalc(changeEvents, TimeVariantAttribute.FLOW_CAPACITY_GETTER, baseFlowCapacity);
	}

	private synchronized void recalcLanes() {
		variableLanes.recalc(changeEvents, TimeVariantAttribute.LANES_GETTER, nofLanes);
	}
}

