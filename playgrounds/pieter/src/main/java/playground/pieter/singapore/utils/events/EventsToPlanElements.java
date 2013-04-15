package playground.pieter.singapore.utils.events;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.TravelledEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.TravelledEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/**
 * 
 * @author sergioo
 * 
 */
public class EventsToPlanElements implements TransitDriverStartsEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		AgentDepartureEventHandler,AgentArrivalEventHandler, ActivityStartEventHandler,
		ActivityEndEventHandler, AgentStuckEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, TravelledEventHandler {

	// Private classes
	private class PTVehicle {

		// Attributes
		private Id transitLineId;
		private Id transitRouteId;
		boolean in = false;
		private Map<Id, Double> passengers = new HashMap<Id, Double>();
		private double distance;
		Id lastStop;
		// Constructors
		public PTVehicle(Id transitLineId, Id transitRouteId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
		}

		// Methods
		public void incDistance(double linkDistance) {
			distance += linkDistance;
		}

		public void addPassenger(Id passengerId) {
			passengers.put(passengerId, distance);
		}

		public double removePassenger(Id passengerId) {
			return distance - passengers.remove(passengerId);
		}

	}

	private abstract class PlanElementTypes {
		static final String ACCESSWALK = "accessWalk";
		static final String WAIT = "wait";
		static final String TRANSFERWALK = "transferWalk";
		static final String EGRESSWALK = "egressWalk";
	}

	private class PlanElement {

		double duration;
		double startTime;
		double endTime;
	}
	private class Activity extends PlanElement{		
		Id facility;
		Coord coord;
		String type;
		public String toString(){
			return type;
		}
	}
	
	private class Journey extends PlanElement{
		Activity fromAct;
		Activity toAct;
		LinkedList<Trip> trips = new LinkedList<EventsToPlanElements.Trip>();
		LinkedList<Transfer> transfers = new LinkedList<EventsToPlanElements.Transfer>();
		Coord orig;
		Coord dest;		
		double distance;
		double accessWalkDistance;
		double accessWalkTime;
		double accessWaitTime;
		double egressWalkDistance;
		double egressWalkTime;
		public String toString(){
			return String.format("start: %f end: %f distance: %f", startTime, endTime, distance);
		}
		
	}
	
	private class Trip extends PlanElement{
		Journey journey;
		String mode;
		Id line;
		Id route;
		Coord orig;
		Coord dest;		
		Id boardingStop;
		Id alightingStop;		
		double distance;
		public String toString(){
			return String.format("start: %f end: %f distance: %f", startTime, endTime, distance);
		}
	}
	
	private class Transfer extends PlanElement{
		Journey journey;
		Trip fromTrip;
		Trip toTrip;
		double walkTime;
		double walkDistance;
		double waitTime;
		public String toString(){
			return String.format("start: %f end: %f walkTime: %f", startTime, endTime, walkTime);
		}
	}


	private class TravellerChain {

		Coord lastCoord;
		//use linked lists so I can use the getlast method
		LinkedList<PlanElement> planElements = new LinkedList<EventsToPlanElements.PlanElement>();
		LinkedList<Activity> acts = new LinkedList<EventsToPlanElements.Activity>();
		LinkedList<Journey> journeys = new LinkedList<EventsToPlanElements.Journey>();
		LinkedList<Trip> trips = new LinkedList<EventsToPlanElements.Trip>();
		LinkedList<Transfer> transfers = new LinkedList<EventsToPlanElements.Transfer>();
		double lastTime = 0;
		boolean inPT = false;
		boolean walking = false;
		boolean traveling=false;
		public boolean inCar;
		public boolean traveledVehicle;


	}

	// Attributes
	private Map<Id, TravellerChain> chains = new HashMap<Id, EventsToPlanElements.TravellerChain>();
	private Map<Id, PTVehicle>  ptVehicles = new HashMap<Id, EventsToPlanElements.PTVehicle>();
	private TransitSchedule transitSchedule;
	private Map<Id, Coord> locations = new HashMap<Id, Coord>();
	private Network network;
	private Map<Id, Integer> acts = new HashMap<Id, Integer>();
	private int stuck = 0;

	public EventsToPlanElements(TransitSchedule transitSchedule, Network network) {
		this.transitSchedule = transitSchedule;
		this.network = network;
	}

	// Methods
	@Override
	public void reset(int iteration) {

	}

	private String getMode(String transportMode, Id line) {
		if (transportMode.contains("bus"))
			return "bus";
		else if (transportMode.contains("rail"))
			return "lrt";
		else if (transportMode.contains("subway"))
			if (line.toString().contains("PE")
					|| line.toString().contains("SE")
					|| line.toString().contains("SW"))
				return "lrt";
			else
				return "mrt";
		else
			return "other";
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
			locations.put(event.getPersonId(),
					network.getLinks().get(event.getLinkId()).getCoord());
		if (chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
			chain.acts.add(new Activity());
			Activity act = chain.acts.getLast();
			act.coord = chain.lastCoord;
			act.duration = chain.lastTime;
			act.endTime = chain.lastTime;
			act.facility = event.getFacilityId();
			act.startTime = 0.0;
			act.type = event.getActType();
		
		}else if ( !event.getActType()
						.equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			Activity act = chain.acts.getLast();
			act.duration = event.getTime() - act.startTime;
			act.endTime = event.getTime();
		}
		chain.lastTime = event.getTime();
		chain.lastCoord = network.getLinks().get(event.getLinkId()).getCoord();
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			

		}
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
		boolean beforeInPT = chain.inPT;
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			chain.inPT = true;

		} else {
			chain.walking = false;
			chain.inPT = false;
			chain.acts.add(new Activity());
			Activity act = chain.acts.getLast();
			act.coord = chain.lastCoord;
			act.facility = event.getFacilityId();
			act.startTime  = chain.lastTime;
			act.type = event.getActType();
			chain.journeys.getLast().toAct=act;
			chain.traveling =false;
		}
		chain.lastTime = event.getTime();
		chain.lastCoord = network.getLinks().get(event.getLinkId()).getCoord();
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
	
		if (event.getLegMode().equals("transit_walk")){
			chain.walking = true;
			if(!chain.traveling){
				chain.journeys.add(new Journey());
				Journey journey = chain.journeys.getLast();
				journey.orig = network.getLinks().get(event.getLinkId()).getCoord();
				journey.fromAct = chain.acts.getLast();
				journey.startTime = event.getTime();
			}
		}
		else if(event.getLegMode().equals("car")){
			chain.inCar = true;
			chain.journeys.add(new Journey());
			Journey journey = chain.journeys.getLast();
			journey.orig = network.getLinks().get(event.getLinkId()).getCoord();
			journey.fromAct = chain.acts.getLast();
			journey.startTime = event.getTime();
		}
			chain.walking = false;
		

		chain.lastTime = event.getTime();
		chain.lastCoord = network.getLinks().get(event.getLinkId()).getCoord();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
		if(chain.inCar){
			Journey journey = chain.journeys.getLast();
			journey.dest =  network.getLinks().get(event.getLinkId()).getCoord();
			journey.duration = event.getTime() - journey.startTime;
			chain.inCar=false;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!event.getPersonId().toString().startsWith("pt_tr"))
			if (event.getVehicleId().toString().startsWith("tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				ptVehicles.get(event.getVehicleId()).addPassenger(
						event.getPersonId());
				chain.trips.add(new Trip());
				Trip trip = chain.trips.getLast();
				chain.journeys.getLast().trips.add(trip);
				trip.boardingStop = ptVehicles.get(event.getVehicleId()).lastStop;
				trip.line = ptVehicles.get(event.getVehicleId()).transitLineId;
				trip.orig =  chain.lastCoord;
				trip.route = ptVehicles.get(event.getVehicleId()).transitRouteId;
				trip.startTime = event.getTime();
			}
	}
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!event.getPersonId().toString().startsWith("pt_tr")) {
			if(event.getVehicleId().toString().startsWith("tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				chain.traveledVehicle = true;
				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				double stageDistance = vehicle.removePassenger(event
						.getPersonId());
				Trip trip = chain.trips.getLast();
				trip.alightingStop = ptVehicles.get(event.getVehicleId()).lastStop;
				trip.dest = chain.lastCoord;
				trip.endTime = event.getTime();
				trip.duration = trip.endTime - trip.startTime;
			}
		}
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getVehicleId().toString().startsWith("tr"))
			ptVehicles.get(event.getVehicleId()).in = true;
		
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getVehicleId().toString().startsWith("tr")) {
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			if (vehicle.in)
				vehicle.in = false;
			vehicle.incDistance(network.getLinks().get(event.getLinkId())
					.getLength());
		} else {
			TravellerChain chain = chains.get(event.getPersonId());
			if(chain.inCar){
				chain.journeys.getLast().distance+=network.getLinks().get(event.getLinkId()).getLength();
			}
		}
	}
	@Override
	public void handleEvent(AgentStuckEvent event) {
		if (!event.getPersonId().toString().startsWith("pt_tr")) {
			TravellerChain chain = chains.get(event.getPersonId());
			stuck++;
		}
	}
	@Override
	public void handleEvent(TravelledEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
		if (chain.traveledVehicle)
			chain.traveledVehicle = false;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptVehicles.put(
				event.getVehicleId(),
				new PTVehicle(event.getTransitLineId(), event
						.getTransitRouteId()));
	}
	public static void main(String[] args) throws IOException {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new MatsimNetworkReader(scenario).readFile(args[1]);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToPlanElements test = new EventsToPlanElements(
				scenario.getTransitSchedule(), scenario.getNetwork());
		eventsManager.addHandler(test);
		new MatsimEventsReader(eventsManager).readFile(args[2]);
		System.out.println(test.chains.get(new org.matsim.core.basic.v01.IdImpl("801060")));
	}

}
