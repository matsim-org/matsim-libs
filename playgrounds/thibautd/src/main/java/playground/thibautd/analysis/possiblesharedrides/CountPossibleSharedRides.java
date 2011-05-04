/* *********************************************************************** *
 * project: org.matsim.*
 * CountPossibleSharedRides.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.Time;

/**
 * Computes and outputs statistics about number of possible shared rides.
 * @author thibautd
 */
public class CountPossibleSharedRides {
	private static final Logger log =
		Logger.getLogger(CountPossibleSharedRides.class);

	private static final double DAY_DUR = 3600d*24d;

	private final double eventInitialSearchWindow = 10*60d;
	private final double eventSeachWindowIncr = 5*60d;

	private final EventsTopology arrivalsTopology;
	private final EventsTopology departuresTopology;
	private final EventsTopology enterLinksTopology;
	private final EventsTopology leaveLinksTopology;
	private final Map<Id, Plan> plans = new HashMap<Id, Plan>();

	private final List<TripData> results = new ArrayList<TripData>();

	private boolean toPrepare = false;

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public CountPossibleSharedRides(
			final Network network,
			final EventsAccumulator events,
			final Population population,
			final double acceptableDistance,
			final double timeWindowRadius) {
		TopologyFactory factory = new TopologyFactory(
				network,
				acceptableDistance,
				timeWindowRadius);
		this.arrivalsTopology = factory.createEventTopology(events.getArrivalEvents());
		this.departuresTopology = factory.createEventTopology(events.getDepartureEvents());
		this.enterLinksTopology = factory.createEventTopology(events.getEnterLinkEvents());
		this.leaveLinksTopology = factory.createEventTopology(events.getLeaveLinkEvents());

		for (Person pers : population.getPersons().values()) {
			this.plans.put(pers.getId(), pers.getSelectedPlan());
		}
	}

	/*
	 * =========================================================================
	 * Analysis functions
	 * =========================================================================
	 */
	/**
	 * Extracts trip data and accumulates them internaly.
	 * To run before any result getting method.
	 */
	public void run() {
		log.info("    computing trip data...");
		for (Map.Entry<Id, Plan> plan : this.plans.entrySet()) {
			//log.info("   treat new plan");
			for (PlanElement pe : plan.getValue().getPlanElements()) {
				if ((pe instanceof Leg) &&
						(((Leg) pe).getMode().equals(TransportMode.car))) {
					this.results.add(getTripData((Leg) pe, plan.getKey()));
				}
			}
		}
		toPrepare = true;
		log.info("    computing trip data... DONE");
	}

	/*
	 * =========================================================================
	 * formated results accessors
	 * =========================================================================
	 */
	/**
	 * @return a chart plotting the average number of trips that an agent can
	 * join per time bin.
	 */
	public ChartUtil getAvergePerTimeBinChart(final int nTimeBins) {
		this.prepareForAnalysis();

		String title = "Average number of possible joint trips per departure time";
		String xLabel = "Time of day (h)";
		String yLabel = "Number of trips";
		XYLineChart output = new XYLineChart(title, xLabel, yLabel);

		// binCount[i][0]: number of trip data taken into account
		// binCount[i][1]: sum of the trip counts for this bin
		int[][] binCounts = new int[nTimeBins][2];
		double[] timeAxis = computeDataPerTimeBin(binCounts, nTimeBins);
		double[] valueAxis = new double[nTimeBins];

		for (int i=0; i < nTimeBins; i++) {
			valueAxis[i] = (binCounts[i][0] > 0 ? 
					((double) binCounts[i][1]) / ((double) binCounts[i][0]) :
					0d);
		}

		output.addSeries("", timeAxis, valueAxis);

		return output;
	}

	private double[] computeDataPerTimeBin(
			final int[][] binCount,
			final int nTimeBins) {
		double[] timeValues = new double[nTimeBins];
		double[] timeBinsUpperBounds = new double[nTimeBins];
		double minTime = Math.min(0d,this.results.get(0).timeOfDay);
		double maxTime = Math.max(DAY_DUR, this.results.get(this.results.size() -1).timeOfDay);
		double currentTime = minTime;
		double timeStep = (maxTime - minTime) / nTimeBins;
		int currentBin = 0;

		// contruct time values
		for (int i=0; i < nTimeBins; i++) {
			timeValues[i] = (currentTime + (timeStep / 2d)) / 3600d;
			currentTime += timeStep;
			timeBinsUpperBounds[i] = currentTime;
		}

		// fill binCount, taking into account the ordering of results
		binCount[0][0] = 0;
		binCount[0][1] = 0;
		for (TripData data : this.results) {
			while (data.timeOfDay > timeBinsUpperBounds[currentBin]) {
				currentBin++;
				binCount[currentBin][0] = 0;
				binCount[currentBin][1] = 0;
			}
			binCount[currentBin][0]++;
			binCount[currentBin][1] += data.numberOfJoinableTrips;
		}

		return timeValues;
	}

