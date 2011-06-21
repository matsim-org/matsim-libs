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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.utils.BoxAndWhiskersChart;

/**
 * Computes and outputs statistics about number of possible shared rides.
 * @author thibautd
 */
public class CountPossibleSharedRides {
	private static final Logger log =
		Logger.getLogger(CountPossibleSharedRides.class);

	private static final double DAY_DUR = 3600d*24d;

	private static final String AGENT_ID_TITLE = "agentId";
	private static final String TOD_TITLE = "departure time (s)";
	private static final String DISTANCE_TITLE = "distance (m)";
	private static final String COUNT_TITLE = "nJoinableTrips";
	private static final String SEPARATOR = "\t";

	private final double eventInitialSearchWindow = 10*60d;
	private final double eventSeachWindowIncr = 5*60d;

	private final double acceptableDistance;
	private final double timeWindowRadius;

	private final EventsTopology arrivalsTopology;
	private final EventsTopology departuresTopology;
	private final EventsTopology enterLinksTopology;
	private final EventsTopology leaveLinksTopology;
	private final Map<Id, Plan> plans = new HashMap<Id, Plan>();

	private final List<TripData> results = new ArrayList<TripData>();

	/**
	 * Used to select whether the joinable trips should be searched in
	 * the neighborhood of the departure/arrival events or based on the
	 * plan leg specification.
	 * <BR>
	 * The event based search may be badly implemented: it searches for the first
	 * event of the good type, on the requested link, around the specified hour,
	 * but nothing allows to check whether this is the event which was searched.
	 * <BR>
	 * Moreover, the meaning of an event-based search is not obvious. Joinable trips are
	 * always searched based on the events.
	 */
	public enum TypeOfSearch {PLAN_BASED, EVENT_BASED};
	private final TypeOfSearch typeOfSearch;

	private boolean toPrepare = false;

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	/**
	 * Base constructor.
	 *
	 * @param network
	 * @param events an {@link EventsAccumulator} corresponding to the output
	 * to analyse
	 * @param population the {@link Population} corresponding to the analysed iteration
	 * @param acceptableDistance the distance an agent is assumed to be willing to
	 * walk to join a meeting point
	 * @param timeWindowRadius the time an agent is assumed to be willing to change its
	 * departure/arrival in the case someone drives him.
	 * @param typeOfSearch how agents' departure and arrival time are computed:
	 * based on events or based on plans.
	 */
	public CountPossibleSharedRides(
			final Network network,
			final EventsAccumulator events,
			final Population population,
			final double acceptableDistance,
			final double timeWindowRadius,
			final TypeOfSearch typeOfSearch) {
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

		this.acceptableDistance = acceptableDistance;
		this.timeWindowRadius = timeWindowRadius;
		this.typeOfSearch = typeOfSearch;
	}

	/**
	 * Initializes an instance using plan information.
	 */
	public CountPossibleSharedRides(
			final Network network,
			final EventsAccumulator events,
			final Population population,
			final double acceptableDistance,
			final double timeWindowRadius) {
		this(network, events, population, acceptableDistance, timeWindowRadius,
				TypeOfSearch.PLAN_BASED);
	}

	/**
	 * Construct an instance with all fields initialized to
	 * null or 0.
	 * To use to load previously stored datafile.
	 */
	public CountPossibleSharedRides() {
		this.arrivalsTopology = null;
		this.departuresTopology = null;
		this.enterLinksTopology = null;
		this.leaveLinksTopology = null;
		this.acceptableDistance = 0d;
		this.timeWindowRadius = 0d;
		this.typeOfSearch = null;
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
		long count = 0L;
		long total = this.plans.size();
		TripData currentTripData;

		log.info("    computing trip data...");
		for (Map.Entry<Id, Plan> plan : this.plans.entrySet()) {
			count++;
			log.info("   treat plan "+count+"/"+total);
			for (PlanElement pe : plan.getValue().getPlanElements()) {
				if ((pe instanceof Leg) &&
						(((Leg) pe).getMode().equals(TransportMode.car))) {
					currentTripData = getTripData((Leg) pe, plan.getKey());

					if (currentTripData == null) {
						log.debug("stopping plan examination.");
						break;
					}

					this.results.add(currentTripData);
				}
			}
		}
		toPrepare = true;
		log.info("    computing trip data... DONE");
	}

	/**
	 * Load from the exported textfile rather than calculating the data.
	 * 
	 * CAUTION: the information displayed in the titles may be inexact (radius and
	 * acceptable distance)!
	 */
	public void loadTripData(final String fileName) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader(fileName);
		
		int agentId=0;
		int tod=1;
		int distance=2;
		int count=3;
		
		// inittialize
		String[] line = nextLine(reader);
		String value;

