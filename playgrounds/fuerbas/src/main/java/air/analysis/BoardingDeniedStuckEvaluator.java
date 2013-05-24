/* *********************************************************************** *
 * project: org.matsim.*
 * BoardingDeniedStuckedEvaluator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import air.analysis.CollectBoardingDeniedStuckEventHandler.PersonEvents;


/**
 * @author dgrether
 *
 */
public class BoardingDeniedStuckEvaluator {

	private static final Logger log = Logger.getLogger(BoardingDeniedStuckEvaluator.class);


	
	private Map<Id, PersonEvents> personStats;
	private Population population;
	private Map<Id, PersonEvents> boardingDeniedNoStuck = new HashMap<Id, PersonEvents>();

	private Map<Id, BoardingDeniedStuck> airportStuckCountMap = new HashMap<Id, BoardingDeniedStuck>();
	private Map<Tuple<Id, Id>, BoardingDeniedStuck> odStuckCountMap = new HashMap<Tuple<Id, Id>, BoardingDeniedStuck>();
	
	public BoardingDeniedStuckEvaluator(Map<Id, PersonEvents> map, Population pop) {
		this.personStats = map;
		this.population = pop;
	}

	public void evaluateAndWrite(String outputFilePrefix) {
		this.evalutateStuckStats();
		String odStuckStats = outputFilePrefix + "stucked_by_od_pair.csv";
		this.writeOdStuckStats(odStuckStats);
		String airportStuckStats = outputFilePrefix + "stucked_by_airport.csv";
		this.writeAirportStuckStats(airportStuckStats);
		log.info("No Stuck but Boarding Denied: " + boardingDeniedNoStuck.size());
	}

	private void writeAirportStuckStats(String filename) {
		String header = "Airport;StuckAndBoardingDenied;StuckNOBoardingDenied;StuckSum";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write(header);
			bw.newLine();
			for (Entry<Id, BoardingDeniedStuck> e : this.airportStuckCountMap.entrySet()) {
				StringBuilder sb = new StringBuilder();
				sb.append(e.getKey().toString());
				sb.append(";");
				BoardingDeniedStuck bds = e.getValue();
				sb.append(Integer.toString(bds.stuckAndBoardingDenied));
				sb.append(";");
				sb.append(Integer.toString(bds.stuckNoBoardingDenied));
				sb.append(";");
				sb.append(Integer.toString(bds.stuckAndBoardingDenied + bds.stuckNoBoardingDenied));
				sb.append(";");
				bw.append(sb.toString());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeOdStuckStats(String filename) {
		String header = "Origin;Destination;StuckAndBoardingDenied;StuckNOBoardingDenied;StuckSum";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write(header);
			bw.newLine();
			for (Entry<Tuple<Id, Id>, BoardingDeniedStuck> e : this.odStuckCountMap.entrySet()) {
				StringBuilder sb = new StringBuilder();
				sb.append(e.getKey().getFirst().toString());
				sb.append(";");
				sb.append(e.getKey().getSecond().toString());
				sb.append(";");
				BoardingDeniedStuck bds = e.getValue();
				sb.append(Integer.toString(bds.stuckAndBoardingDenied));
				sb.append(";");
				sb.append(Integer.toString(bds.stuckNoBoardingDenied));
				sb.append(";");
				sb.append(Integer.toString(bds.stuckAndBoardingDenied + bds.stuckNoBoardingDenied));
				sb.append(";");
				bw.append(sb.toString());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final class BoardingDeniedStuck {
		int stuckAndBoardingDenied = 0;
		int stuckNoBoardingDenied = 0;
	}

	private void evalutateStuckStats(){
		for (Entry<Id, PersonEvents> entry : this.personStats.entrySet()){
			PersonEvents pe = entry.getValue();
			Tuple<Id, Id> odTuple = this.getOdTuple(entry.getKey());
			BoardingDeniedStuck odCount = this.getOdCount(odTuple);
			if (pe.stuckEvent != null) { // STUCK
				Id airportCode = pe.stuckEvent.getLinkId();
				BoardingDeniedStuck airportCount = this.getAirportCount(airportCode);
				if (pe.boardingDeniedEvents.isEmpty()) { // NO BOARDING DENIED
					airportCount.stuckNoBoardingDenied++;
					odCount.stuckNoBoardingDenied++;
				}
				else { // BOARDING DENIED
					airportCount.stuckAndBoardingDenied++;
					odCount.stuckAndBoardingDenied++;
				}
			}
			else { // NO  STUCK
				if (pe.boardingDeniedEvents.isEmpty()) {
					throw new IllegalStateException("Neither boarding denied nor stuck event in result, should not be collected!");
				}
				this.boardingDeniedNoStuck.put(entry.getKey(), pe);
			}
		}
	}

	private Tuple<Id, Id> getOdTuple(Id personId) {
		Person person = this.population.getPersons().get(personId);
		Plan plan = person.getPlans().get(0);
		Id fromLinkId = ((Activity)plan.getPlanElements().get(0)).getLinkId();
		Id toLinkId = ((Activity)plan.getPlanElements().get(2)).getLinkId();
		Tuple<Id, Id> t = new Tuple<Id, Id>(fromLinkId, toLinkId);
		return t;
	}
	
	private BoardingDeniedStuck getAirportCount(Id airportCode){
		BoardingDeniedStuck airportCount = airportStuckCountMap.get(airportCode);
		if (airportCount == null) {
			airportCount = new BoardingDeniedStuck();
			airportStuckCountMap.put(airportCode, airportCount);
		}
		return airportCount;
	}
	
	private BoardingDeniedStuck getOdCount(Tuple<Id, Id> odTuple) {
		BoardingDeniedStuck odCount = odStuckCountMap.get(odTuple);
		if (odCount == null) {
			odCount = new BoardingDeniedStuck();
			odStuckCountMap.put(odTuple, odCount);
		}
		return odCount;
	}
	
	
	

}
