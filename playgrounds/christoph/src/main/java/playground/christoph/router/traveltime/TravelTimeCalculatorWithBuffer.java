/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorWithBuffer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.router.traveltime;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

public class TravelTimeCalculatorWithBuffer extends TravelTimeCalculator {

	private static final Logger log = Logger.getLogger(TravelTimeCalculatorWithBuffer.class);

	private Map<Id, double[]> bufferedTravelTimes;
	private Set<Id> nonCarAgents = new HashSet<Id>();

	public TravelTimeCalculatorWithBuffer(Network network, int timeslice,
			int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, timeslice, maxTime, ttconfigGroup);

		initBuffer(network);
	}

	public TravelTimeCalculatorWithBuffer(Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, ttconfigGroup);

		initBuffer(network);
	}

	/*
	 * Initialize TravelTimes by using an EventsFile from a
	 * previous run.
	 */
	public void initTravelTimes(String eventsFile) {

		if (eventsFile == null || !new File(eventsFile).exists()) {
			log.warn("No valid EventsFile - using free speed travel times instead.");
			return;
		} else if (!eventsFile.toLowerCase(Locale.ROOT).endsWith("xml.gz") && !eventsFile.toLowerCase(Locale.ROOT).endsWith(".xml")) {
			log.warn("EventsFile has to be in xml Format - .txt EventsFiles do not contain the TransportMode of a Leg.");
			return;
		}

		// We use a new EventsManager where we only register the TravelTimeCalculator.
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(this);

		log.info("Processing events file to get initial travel times...");
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
	}

	/*
	 * Initially use FreeSpeedTravelTimes
	 */
	private void initBuffer(Network network) {
		int timeSlice = this.getTimeSlice();
		int numSlots = this.getNumSlots();

		bufferedTravelTimes = new ConcurrentHashMap<Id, double[]>();

		for (Link link : network.getLinks().values())
		{
			double[] travelTimeArray = new double[numSlots];

			int time = 0;
			for (int i = 0; i < numSlots; i++)
			{
				travelTimeArray[i] = link.getLength() / link.getFreespeed(time);
				time = time + timeSlice;
			}

			bufferedTravelTimes.put(link.getId(), travelTimeArray);
		}
	}

	private void updateBufferedTravelTimes(int iteration) {
		if (iteration == 0) return;	// before the first Iteration -> nothing to do

		for (Entry<Id, double[]> entry : bufferedTravelTimes.entrySet()) {
			double[] travelTimeArray = entry.getValue();

			int time = 0;
			for (int i = 0; i < this.getNumSlots(); i++) {
				travelTimeArray[i] = this.getLinkTravelTime(entry.getKey(), time);
				time = time + this.getTimeSlice();
			}
		}
	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.getTimeSlice();
		if (slice >= this.getNumSlots()) slice = this.getNumSlots() - 1;
		return slice;
	}

	public double getBufferedLinkTravelTime(Link link, double time) {
		double[] travelTimeArray = bufferedTravelTimes.get(link.getId());
		int slice = getTimeSlotIndex(time);
		return travelTimeArray[slice];
	}

	/*
	 * Before resetting the data, we copy them to the buffer.
	 */
	@Override
	public void reset(int iteration) {
		updateBufferedTravelTimes(iteration);
		nonCarAgents = new HashSet<Id>();

		super.reset(iteration);
	}

	/*
	 * We only want to calculate traveltimes of car legs. Therefore we collect
	 * all agents that perform non-car legs.
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getLegMode().equals(TransportMode.car)) {
			nonCarAgents.add(event.getPersonId());
		}
		super.handleEvent(event);
	}

	/*
	 * We try to remove the agent from the nonCarAgents List (if it was a carAgent,
	 * nothing will happen).
	 */
	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		nonCarAgents.remove(event.getPersonId());
		super.handleEvent(event);
	}

	/*
	 * If it is an agent with a car leg, we pass the event to the superclass.
	 */
	@Override
	public void handleEvent(final LinkEnterEvent e) {
		if (!nonCarAgents.contains(e.getDriverId())) {
			super.handleEvent(e);
		}
	}

	/*
	 * If it is an agent with a car leg, we pass the event to the superclass.
	 */
	@Override
	public void handleEvent(final LinkLeaveEvent e) {
		if (!nonCarAgents.contains(e.getDriverId())) {
			super.handleEvent(e);
		}
	}

	/*
	 * We try to remove the agent from the nonCarAgents List and pass the
	 * event then to the superclass.
	 */
	@Override
	public void handleEvent(PersonStuckEvent event) {
		nonCarAgents.remove(event.getPersonId());
		super.handleEvent(event);
	}
	
	public TravelTime getTravelTimesFromPreviousIteration() {
		return new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return getBufferedLinkTravelTime(link, time);
			}
			
		};
	}
	
}