	/**
	 * @return a  "box and whiskers" representation of the distribution of the number
	 * of trips joinable per time bin.
	 */
	public ChartUtil getBoxAndWhiskersPerTimeBin(final int nTimeBins) {
		this.prepareForAnalysis();
		//TODO: create a ChartUtil that returns a BaW plot.
		log.warn("using unimplemented method getBoxAndWhiskersPerTimeBin!");
		return null;
	}

	/*
	 * =========================================================================
	 * various helper methods
	 * =========================================================================
	 */
	/**
	 * XXX: only works if agent's plan has been modified during the iterations!
	 */
	private TripData getTripData(final Leg leg, final Id personId) {
		Route route = leg.getRoute();
		double departureTime = leg.getDepartureTime();

		if (departureTime == Time.UNDEFINED_TIME) {
			throw new RuntimeException("Only possible to count shared ride possibilities"
					+" if the times are set properly in plans. Exiting.");
		}

		double arrivalTime = departureTime + leg.getTravelTime();
		//TODO: get distance by a non-deprecated way
		double distance = route.getDistance();
		int numberOfJoinableTrips = 0;
		Id departureId = route.getStartLinkId();
		Id arrivalId = route.getEndLinkId();

		// correct leg info based on events
		//log.debug("getting departure and arrival events...");
		LinkEvent departure = getDepartureEvent(personId, departureTime, departureId);
		LinkEvent arrival = getArrivalEvent(personId, arrivalTime, arrivalId);
		//log.debug("getting departure and arrival events... DONE");

		//log.debug("counting possible shared rides...");
		numberOfJoinableTrips = countPossibleSharedRides(departure, arrival);
		//log.debug("counting possible shared rides... DONE");

		return new TripData(departure.getTime(), distance, numberOfJoinableTrips);
	}

	private LinkEvent getDepartureEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		return this.getEvent(person, expectedTime, link, this.departuresTopology);
	}

	private LinkEvent getArrivalEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		return this.getEvent(person, expectedTime, link, this.arrivalsTopology);
	}

	private LinkEvent getEvent(
			final Id person,
			final double expectedTime,
			final Id link,
			final EventsTopology searchEvents) {
		double timeWindow = this.eventInitialSearchWindow;
		List<LinkEvent> currentEventList;

		// increment the tw size until an event corresponding to the agent is
		// found.
		while (true) {
			//System.out.println("tic");
			currentEventList = searchEvents.getEventsInTimeWindow(
					link,
					expectedTime,
					timeWindow);
			for (LinkEvent currentEvent : currentEventList) {
				if (currentEvent.getPersonId().equals(person)) {
					return currentEvent;
				}
			}
			timeWindow +=  this.eventSeachWindowIncr;
			//System.out.println("tac, tw: "+(timeWindow/3600d)+", personId: "+person);
		}
	}

	private int countPossibleSharedRides(
			final LinkEvent departure,
			final LinkEvent arrival) {
		List<LinkEvent> departureNeighbors =
			this.leaveLinksTopology.getNeighbors(departure);
		List<LinkEvent> arrivalNeighbors =
			this.enterLinksTopology.getNeighbors(arrival);
		int count = 0;
		
		// count the number of persons having a "leaveLinkEvent" near the
		// departure and a "enterLinkEvent" near the arrival: they are considered
		// as potential shared ride drivers.
		for (PersonEvent event : departureNeighbors) {
			if ( passesHere(event.getPersonId(), arrivalNeighbors) ) {
				count++;
			}
		}

		return count;
	}

	/**
	 * @return true if there is an event in the list corresponding to the person.
	 */
	private boolean passesHere(
			final Id person,
			final List<? extends PersonEvent> events) {
		for (PersonEvent event : events) {
			if (event.getPersonId().equals(person)) {
				//remove event from list?
				return true;
			}
		}

		return false;
	}

	private void prepareForAnalysis() {
		if (toPrepare) {
			Collections.sort(this.results, new TripDataComparator());
			toPrepare = false;
		}
	}

	/*
	 * =========================================================================
	 * helper classes
	 * =========================================================================
	 */
	/**
	 * stocks all relevant information related to a trip
	 */
	private class TripData {
		public final double timeOfDay;
		public final double distance;
		public final int numberOfJoinableTrips;

		public TripData(
				final double tod,
				final double dist,
				final int n) {
			this.timeOfDay = tod;
			this.distance = dist;
			this.numberOfJoinableTrips = n;
		}
	}

	private class TripDataComparator implements Comparator<TripData> {
		public TripDataComparator() {};

		@Override
		public int compare(TripData td1, TripData td2) {
			if (td1.timeOfDay < td2.timeOfDay) {
				return -1;
			}
			else if (td1.timeOfDay > td2.timeOfDay) {
				return 1;
			}
			return 0;
		}
	}
}

