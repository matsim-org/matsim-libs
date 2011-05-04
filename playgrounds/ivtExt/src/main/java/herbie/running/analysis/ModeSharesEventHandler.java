/* *********************************************************************** *
 * project: org.matsim.*
 * ModeDistanceSharesEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package herbie.running.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordUtils;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;

/**
 * Collects and processes data on the mode shares, based on the travelled
 * (euclidean) distance.
 * Can also produce statistics on the mode shares based on the number of
 * legs.
 *
 * @author thibautd
 */
public class ModeSharesEventHandler
		extends AbstractClassifiedFrequencyAnalysis
		implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	private static final Logger log =
		Logger.getLogger(ModeSharesEventHandler.class);

	// use euclidean distance rather than network distance, as linkEvents
	// are not generated for non-car modes.

	private final Map<Id, AgentDepartureEvent> pendantDepartures =
			new HashMap<Id, AgentDepartureEvent>();
	private final Network network;
		
	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	/**
	 * @param controler the controler, used to get the network
	 */
	public ModeSharesEventHandler(final Controler controler) {
		this.network = controler.getNetwork();
	}
	
	/*
	 * =========================================================================
	 * Handling methods
	 * =========================================================================
	 */
	@Override
	public void reset(final int iteration) {
		this.processEndOfIteration(iteration);

		this.frequencies.clear();
		this.rawData.clear();

		if (this.pendantDepartures.size() > 0) {
			log.warn("Some arrivals were not handled!");
			this.pendantDepartures.clear();
		}
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		// catch the previous value to check consistency of the process
		AgentDepartureEvent old =
			this.pendantDepartures.put(event.getPersonId(), event);

		if (old != null) {
			log.warn("One departure were not handled before the following one "+
					" for agent "+event.getPersonId());
		}
	}

	@Override
	public void handleEvent(final AgentArrivalEvent arrivalEvent) {
		AgentDepartureEvent departureEvent =
			this.pendantDepartures.remove(arrivalEvent.getPersonId());
		String mode = arrivalEvent.getLegMode();
		Frequency frequency;
		ResizableDoubleArray rawDataElement;
		Link departureLink;
		Link arrivalLink;
		double distance;

		// Consistency check...
		if (departureEvent == null) {
			log.warn("One arrival do not correspond to any departure for agent "+
					arrivalEvent.getPersonId());
			return;
		}
		else if (!mode.equals(departureEvent.getLegMode())) {
			log.warn("Departure and arrival have uncompatible modes!");
			return;
		}
		// consistency check... DONE
		
		if (this.frequencies.containsKey(mode)) {
			frequency = this.frequencies.get(mode);
			rawDataElement = this.rawData.get(mode);
		}
		else {
			frequency = new Frequency();
			rawDataElement = new ResizableDoubleArray();

			this.frequencies.put(mode, frequency);
			this.rawData.put(mode, rawDataElement);
		}

		// compute data
		departureLink = this.network.getLinks().get(departureEvent.getLinkId());
		arrivalLink = this.network.getLinks().get(arrivalEvent.getLinkId());

		distance = CoordUtils.calcDistance(
				departureLink.getCoord(),
				arrivalLink.getCoord());

		// remember data
		frequency.addValue(distance);
		rawDataElement.addElement(distance);
	}

	/*
	 * =========================================================================
	 * processing methods
	 * =========================================================================
	 */
	/**
	 * Helper method to perform any necessary data processing before clearing
	 * data structures.
	 */
	private void processEndOfIteration(final int iteration) {
		Map<String, Double> modeShares = getModeShares();

		log.info("Share of traveled distances by mode:");
		for (String mode : modeShares.keySet()) {
			log.info(mode+": "+modeShares.get(mode));
		}
	}

	private Map<String, Double> getModeShares() {
		Map<String, Double> modeShares = new HashMap<String, Double>();
		double totalDistance = 0d;
		double currentDistance;

		for (String mode : modeShares.keySet()) {
			currentDistance = 0d;
			for (double d : this.rawData.get(mode).getElements()) {
				currentDistance += d;
			}
			totalDistance += currentDistance;
			modeShares.put(mode, currentDistance);
		}

		for (String mode : modeShares.keySet()) {
			currentDistance =  modeShares.get(mode);
			modeShares.put(mode, currentDistance / totalDistance);
		}

		return modeShares;
	}

	/*
	 * =========================================================================
	 * Miscelaneous
	 * =========================================================================
	 */
	@Override
	public void run(Person person) { /*do nothing*/ }
}

