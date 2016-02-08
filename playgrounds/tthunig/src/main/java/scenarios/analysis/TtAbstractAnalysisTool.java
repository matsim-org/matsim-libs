/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * Abstract tool to analyze a MATSim simulation of an arbitrary scenario.
 * 
 * It calculates the total and average travel time on the routes and the number
 * of users on each route. Both once in total and once depending on the
 * departure and arrival times.
 * 
 * Additionally it calculates the number of route starts per second and the
 * number of agents on each route per second.
 * 
 * Last but not least it calculates the total travel time in the network.
 * 
 * Note: To use this analyzer you have to implement the abstract methods. I.e.
 * you have to define the number of different routes of your specific scenario
 * and implement a unique route determination via link enter events.
 * 
 * Note: This class calculates travel times via departure and arrival events.
 * I.e. it only gives reliable results if all agents can departure without delay
 * regarding to the flow capacity of the first link. If they are delayed because
 * of storage capacity the results are still fine.
 * 
 * The results may be plotted by gnuplot scripts (see e.g.
 * runs-svn/braess/analysis).
 * 
 * @author tthunig, tschlenther
 */
public abstract class TtAbstractAnalysisTool implements PersonArrivalEventHandler,
PersonDepartureEventHandler, LinkEnterEventHandler, PersonStuckEventHandler, PersonEntersVehicleEventHandler{

	private static final Logger log = Logger.getLogger(TtAbstractAnalysisTool.class);
	
	private double totalTT;
	private double[] totalRouteTTs;
	private int[] routeUsers;
	private int numberOfStuckedAgents = 0;

	// collects the departure times per person
	private Map<Id<Person>, Double> personDepartureTimes;
	
	// collects information about the used route per person
	private Map<Id<Person>, Integer> personRouteChoice;
	// counts the number of route starts per second (gets filled when the agent
	// arrives)
	private Map<Double, int[]> routeStartsPerSecond;
	// counts the number of agents on each route per second
	private Map<Double, int[]> onRoutePerSecond;

	private Map<Double, double[]> totalRouteTTsByDepartureTime;
	private Map<Double, int[]> routeUsersByDepartureTime;
	
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicle2PersonsMap;
	
	private int numberOfRoutes;

	public TtAbstractAnalysisTool() {
		super();
		defineNumberOfRoutes();
		reset(0);
	}
	
	/**
	 * resets all fields
	 */
	@Override
	public void reset(int iteration) {
		this.totalTT = 0.0;
		this.totalRouteTTs = new double[numberOfRoutes];
		this.routeUsers = new int[numberOfRoutes];
		this.numberOfStuckedAgents = 0;
		
		this.personDepartureTimes = new HashMap<>();
		this.personRouteChoice = new HashMap<>();
		this.routeStartsPerSecond = new TreeMap<>();
		this.onRoutePerSecond = new TreeMap<>();

		this.totalRouteTTsByDepartureTime = new TreeMap<>();
		this.routeUsersByDepartureTime = new TreeMap<>();
		
		this.vehicle2PersonsMap = new HashMap<>();
	}
	
	/**
	 * Defines the field variable numberOfRoutes for the specific scenario by
	 * using the setter method.
	 */
	protected abstract void defineNumberOfRoutes();

	/**
	 * Creates a mapping between vehicles and their occupants
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event){
		if (!vehicle2PersonsMap.containsKey(event.getVehicleId()))
			vehicle2PersonsMap.put(event.getVehicleId(), new HashSet<Id<Person>>());
		vehicle2PersonsMap.get(event.getVehicleId()).add(event.getPersonId());
	}
	
	/**
	 * Remembers the persons departure times.
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"A person has departured at least two times without arrival.");
		}

		// remember the persons departure time
		this.personDepartureTimes.put(event.getPersonId(), event.getTime());
	}

	/**
	 * Determines the agents route choice.
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int route = determineRoute(event);

		// if a route was determined
		if (route != -1) {
			// remember the route choice for all persons inside the vehicle
			for (Id<Person> occupantId : vehicle2PersonsMap.get(event.getVehicleId())){
				if (this.personRouteChoice.containsKey(occupantId))
					throw new IllegalStateException("Person " + occupantId + " was seen at least twice on a route specific link."
						+ " Did it travel more than once without arrival?");

				this.personRouteChoice.put(occupantId, route);
			}
		}
	}

	/**
	 * Determines the vehicles route choice if it is unique.
	 * 
	 * @return the route id (counts from 0 to numberOfRoutes)
	 */
	protected abstract int determineRoute(LinkEnterEvent linkEnterEvent);
	
	
	/**
	 * Calculates the total travel time and the route travel time of the agent.
	 * 
	 * Fills all fields and maps with the person specific route and travel time
	 * informations.
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " has arrived without departure.");
		}
		if (!this.personRouteChoice.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " arrived, but was not seen on any route.");
		}

		// calculate total travel time
		double personArrivalTime = event.getTime();
		double personDepartureTime = this.personDepartureTimes.get(event
				.getPersonId());
		double personTotalTT = personArrivalTime - personDepartureTime;
		this.totalTT += personTotalTT;

		// store route specific information
		int personRoute = this.personRouteChoice.get(event.getPersonId());
		this.totalRouteTTs[personRoute] += personTotalTT;
		this.routeUsers[personRoute]++;

		// fill maps for calculating avg tt per route
		if (!this.totalRouteTTsByDepartureTime.containsKey(personDepartureTime)) {
			// this is equivalent to
			// !this.routeUsersPerDepartureTime.containsKey(personDepartureTime)
			this.totalRouteTTsByDepartureTime.put(personDepartureTime, new double[numberOfRoutes]);
			this.routeUsersByDepartureTime.put(personDepartureTime, new int[numberOfRoutes]);
		}
		this.totalRouteTTsByDepartureTime.get(personDepartureTime)[personRoute] += personTotalTT;
		this.routeUsersByDepartureTime.get(personDepartureTime)[personRoute]++;

		// increase the number of persons on route for each second the
		// person is traveling on it
		for (int i = 0; i < personTotalTT; i++) {
			if (!this.onRoutePerSecond.containsKey(personDepartureTime + i)) {
				this.onRoutePerSecond.put(personDepartureTime + i, new int[numberOfRoutes]);
			}
			this.onRoutePerSecond.get(personDepartureTime + i)[this.personRouteChoice
					.get(event.getPersonId())]++;
		}

		// add one route start for the specific departure time
		if (!this.routeStartsPerSecond.containsKey(personDepartureTime)) {
			this.routeStartsPerSecond.put(personDepartureTime, new int[numberOfRoutes]);
		}
		this.routeStartsPerSecond.get(personDepartureTime)[personRoute]++;

		// remove all trip dependent information of the arrived person
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("Agent " + event.getPersonId() + " stucked on link " + event.getLinkId());
		if (numberOfStuckedAgents == 0){
			log.warn("This handler counts stucked agents but doesn't consider its travel times or route choice.");
		}
		
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
		
		numberOfStuckedAgents++;
	}

	/**
	 * Calculates and returns the average travel times on the single routes in
	 * Braess' example. The first entry corresponds to the upper route, the
	 * second to the middle route and the third to the lower route.
	 * 
	 * @return average travel times
	 */
	public double[] calculateAvgRouteTTs() {
		double[] avgRouteTTs = new double[numberOfRoutes];
		for (int i = 0; i < numberOfRoutes; i++) {
			avgRouteTTs[i] = this.totalRouteTTs[i] / this.routeUsers[i];
		}
		return avgRouteTTs;
	}

	public double getTotalTT() {
		return totalTT;
	}

	public double[] getTotalRouteTTs() {
		return totalRouteTTs;
	}

	public int[] getRouteUsers() {
		return routeUsers;
	}

	/**
	 * @return a map containing the number of route starts for each time step.
	 * Thereby a route start is the departure event of an agent using this route.
	 */
	public Map<Double, int[]> getRouteDeparturesPerSecond() {
		// determine minimum and maximum departure time
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerSecond.keySet());
		
		// fill missing time steps between first and last departure with zero starts
		// note: matsim departure times are always integer
		for (long l = firstLastDepartureTuple.getFirst().longValue(); l <= firstLastDepartureTuple.getSecond().longValue(); l++) {
			if (!this.routeStartsPerSecond.containsKey((double) l)) {
				this.routeStartsPerSecond.put((double) l, new int[numberOfRoutes]);
			}
		}

		return routeStartsPerSecond;
	}
	
	/**
	 * @param set
	 * @return minimum and maximum entry of the set of doubles
	 */
	private static Tuple<Double, Double> determineMinMaxDoubleInSet(Set<Double> set) {
		double minEntry = Long.MAX_VALUE;
		double maxEntry = Long.MIN_VALUE;
		for (Double currentEntry : set) {
			if (currentEntry < minEntry)
				minEntry = currentEntry;
			if (currentEntry > maxEntry)
				maxEntry = currentEntry;
		}
		return new Tuple<Double, Double>(minEntry, maxEntry);
	}

	/** 
	 * @return a map containing the number of bygone route starts for each time step.
	 * Thereby a route start is the departure event of an agent using this route.
	 * For each time step all bygone route starts are summed up.
	 */
	public Map<Double, int[]> getSummedRouteDeparturesPerSecond(){
		
		// determine minimum and maximum departure time
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerSecond.keySet());
		
		Map<Double, int[]> summedRouteDeparturesPerSecond = new TreeMap<>();
		
		// create a map entry for each time step between minimum and maximum departure time
		// note: matsim departure times are always integer. time steps are assumed to be 1
		for (long second = firstLastDepartureTuple.getFirst().longValue(); 
				second <= firstLastDepartureTuple.getSecond().longValue(); second++) {
			
			// initialize departure array as {0,...,0}
			summedRouteDeparturesPerSecond.put((double) second, new int[numberOfRoutes]);
			for (int i = 0; i < numberOfRoutes; i++){
				// add value from the previous second if second > min
				if (second > firstLastDepartureTuple.getFirst().longValue()){
					summedRouteDeparturesPerSecond.get((double)second)[i] +=
							summedRouteDeparturesPerSecond.get((double)second - 1)[i];
				}
				// increment for every departure in this second
				if (this.routeStartsPerSecond.containsKey((double) second)) {
					summedRouteDeparturesPerSecond.get((double) second)[i] += 
							this.routeStartsPerSecond.get((double) second)[i];
				}
			}			
		}
		
		return summedRouteDeparturesPerSecond;
	}

	/**
	 * @return the number of agents on route (between departure and arrival
	 * event) per time step.
	 */
	public Map<Double, int[]> getOnRoutePerSecond() {
		// already contains entries for all time steps (seconds)
		// between first departure and last arrival
		return onRoutePerSecond;
	}

	/**
	 * @return the average route travel times by departure time.
	 * 
	 * Thereby the double array in each map entry contains the average
	 * route travel times for all different routes in the network
	 * (always for agents with the specific departure time)
	 */
	public Map<Double, double[]> calculateAvgRouteTTsByDepartureTime() {
		Map<Double, double[]> avgTTsPerRouteByDepartureTime = new TreeMap<>();

		// calculate average route travel times for existing departure times
		for (Double departureTime : this.totalRouteTTsByDepartureTime.keySet()) {
			double[] totalTTsPerRoute = this.totalRouteTTsByDepartureTime
					.get(departureTime);
			int[] usersPerRoute = this.routeUsersByDepartureTime
					.get(departureTime);
			double[] avgTTsPerRoute = new double[numberOfRoutes];
			for (int i = 0; i < numberOfRoutes; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is departing for the specific route at this time
					avgTTsPerRoute[i] = Double.NaN;
				else
					avgTTsPerRoute[i] = totalTTsPerRoute[i] / usersPerRoute[i];
			}
			avgTTsPerRouteByDepartureTime.put(departureTime, avgTTsPerRoute);
		}

		// fill missing time steps between first and last departure
		long firstDeparture = Long.MAX_VALUE;
		long lastDeparture = Long.MIN_VALUE;
		for (Double departureTime : avgTTsPerRouteByDepartureTime.keySet()) {
			// matsim departure times are always integer
			if (departureTime < firstDeparture)
				firstDeparture = departureTime.longValue();
			if (departureTime > lastDeparture)
				lastDeparture = departureTime.longValue();
		}
		for (long l = firstDeparture; l <= lastDeparture; l++) {
			if (!avgTTsPerRouteByDepartureTime.containsKey((double) l)) {
				// add NaN-values as travel times when no agent has a departure
				double[] nanTTsPerRoute = new double[numberOfRoutes];
				for (int i = 0; i < numberOfRoutes; i++){
					nanTTsPerRoute[i] = Double.NaN;
				}
				avgTTsPerRouteByDepartureTime.put((double) l, nanTTsPerRoute);
			}
		}

		return avgTTsPerRouteByDepartureTime;
	}

	public int getNumberOfStuckedAgents() {
		return numberOfStuckedAgents;
	}

	public void setNumberOfRoutes(int numberOfRoutes) {
		this.numberOfRoutes = numberOfRoutes;
	}

	public int getNumberOfRoutes() {
		return numberOfRoutes;
	}
}
