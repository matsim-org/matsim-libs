/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.taste_variations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.ExpTransRouteUtils;

/**
 * Reads an events file to obtain from each passenger:
 * 		transit walk time
 * 		travel time inside pt-vehicle
 * 		transit distance inside pt-vehicle
 * 		number of veh changes
 */
public class IndividualPTvaluesFromEvents implements	PersonEntersVehicleEventHandler, 
																			PersonLeavesVehicleEventHandler,
																			VehicleArrivesAtFacilityEventHandler, 
																			TransitDriverStartsEventHandler, 
																			PersonArrivalEventHandler,
																			PersonDepartureEventHandler,
																			ActivityStartEventHandler{
	
	private final static Logger log = Logger.getLogger(IndividualPTvaluesFromEvents.class);
	private Map <Id, Tuple<Id, Id>> vehRouteMap = new TreeMap <Id, Tuple<Id, Id>>();   //stores the current route in which the vehicle is serving veh, tuple<line, route>
	private Map<Id, Id> vehStops = new HashMap<Id, Id>();          //tracks in which stops are traveling vehicles   
	private Set<Id> transitDrivers = new HashSet<Id>();			   
	private Map <Id, AgentEvents> agentEventsMap = new TreeMap <Id, AgentEvents>();
	private Map <Id, IndividualPTvalues> agentIndPTvaluesMap = new TreeMap <Id, IndividualPTvalues>();
	private final TransitSchedule schedule;
	private final Network network;
	private String PTINTERACTION =   PtConstants.TRANSIT_ACTIVITY_TYPE;
	
	public IndividualPTvaluesFromEvents(TransitSchedule schedule, Network network){
		this.schedule = schedule;
		this.network = network;
	}
	
	@Override	 //to identify transit drivers and  transit routes related to each pt vehicle
	public void handleEvent(TransitDriverStartsEvent event) {
		TransitDriverStartsEvent transitDriverStartsEvent = event;
		this.transitDrivers.add(event.getDriverId());
		Tuple<Id,Id> tuple = new Tuple<Id, Id>(transitDriverStartsEvent.getTransitLineId(), transitDriverStartsEvent.getTransitRouteId());
		vehRouteMap.put(transitDriverStartsEvent.getVehicleId(), tuple );  // warning!  vehicles change route dynamically!!  route is updated here
	}
	
    @Override
    public void handleEvent(ActivityStartEvent event) {   						
    	if (this.transitDrivers.contains(event.getPersonId())){
			return; // ignore transit drivers 
		}
    	Id personId = event.getPersonId();      
    	AgentEvents s =  agentEventsMap.get(personId);       //calculate number of changes
    	s.setBeforeLastAct(s.getLastActType());
    	s.setLastActType(s.getThisActType());
    	s.setThisActType(event.getActType());
    	if(s.getBeforeLastAct() != null && s.getLastActType() != null){   // to be sure that the agent has at least done something before
    		if(s.getBeforeLastAct().equals(PTINTERACTION) && s.getLastActType().equals(PTINTERACTION) && s.getThisActType().equals(PTINTERACTION) && s.getLastLegMode().equals(TransportMode.transit_walk)){
    			IndividualPTvalues values = this.agentIndPTvaluesMap.get(personId);
    			values.setChanges(values.getChanges() + 1);
        	}	
    	}
    }
    
	@Override
	public void handleEvent(PersonDepartureEvent event) {	//to calculate transitWalk time
		if (this.transitDrivers.contains(event.getPersonId())){
			return; // ignore transit drivers 
		}
		
		Id personId = event.getPersonId();							//it seems that events from an agent starts always with a departure
		if(!agentEventsMap.keySet().contains(personId)){     // so in this point maps of agents are initialized
    		AgentEvents agentStatus = new AgentEvents();  
			agentEventsMap.put(personId , agentStatus );
    	}
    	if(!agentIndPTvaluesMap.keySet().contains(personId)){
    		IndividualPTvalues indPtValues = new IndividualPTvalues (personId);
    		agentIndPTvaluesMap.put(personId, indPtValues);
    	}
    	
		AgentEvents status= agentEventsMap.get(personId);
		status.setLastLegMode(event.getLegMode());
		status.setLastDepartureTime(event.getTime());
		
		if (event.getLegMode().equals(TransportMode.transit_walk)){
			status.setLastBoardingStop(null);
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {							//to calculate transitWalk time
		if (this.transitDrivers.contains(event.getPersonId())){
			return; // ignore transit drivers 
		}
		if (event.getLegMode().equals(TransportMode.transit_walk)){
			AgentEvents status= agentEventsMap.get(event.getPersonId());
			double walkTime = event.getTime() - status.getLastDepartureTime();
			IndividualPTvalues values = agentIndPTvaluesMap.get(event.getPersonId());
			values.setTrWalkTime(values.getTrWalkTime() + walkTime);
			
			status.setLastDepartureTime(Double.NaN);// to make errors more evident
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId())){
			return; // ignore transit drivers 
		}
		if(!vehRouteMap.keySet().contains(event.getVehicleId())){
			return; // ignore non transit vehicles, we handle now only passengers
		}
		
		AgentEvents status= agentEventsMap.get(event.getPersonId());
		status.setLastLegMode(TransportMode.pt);
		//status.setLastDepartureTime(event.getTime());         //no: the travel time starts in event "departure" as long as it includes the waiting time
		Id stopId = this.vehStops.get(event.getVehicleId());
		status.setLastBoardingStop(stopId);
	}
	
	@Override  //to know later on where passengers get off
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehStops.put( event.getVehicleId(),  event.getFacilityId()  );
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId())){
			return; // ignore transit drivers 
		}
		if(!vehRouteMap.keySet().contains(event.getVehicleId())){
			return; // ignore non transit vehicles, we handle now only passengers
		}
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		Id stopId = this.vehStops.get(vehId);
		double time = event.getTime();
		Tuple<Id,Id> tuple =vehRouteMap.get(vehId); 
		
		Id trLineId = tuple.getFirst();
		Id trRouteId = tuple.getSecond();
		AgentEvents status= agentEventsMap.get(personId);     
		double tripTime =  time- 	status.getLastDepartureTime();
		IndividualPTvalues values = this.agentIndPTvaluesMap.get(event.getPersonId());
		values.setTrTime(values.getTrTime() + tripTime);
		
		TransitLine trLine = schedule.getTransitLines().get(trLineId); //create a ExperimentalTransitRoute to calculate distance
		TransitRoute route = trLine.getRoutes().get(trRouteId);
		Id lastBoardStopId = status.getLastBoardingStop();
		TransitStopFacility accessFacility = schedule.getFacilities().get(lastBoardStopId);
		TransitStopFacility egressFacility = schedule.getFacilities().get(stopId);
		ExperimentalTransitRoute expTrRoute = new ExperimentalTransitRoute(accessFacility, trLine, route, egressFacility);
		ExpTransRouteUtils ptRouteUtill = new ExpTransRouteUtils(network, schedule, expTrRoute);
		double tripDistance =  ptRouteUtill.getExpRouteDistance();
		values.setTrDistance(values.getTrDistance() + tripDistance);
		
		expTrRoute= null;
		ptRouteUtill=null;
		
		status.setLastDepartureTime(Double.NaN);
		status.setLastBoardingStop(null);
		status.setLastLegMode(TransportMode.pt);
	}

	@Override
	public void reset(int iteration) {
		agentEventsMap.clear();
		transitDrivers.clear();
		vehStops.clear();
		vehRouteMap.clear();
		agentIndPTvaluesMap.clear();
	}
	
	protected IndividualPTvalues getIndividualPTvalues(final Id idAgent){
		return agentIndPTvaluesMap.get(idAgent);
	}
	
	protected Map <Id, IndividualPTvalues> getAgentIndPTvaluesMap (){
		return agentIndPTvaluesMap;
	}
	
	protected void resetAgentEvents(final Id idAgent){
		AgentEvents agentEvents= agentEventsMap.get(idAgent);
		agentEvents.setBeforeLastAct(null);
		agentEvents.setLastActType(null);
		agentEvents.setLastBoardingStop(null);
		agentEvents.setLastDepartureTime(0);
		agentEvents.setLastLegMode(null);
		agentEvents.setThisActType(null);
	}
	
	
	protected class AgentEvents {
		
		private Id lastBoardingStop;
		private double lastDepartureTime;
		private String beforeLastActType = null;
		private String lastActType= null;
		private String lastMode= null;
		private String thisActType= null;
			
		/////////////only getters and setters////////
		public String getBeforeLastAct() {return beforeLastActType;}
		public void setBeforeLastAct(String beforeLastAct) {this.beforeLastActType = beforeLastAct;}
		public String getLastActType() {return lastActType;	}
		public void setLastActType(String lastAct) {this.lastActType = lastAct;}
		public String getLastLegMode() {return lastMode;}
		public void setLastLegMode (String lastLeg) {this.lastMode = lastLeg;}
		public String getThisActType() {return thisActType;}
		public void setThisActType (String thisAct) {this.thisActType = thisAct;}
		public Id getLastBoardingStop() {return lastBoardingStop;}
		public void setLastBoardingStop(Id lastBoardingStop) {this.lastBoardingStop = lastBoardingStop;}
		public double getLastDepartureTime() {return lastDepartureTime;}
		public void setLastDepartureTime(double lastDepartureTime) {this.lastDepartureTime = lastDepartureTime;}
	}
	
	public static void main(String[] args) {
		String netFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String inputEventFile = "../../input/juli/deleteme/1000.events.xml";
		
		//get agents ids
		DataLoader loader = new DataLoader();
		TransitSchedule schedule = loader.readTransitSchedule(scheduleFile);
		Network net = loader.readNetwork(netFile);
		loader = null;
		
		//read and filter out events
		IndividualPTvaluesFromEvents ptEventsAnalyzer = new IndividualPTvaluesFromEvents(schedule, net); 
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ptEventsAnalyzer);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inputEventFile);
		log.info("Events file read");
		
		Id<Person> agentId = Id.create("24140963X3", Person.class);
		IndividualPTvalues status = ptEventsAnalyzer.getIndividualPTvalues(agentId);
		System.out.println(status.getChanges());
		System.out.println(status.getTrDistance());
		System.out.println(status.getTrTime());
		System.out.println(status.getTrWalkTime());
//		System.out.println("===================");
//		agentId = new IdImpl("11156831");
//		status = ptEventsAnalyzer.getIndividualPTvalues(agentId);
//		System.out.println(status.getChanges());
//		System.out.println(status.getTrDistance());
//		System.out.println(status.getTrTime());
//		System.out.println(status.getTrWalkTime());

		
	}

}