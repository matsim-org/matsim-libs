package playground.pieter.singapore.utils.events;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

		int planElementID = -1;
		int activityID = -1;
		int journeyID = -1;
		int tripID = -1;
		int transferID = -1;
		String planElementType;
		Coord orig;
		Coord dest;
		Id origFacility;
		Id destFacility;
		String mode;
		Id line;
		Id route;
		double distance;
		double duration;
		double startTime;
		double endTime;

		public String toString(){
			return String.format("%3d %5d %6d %6d %6d %6.1f %6.1f %6.1f %6.1f %12s %12s %9s %11s %12s %6.1f %6.1f %7.1f %5.1f\n"
					, planElementID,activityID,journeyID,
					tripID,transferID, orig.getX(),orig.getY(),dest.getX(),dest.getY(),origFacility,destFacility,
					mode,line!=null?line.toString():"null",route!=null?route.toString():"null",distance,duration, startTime,endTime);
		}
	}

	private class TravellerChain extends PlanElement {

		Coord lastCoord;
		ArrayList<PlanElement> planElements = new ArrayList<EventsToPlanElements.PlanElement>();
		double lastTime = 0;
		boolean inPT = false;
		boolean walk = false;
		boolean traveledVehicle = false;
		public void addCurrentStateToPlanElements() {
			PlanElement p = new PlanElement();
			planElements.add(p);
			p.activityID = activityID;
			p.dest = dest;
			p.destFacility = destFacility;
			p.distance = distance;
			p.duration = duration;
			p.endTime = endTime;
			p.journeyID = journeyID;
			p.line = line;
			p.mode = mode;
			p.orig = orig;
			p.origFacility = origFacility;
			p.planElementID = planElementID;
			p.planElementType = planElementType;
			p.route = route;
			p.startTime = startTime;
			p.transferID = transferID;
			p.tripID = tripID;
		}
		public String toString(){
			return String.format("--------------------------\n" +
					"pID actID jrnyID tripID xferID              orig             dest  origFacility destFacility      mode        line        route distance duration startTime endTime\n" +
					"%3d %5d %6d %6d %6d %6.1f %6.1f %6.1f %6.1f %12s %12s %9s %11s %12s %6.1f %6.1f %7.1f %5.1f\n" +
					"--------------------------", planElementID,activityID,journeyID,
					tripID,transferID, orig.getX(),orig.getY(),dest.getX(),dest.getY(),origFacility,destFacility,
					mode,line!=null?line.toString():"null",route!=null?route.toString():"null",distance,duration, startTime,endTime);
		}
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
		if (!event.getPersonId().toString().startsWith("pt_tr"))
			locations.put(event.getPersonId(),
					network.getLinks().get(event.getLinkId()).getCoord());
		if (!event.getPersonId().toString().startsWith("pt_tr")
				&& chain == null) {
			chain = new TravellerChain();
			chains.put(event.getPersonId(), chain);
			chain.activityID++;
			chain.dest = network.getLinks().get(event.getLinkId()).getCoord();
			chain.destFacility = event.getFacilityId();
			chain.distance = 0.0;
			chain.duration = event.getTime();
			chain.endTime = event.getTime();
			chain.journeyID = -1;
			chain.line = null;
			chain.mode = null;
			chain.orig = chain.dest;
			chain.origFacility = chain.destFacility;
			chain.planElementID++;
			chain.planElementType = event.getActType();
			chain.route = null;
			chain.startTime = 0.0;
			chain.transferID = -1;
			chain.tripID = -1;
			chain.lastCoord = chain.dest;
			chain.lastTime = event.getTime();
			chain.dest = chain.orig;
			chain.addCurrentStateToPlanElements();			
		}else if (!event.getPersonId().toString().startsWith("pt_tr")
				&& !event.getActType()
						.equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			chain.duration = event.getTime() - chain.startTime;
			chain.endTime = event.getTime();
			chain.lastTime = event.getTime();
			chain.addCurrentStateToPlanElements();
		}
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			

		}
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
		boolean beforeInPT = chain.inPT;
		chain.dest = network.getLinks().get(event.getLinkId()).getCoord();
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			chain.inPT = true;

		} else {
			chain.inPT = false;
			chain.activityID++;
			chain.destFacility = event.getFacilityId();
			chain.distance = 0.0;
			chain.duration = -1;
			chain.endTime = -1;
			chain.lastCoord = chain.dest;
			chain.line = null;
			chain.mode = null;
			chain.orig = chain.dest;
			chain.origFacility = chain.destFacility;
			chain.planElementType = event.getActType();
			chain.route = null;
			chain.startTime = event.getTime();
			chain.planElementID++;
		}
		chain.lastTime = event.getTime();
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!event.getPersonId().toString().startsWith("pt_tr"))
			if (event.getVehicleId().toString().startsWith("tr")) {
				TravellerChain chain = chains.get(event.getPersonId());
				ptVehicles.get(event.getVehicleId()).addPassenger(
						event.getPersonId());
				chain.line = ptVehicles.get(event.getVehicleId()).transitLineId;
				chain.route = ptVehicles.get(event.getVehicleId()).transitRouteId;
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
				chain.distance = stageDistance;
			}
		}
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getVehicleId().toString().startsWith("tr"))
			ptVehicles.get(event.getVehicleId()).in = true;
		else
			chains.get(event.getPersonId()).inPT = true;
		
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
		}
	}
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());

		if (event.getLegMode().equals("transit_walk"))
			chain.walk = true;
		else
			chain.walk = false;
		if(!chain.inPT){
			//start of a chain
			chain.journeyID++;
			chain.tripID = 0;
		}else{
			chain.tripID++;
		}
		chain.dest=null;
		chain.destFacility=null;
		chain.distance=0.0;
		chain.duration = 0.0;
		chain.endTime = -1;
		chain.mode = event.getLegMode();
		chain.lastTime = event.getTime();
	}
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getPersonId().toString().startsWith("pt_tr"))
			return;
		TravellerChain chain = chains.get(event.getPersonId());
		if(!chain.inPT){
			//car trip ends
			chain.dest = network.getLinks().get(event.getLinkId()).getCoord();
			chain.endTime = event.getTime();
			chain.addCurrentStateToPlanElements();
		}else{
			chain.tripID++;
			chain.addCurrentStateToPlanElements();
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
		EventsToPlanElements timeDistribution = new EventsToPlanElements(
				scenario.getTransitSchedule(), scenario.getNetwork());
		eventsManager.addHandler(timeDistribution);
		new MatsimEventsReader(eventsManager).readFile(args[2]);
		System.out.println(timeDistribution.chains.get(new org.matsim.core.basic.v01.IdImpl("4101962")));
	}

}
