/* *********************************************************************** *
 * project: org.matsim.*
 * DgCalcSimulatedOdMatrix
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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;

import air.demand.FlightODRelation;


/**
 * Collects data from events for analysis of flight scenario passenger demand. 
 * @author dgrether
 *
 */
public class DgFlightDemandEventHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler, BoardingDeniedEventHandler{

	private Map<Id, PersonDepartureEvent> agent2DepartureEventMap;
	private SortedMap<String, SortedMap<String, FlightODRelation>> fromAirport2FlightOdRelMap;
	private int totalStuck;
	private int totalDirectFlights;
	private double totalBoardingDenied;
	
	public DgFlightDemandEventHandler(){
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.agent2DepartureEventMap = new HashMap<Id, PersonDepartureEvent>();
		this.fromAirport2FlightOdRelMap = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		this.totalStuck = 0;
		this.totalDirectFlights = 0;
		this.totalBoardingDenied = 0;
	}

	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.totalStuck++;
			PersonDepartureEvent departureEvent = this.agent2DepartureEventMap.get(event.getPersonId());
			String fromAirport = departureEvent.getLinkId().toString();
			String toAirport = "stuck";
			this.addFromToRelation(fromAirport, toAirport);
		}
	}
	
	private void addFromToRelation(String fromAirport, String toAirport){
		SortedMap<String, FlightODRelation> toRelations = this.fromAirport2FlightOdRelMap.get(fromAirport);
		if (toRelations == null ) {
			toRelations = new TreeMap<String, FlightODRelation>();
			this.fromAirport2FlightOdRelMap.put(fromAirport, toRelations);
		}
		FlightODRelation odRel = toRelations.get(toAirport);
		if (odRel == null){
			toRelations.put(toAirport, new FlightODRelation(fromAirport, toAirport, new Double(1)));
		}
		else {
			odRel.setNumberOfTrips(odRel.getNumberOfTrips() + 1);
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.totalDirectFlights++;
			PersonDepartureEvent departureEvent = this.agent2DepartureEventMap.get(event.getPersonId());
			String fromAirport = departureEvent.getLinkId().toString();
			String toAirport = event.getLinkId().toString();
			this.addFromToRelation(fromAirport, toAirport);
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.agent2DepartureEventMap.put(event.getPersonId(), event);
		}
	}
	
	public SortedMap<String, SortedMap<String, FlightODRelation>> getDirectFlightODRelations(){
		return this.fromAirport2FlightOdRelMap;
	}
	
	public double getStuck() {
		return this.totalStuck;
	}

	public double getBoardingDenied() {
		return this.totalBoardingDenied;
	}
	
	public int getNumberOfDirectFlights(){
		return this.totalDirectFlights;
	}
	
	@Override
	public void handleEvent(BoardingDeniedEvent e) {
		this.totalBoardingDenied++;
	}
	
	

	
	

}