		for (int i=0; i < line.length; i++) {
			value = line[i];

			if (value.equals(AGENT_ID_TITLE)) {
				agentId = i;
			}
			else if (value.equals(TOD_TITLE)) {
				tod = i;
			}
			else if (value.equals(DISTANCE_TITLE)) {
				distance = i;
			}
			else if (value.equals(COUNT_TITLE)) {
				count = i;
			}
		}

		// load data
		this.results.clear();
		for (line = nextLine(reader); line != null; line = nextLine(reader)) {
			this.results.add(new TripData(
						new IdImpl(line[agentId]),
						Double.parseDouble(line[tod]),
						Double.parseDouble(line[distance]),
						Integer.valueOf(line[count])));
		}
	}

	private String[] nextLine(final BufferedReader reader) throws IOException {
		String line = reader.readLine();

		return (line == null ? null : line.split(SEPARATOR));
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
	public ChartUtil getAveragePerTimeBinChart(final int nTimeBins) {
		this.prepareForAnalysis();

		String title = "Number of possible joint trips per departure time\n"+
				"acceptable distance: "+this.acceptableDistance+"m,\n"+
				"acceptable time: "+(this.timeWindowRadius/60d)+" min.";
		String xLabel = "Time of day (h)";
		String yLabel = "Number of trips";
		XYLineChart output = new XYLineChart(title, xLabel, yLabel);

		// binCount[i][0]: number of trip data taken into account
		// binCount[i][1]: sum of the trip counts for this bin
		int[][] binCounts = new int[nTimeBins][2];
		double[] valueAxisAverage = new double[nTimeBins];
		double[] valueAxisMin = new double[nTimeBins];
		double[] valueAxisMax = new double[nTimeBins];
		double[] timeAxis = computeDataPerTimeBin(
				binCounts, valueAxisMin, valueAxisMax, nTimeBins);

		for (int i=0; i < nTimeBins; i++) {
			valueAxisAverage[i] = (binCounts[i][0] > 0 ? 
					((double) binCounts[i][1]) / ((double) binCounts[i][0]) :
					0d);
		}

		output.addSeries("Min", timeAxis, valueAxisMin);
		output.addSeries("Average", timeAxis, valueAxisAverage);
		output.addSeries("Max", timeAxis, valueAxisMax);
		output.addMatsimLogo();

		return output;
	}

	private double[] computeDataPerTimeBin(
			final int[][] binCount,
			final double[] valueAxisMin,
			final double[] valueAxisMax,
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
		valueAxisMin[0] = Double.POSITIVE_INFINITY;
		valueAxisMax[0] = 0d;

		for (TripData data : this.results) {
			while (data.timeOfDay > timeBinsUpperBounds[currentBin]) {
				currentBin++;
				binCount[currentBin][0] = 0;
				binCount[currentBin][1] = 0;
				valueAxisMin[currentBin] = Double.POSITIVE_INFINITY;
				valueAxisMax[currentBin] = 0d;

				if (valueAxisMin[currentBin - 1] == Double.POSITIVE_INFINITY) {
					// there was no trip
					valueAxisMin[currentBin - 1] = 0d;
				}
			}
			binCount[currentBin][0]++;
			binCount[currentBin][1] += data.numberOfJoinableTrips;

			valueAxisMin[currentBin] = 
				Math.min(valueAxisMin[currentBin], data.numberOfJoinableTrips);
			valueAxisMax[currentBin] = 
				Math.max(valueAxisMax[currentBin], data.numberOfJoinableTrips);
		}

		return timeValues;
	}

	/**
	 * @return a  "box and whiskers" representation of the distribution of the number
	 * of trips joinable per time bin.
	 */
	public ChartUtil getBoxAndWhiskersPerTimeBin(final int nTimeBins) {
		this.prepareForAnalysis();
		String title = "Number of possible joint trips per departure time\n"+
				"acceptable distance: "+this.acceptableDistance+"m,\n"+
				"acceptable time: "+(this.timeWindowRadius/60d)+" min.";
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				title,
				"time of day (h)",
				"n joinable trips",
				24d / nTimeBins);

		for (TripData data : this.results) {
			chart.add(data.timeOfDay / 3600d, data.numberOfJoinableTrips);
		}

		return chart;
	}

	/**
	 * Writes the collected data in the form of a tab separated textfile.
	 * The underlying stream isn't closed.
	 */
	public void writeRawData(final BufferedWriter writer) {
		this.writeRawData(new PrintWriter(writer));
	}

	/**
	 * Writes the collected data in the form of a tab separated textfile.
	 * The underlying stream isn't closed.
	 */
	public void writeRawData(final PrintWriter writer) {
		this.prepareForAnalysis();
		String line = AGENT_ID_TITLE + SEPARATOR
			+ TOD_TITLE + SEPARATOR
			+ DISTANCE_TITLE + SEPARATOR
			+ COUNT_TITLE + SEPARATOR;
		int count = 0;
		int next = 1;

		log.info("printing data to text file...");
		
		for (TripData data : this.results) {
			writer.println(line);
			line = data.id.toString();
			line += "\t" + data.timeOfDay;
			line += "\t" + data.distance;
			line += "\t" + data.numberOfJoinableTrips;

			count++;
			if (count == next) {
				log.info("line # "+count+" processed.");
				next *= 2;
			}
		}
		
		writer.print(line);
		writer.flush();
		// the closing is let to the caller
		log.info("printing data to text file... DONE");
		log.info(count+" lines succesfully written.");
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
		//problem: implies using Route.calcDistances, which costs a lot!
		double distance = route.getDistance();
		int numberOfJoinableTrips = 0;
		Id departureId = route.getStartLinkId();
		Id arrivalId = route.getEndLinkId();

		// correct leg info based on events
		switch (this.typeOfSearch) {
			case EVENT_BASED:
				//log.debug("getting departure events...");
				LinkEvent departure = getDepartureEvent(
						personId,
						departureTime,
						departureId);
				//log.debug("getting departure events... DONE");
				//log.debug("getting arrival events...");
				LinkEvent arrival = getArrivalEvent(
						personId,
						arrivalTime,
						arrivalId);
				if (arrival == null) {
					log.debug("arrival unfound: agent may have been stucked and removed.");
					return null;
				}
				//log.debug("getting arrival events... DONE");

				//log.debug("counting possible shared rides...");
				numberOfJoinableTrips = countPossibleSharedRides(departure, arrival);
				//log.debug("counting possible shared rides... DONE");
				departureTime = departure.getTime();
				break;
			case PLAN_BASED:
				numberOfJoinableTrips = countPossibleSharedRides(
						departureTime,
						departureId,
						arrivalTime,
						arrivalId);
				break;
		}

		return new TripData(personId, departureTime, distance, numberOfJoinableTrips);
	}

	private LinkEvent getDepartureEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		//log.debug("expected time: "+(expectedTime/3600d)+"h");
		return this.getEvent(person, expectedTime, link, this.departuresTopology);
	}

	private LinkEvent getArrivalEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		//log.debug("expected time: "+(expectedTime/3600d)+"h");
		return this.getEvent(person, expectedTime, link, this.arrivalsTopology);
	}

	/**
	 * @return the event of the given person at the given link which is the closer
	 * to expectedTime in the given topology.
	 */
	private LinkEvent getEvent(
			final Id person,
			final double expectedTime,
			final Id link,
			final EventsTopology searchEvents) {
		double timeWindow = this.eventInitialSearchWindow;
		List<LinkEvent> currentEventList;
		int lastSize = -1;

		// increment the tw size until an event corresponding to the agent is
		// found.
		while (true) {
			//System.out.println("tic");
			currentEventList = searchEvents.getEventsInTimeWindow(
					link,
					expectedTime,
					timeWindow);
			// todo: cross a list sorted by temporal distance to the expected value
			for (LinkEvent currentEvent : currentEventList) {
				if (currentEvent.getPersonId().equals(person)) {
					return currentEvent;
				}
			}

			if (searchEvents.wasLastExplorationExhaustive()) {
				// we explored all events
				return null;
			}
			lastSize = currentEventList.size();

			//timeWindow +=  this.eventSeachWindowIncr;
			timeWindow *= 2d;
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
	
		return countPossibleSharedRides(departureNeighbors, arrivalNeighbors);
	}

	private int countPossibleSharedRides(
			final double departureTime,
			final Id departureLink,
			final double arrivalTime,
			final Id arrivalLink
			) {
		List<LinkEvent> departureNeighbors =
			this.leaveLinksTopology.getNeighbors(
					departureTime,
					departureLink);
		List<LinkEvent> arrivalNeighbors =
			this.enterLinksTopology.getNeighbors(
					arrivalTime,
					arrivalLink);
	
		return countPossibleSharedRides(departureNeighbors, arrivalNeighbors);
	}

	private int countPossibleSharedRides(
			final List<LinkEvent> departureNeighbors,
			final List<LinkEvent> arrivalNeighbors) {
		List<Id> alreadyChecked = new ArrayList<Id>(1000);
		Id currentId;
		int count = 0;
		
		// count the number of persons having a "leaveLinkEvent" near the
		// departure and a "enterLinkEvent" near the arrival: they are considered
		// as potential shared ride drivers.
		for (PersonEvent event : departureNeighbors) {
			currentId = event.getPersonId();
			if (( !alreadyChecked.contains(currentId) ) &&
				( passesHere(currentId, arrivalNeighbors) )) {
				alreadyChecked.add(currentId);
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
		public final Id id;

		public TripData(
				final Id id,
				final double tod,
				final double dist,
				final int n) {
			this.id = id;
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

