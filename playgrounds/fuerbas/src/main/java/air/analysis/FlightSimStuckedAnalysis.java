/* *********************************************************************** *
 * project: org.matsim.*
 * FlightSimStuckedAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import air.demand.FlightODRelation;



/**
 * @author dgrether
 *
 */
public class FlightSimStuckedAnalysis implements AgentDepartureEventHandler, AgentStuckEventHandler, BoardingDeniedEventHandler{
	
	private static final Logger log = Logger.getLogger(FlightSimStuckedAnalysis.class);
	
	private final static class PersonEvents {
		List<BoardingDeniedEvent> boardingDeniedEvents = new ArrayList<BoardingDeniedEvent>();
		AgentStuckEvent stuckEvent = null;
	}

	
	private Map<Id, AgentDepartureEvent> agent2DepartureEventMap;
	private Map<Id, PersonEvents> personStats;
	private SortedMap<String, SortedMap<String, FlightODRelation>> fromAirport2FlightOdRelMap;
	private int totalStuck;
	private int totalDirectFlights;
	private double totalBoardingDenied;
	
	public FlightSimStuckedAnalysis(){
		this.reset(0);
	}

	@Override
	public void handleEvent(BoardingDeniedEvent e) {
		if (! this.personStats.containsKey(e.getPersonId())){
			this.personStats.put(e.getPersonId(), new PersonEvents());
		}
		this.personStats.get(e.getPersonId()).boardingDeniedEvents.add(e);
		
	}
	
	@Override
	public void handleEvent(AgentStuckEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.totalStuck++;
			if (! this.personStats.containsKey(event.getPersonId())){
				this.personStats.put(event.getPersonId(), new PersonEvents());
			}
			this.personStats.get(event.getPersonId()).stuckEvent = event;
			
			AgentDepartureEvent departureEvent = this.agent2DepartureEventMap.get(event.getPersonId());
			String fromAirport = departureEvent.getLinkId().toString();
			String toAirport = "stuck";
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.agent2DepartureEventMap.put(event.getPersonId(), event);
		}
	}
	
	public void evaluate(){
		Map<Id, PersonEvents> boardingDeniedNoStuck = new HashMap<Id, PersonEvents>();
		Map<Id, PersonEvents> noBoardingDeniedButStuck = new HashMap<Id, PersonEvents>();
		Map<Id, PersonEvents> boardingDeniedAndStuck = new HashMap<Id, PersonEvents>();
		
		for (Entry<Id, PersonEvents> entry : this.personStats.entrySet()){
			if (! entry.getValue().boardingDeniedEvents.isEmpty()){
				if (entry.getValue().stuckEvent == null){
					boardingDeniedNoStuck.put(entry.getKey(), entry.getValue());
				}
				else {
					boardingDeniedAndStuck.put(entry.getKey(), entry.getValue());
				}
			}
			else {
				noBoardingDeniedButStuck.put(entry.getKey(), entry.getValue());
			}
		}
		log.info("Evaluating boarding denied and stuck passengers...");
		this.evalutateStuckStats(boardingDeniedAndStuck);
		log.info("");
		log.info("Evaluating NO boarding denied and stuck passengers...");
		this.evalutateStuckStats(noBoardingDeniedButStuck);
		log.info("");
		log.info("Boarding Denied No Stuck: " + boardingDeniedNoStuck.size());
		log.info("No Boarding Denied But Stuck: " + noBoardingDeniedButStuck.size());
		log.info("Boarding Denied And Stuck: " + boardingDeniedAndStuck.size());
	}
	
	private void evalutateStuckStats(Map<Id, PersonEvents> map){
		Map<Id, Integer> airportStuckCountMap = new HashMap<Id, Integer>();
		for (Entry<Id, PersonEvents> entry : map.entrySet()){
			PersonEvents pe = entry.getValue();
			Id airportCode = pe.stuckEvent.getLinkId();
			Integer count = airportStuckCountMap.get(airportCode);
			if (count == null) {
				count = 0;
			}
			count++;
			airportStuckCountMap.put(airportCode, count);
		}
		log.info("Persons stucked at airports: ");
		this.printMapToLog(airportStuckCountMap);
		
	}
	
	private void printMapToLog(Map  m){
		for (Object key : m.keySet()) {
			log.info("  Key: " + key + " Value " + m.get(key));
		}
	}
	
	private void printToLog(Entry<Id, PersonEvents> entry){
		log.info("Person Id: " + entry.getKey());
		log.info("  BoardingDeniedEvents: " );
		for (BoardingDeniedEvent event  : entry.getValue().boardingDeniedEvents){
			log.info("  " + event);
		}
		log.info("  StuckEvent: " );
		log.info("  " + entry.getValue().stuckEvent);
	}
	
	@Override
	public void reset(int iteration) {
		this.personStats = new HashMap<Id, PersonEvents>();
		this.agent2DepartureEventMap = new HashMap<Id, AgentDepartureEvent>();
		this.fromAirport2FlightOdRelMap = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		this.totalStuck = 0;
		this.totalDirectFlights = 0;
		this.totalBoardingDenied = 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] runNumbers ={
				"1836", 
//				"1837",
//				"1838",
//				"1839",
//				"1840",
//				"1841"				
		};
		String iteration = "600";
		
		for (String runNumber : runNumbers) {
			String events = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".events.xml.gz";
			String outputFile = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".simulated_direct_flights.csv";

			EventsManager eventsManager = EventsUtils.createEventsManager();
			FlightSimStuckedAnalysis handler = new FlightSimStuckedAnalysis();
			eventsManager.addHandler(handler);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(events);
			handler.evaluate();
		}
	}


}
