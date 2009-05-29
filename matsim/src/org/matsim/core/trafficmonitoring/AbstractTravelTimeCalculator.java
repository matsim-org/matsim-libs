/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractTravelTimeCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.trafficmonitoring;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.router.util.TravelTime;

public abstract class AbstractTravelTimeCalculator implements TravelTime, EventHandler{

	protected final int timeslice;
	protected final int numSlots;
	private final TravelTimeAggregatorFactory factory;
	private  final AbstractTravelTimeAggregator aggregator;

	/**
	 * 
	 * @param network
	 * @param timeslice
	 * @param maxTime
	 * @param factory
	 * @deprecated use the constructor without reference to network
	 */
	@Deprecated
	public AbstractTravelTimeCalculator(final Network network, final int timeslice, 
			final int maxTime, TravelTimeAggregatorFactory factory) {
		this.factory = factory;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.aggregator = this.factory.createTravelTimeAggregator(this.numSlots, this.timeslice);
	}
	
	
	public AbstractTravelTimeCalculator(final int timeslice, 
			final int maxTime, TravelTimeAggregatorFactory factory) {
		this.factory = factory;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.aggregator = this.factory.createTravelTimeAggregator(this.numSlots, this.timeslice);
	}

	protected int getNumSlots() {
		return numSlots;
	}

	protected TravelTimeAggregatorFactory getTravelTimeAggregatorFactory() {
		return factory;
	}

	protected AbstractTravelTimeAggregator getTravelTimeAggregator() {
		return aggregator;
	}
	
	public abstract double getLinkTravelTime(Link link, double time);
	
}