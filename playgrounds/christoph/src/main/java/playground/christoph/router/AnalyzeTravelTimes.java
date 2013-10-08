/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeTravelTimes.java
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

package playground.christoph.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class AnalyzeTravelTimes implements PersonArrivalEventHandler, PersonDepartureEventHandler {

	private static final Logger log = Logger.getLogger(AnalyzeTravelTimes.class);
		
	private final Set<Id> replannedPersons;
	private final List<Double> replannedTrips;
	private final List<Double> notReplannedTrips;
	private final Map<Id, Double> activeTrips;
	
	private final int numPersons;
	
	public AnalyzeTravelTimes(Set<Id> replannedPersons, int populationSize) {
		this.activeTrips = new HashMap<Id, Double>();
		this.replannedPersons = replannedPersons;
		this.replannedTrips = new ArrayList<Double>();
		this.notReplannedTrips = new ArrayList<Double>();
		
		this.numPersons = populationSize;
	}

	public void handleEvent(PersonDepartureEvent event) {
		activeTrips.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(PersonArrivalEvent event) {
		double travelTime = event.getTime() - activeTrips.remove(event.getPersonId());
		
		if (replannedPersons.contains(event.getPersonId())) replannedTrips.add(travelTime);
		else notReplannedTrips.add(travelTime);
	}

	public void reset(int iteration) {
		activeTrips.clear();
		replannedTrips.clear();
		notReplannedTrips.clear();
	}		
	
	public void printStatistics() {
	
		double totalMean = 0.0;
		
		double meanReplannedTrip = 0.0;
		for (double d : this.replannedTrips) {
			meanReplannedTrip += d;
			totalMean += d;
		}
		meanReplannedTrip = meanReplannedTrip / this.replannedTrips.size();
		
		double meanNotReplannedTrip = 0.0;
		for (double d : this.notReplannedTrips) {
			meanNotReplannedTrip += d;
			totalMean += d;
		}
		meanNotReplannedTrip = meanNotReplannedTrip / this.notReplannedTrips.size();
		
		int totalTrips = this.replannedTrips.size() + this.notReplannedTrips.size();
		totalMean = totalMean / totalTrips;
		
		double tripsPerPerson = (double)totalTrips / (double)numPersons;
				
		double meanReplannedAbsoluteTripDeviation = 0.0;
		for (double d : this.replannedTrips) {
			meanReplannedAbsoluteTripDeviation += Math.abs(meanReplannedTrip - d);
		}
		meanReplannedAbsoluteTripDeviation = meanReplannedAbsoluteTripDeviation / this.replannedTrips.size();
		
		double s2 = 0.0;
		for (double d : this.replannedTrips) {
			s2 = s2 + Math.pow(d - meanReplannedTrip, 2);
		}
		s2 = s2 / (this.replannedTrips.size() - 1);
		double replannedStandardDeviation = Math.pow(s2, 0.5);
		
		double meanNotReplannedAbsoluteTripDeviation = 0.0;
		for (double d : this.notReplannedTrips) {
			meanNotReplannedAbsoluteTripDeviation += Math.abs(meanNotReplannedTrip - d);
		}
		meanNotReplannedAbsoluteTripDeviation = meanNotReplannedAbsoluteTripDeviation / this.notReplannedTrips.size();
		
		s2 = 0.0;
		for (double d : this.notReplannedTrips) {
			s2 = s2 + Math.pow(d - meanNotReplannedTrip, 2);
		}
		s2 = s2 / (this.notReplannedTrips.size() - 1);
		double notReplannedStandardDeviation = Math.pow(s2, 0.5);
		
		List<Double> totalTripsList = new ArrayList<Double>();
		totalTripsList.addAll(replannedTrips);
		totalTripsList.addAll(notReplannedTrips);
		double meanTotalAbsoluteTripDeviation = 0.0;
		for (double d : totalTripsList) {
			meanTotalAbsoluteTripDeviation += Math.abs(totalMean - d);
		}
		meanTotalAbsoluteTripDeviation = meanTotalAbsoluteTripDeviation / totalTrips;
		
		s2 = 0.0;
		for (double d : totalTripsList) {
			s2 = s2 + Math.pow(d - totalMean, 2);
		}
		s2 = s2 / (totalTrips - 1);
		double totalStandardDeviation = Math.pow(s2, 0.5);
		
		log.info("");
		log.info("number of Persons: \t" + numPersons);
		log.info("number of Trips: \t" + totalTrips);
		log.info("number of (probably) replanned Trips: \t" + this.replannedTrips.size());
		log.info("number of not replanned Trips: \t" + this.notReplannedTrips.size());
		log.info("");
		log.info("mean replanned Trip length: \t" + meanReplannedTrip);
		log.info("mean not replanned Trips length: \t" + meanNotReplannedTrip);
		log.info("mean total Trips length: \t" + totalMean);
		log.info("");
		log.info("mean replanned daily Trip length: \t" + meanReplannedTrip * tripsPerPerson);
		log.info("mean not replanned daily Trips length: \t" + meanNotReplannedTrip * tripsPerPerson);
		log.info("mean total daily Trips length: \t" + totalMean * tripsPerPerson);
		log.info("");
		log.info("mean absolute replanned trip deviation: \t" + meanReplannedAbsoluteTripDeviation);
		log.info("replanned trips standard deviation: \t" + replannedStandardDeviation);
		log.info("");
		log.info("mean absolute not replanned trip deviation: \t" + meanNotReplannedAbsoluteTripDeviation);
		log.info("not replanned trips standard deviation: \t" + notReplannedStandardDeviation);
		log.info("");
		log.info("mean total absolute trip deviation: \t" + meanTotalAbsoluteTripDeviation);
		log.info("total trips standard deviation: \t" + totalStandardDeviation);
		log.info("");
	}

}