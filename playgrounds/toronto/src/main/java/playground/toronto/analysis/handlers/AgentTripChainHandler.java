package playground.toronto.analysis.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.toronto.analysis.tripchains.ActivityComponent;
import playground.toronto.analysis.tripchains.AutoDriveComponent;
import playground.toronto.analysis.tripchains.InTransitVehicleComponent;
import playground.toronto.analysis.tripchains.TripChainComponent;
import playground.toronto.analysis.tripchains.WaitForTransitComponent;
import playground.toronto.analysis.tripchains.WalkComponent;

/**
 * A handler for analyzing trip chain components. A trip chain is similar to a plan,
 * except we don't care about activities. I wrote this because I want to be sure that
 * the transit trip-time components I am analyzing are correct.
 * 
 * Generally, this handler facilitates post-run analysis of travel time components,
 * specifically for transit trips (although it recognizes and handles auto driver
 * trips as well)
 * 
 * TODO: Implement special handling for mixed-mode trips?
 * 
 * @author pkucirek
 *
 */
public class AgentTripChainHandler implements ActivityStartEventHandler, ActivityEndEventHandler,
	AgentDepartureEventHandler, PersonEntersVehicleEventHandler, AgentArrivalEventHandler{

	private static final Logger log = Logger.getLogger(AgentTripChainHandler.class);
	
	//Main components
	private HashMap<Id, List<TripChainComponent>> personTripChainMap;
	private TransitSchedule schedule;
	//private Vehicles vehicles;
	
	//Internal set for assembling trip chains
	private HashMap<Id, TripChainComponent> currentComponents;
	
	
	////////////////////////////////////////////////////////////////////////////////
	public AgentTripChainHandler(TransitSchedule tSchedule, Vehicles vehList){
		this.schedule = tSchedule;
		//this.vehicles = vehList;
		this.personTripChainMap = new HashMap<Id, List<TripChainComponent>>();
		this.currentComponents = new HashMap<Id, TripChainComponent>();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	private void addTripComponent(Id pid, TripChainComponent tcc){
		if (!this.personTripChainMap.containsKey(pid)){
			ArrayList<TripChainComponent> tripChain = new ArrayList<TripChainComponent>();
			tripChain.add(tcc);
			this.personTripChainMap.put(pid, tripChain);
		}else{
			this.personTripChainMap.get(pid).add(tcc);
		}
	}
	
	//A dumb search function. But I have no other way to map VehicleId to Line+Route!
	private Tuple<Id, Id> findTransitLineFromVehicle(Id vehId){
		Tuple<Id, Id> result = null;
		for (TransitLine line : this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (Departure dep : route.getDepartures().values()){
					if (dep.getVehicleId().equals(vehId)){
						result = new Tuple<Id, Id>(line.getId(), route.getId());
					}
				}
			}
		}
		return result;
	}
	
	public List<TripChainComponent> getTripChain(Id pid){
		return this.personTripChainMap.get(pid);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(int iteration) {
		this.personTripChainMap = new HashMap<Id, List<TripChainComponent>>();
		this.currentComponents = new HashMap<Id, TripChainComponent>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id pid = event.getPersonId();
		TripChainComponent tcc = this.currentComponents.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}
		tcc.finishComponent(event.getTime());
		this.addTripComponent(pid, tcc);
		this.currentComponents.remove(pid);
	}

	/*
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id pid = event.getPersonId();
		TripChainComponent tcc = this.currentComponents.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}else if(tcc instanceof InTransitVehicleComponent){
			InTransitVehicleComponent itvc = (InTransitVehicleComponent) tcc;
			itvc.finishComponent(event.getTime());
			this.addTripComponent(pid, itvc);
			this.currentComponents.remove(pid);
		}
	}*/

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id pid = event.getPersonId();
		TripChainComponent tcc = this.currentComponents.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}else if (tcc instanceof WaitForTransitComponent){
			//End wait episode
			WaitForTransitComponent wftc = (WaitForTransitComponent) tcc;
			wftc.finishComponent(event.getTime());
			this.addTripComponent(pid, wftc);
			currentComponents.remove(pid); 
			
			//Start an in-vehicle episode
			InTransitVehicleComponent itvc = new InTransitVehicleComponent(event.getTime());
			Tuple<Id, Id> tup = findTransitLineFromVehicle(event.getVehicleId());
			if (tup != null) itvc.setRouteInfo(tup.getFirst(), tup.getSecond());
			currentComponents.put(pid, itvc);
		}
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		String mode = event.getLegMode();
		Id pid = event.getPersonId();
		
		if (mode.equals(LegModes.transitWalkMode)){
			WalkComponent tcc = new WalkComponent(event.getTime());
			currentComponents.put(pid, tcc);
		} else if (mode.equals(LegModes.inVehicleMode)){
			//the in-vehicle component begins with a wait time.
			WaitForTransitComponent tcc = new WaitForTransitComponent(event.getTime());
			currentComponents.put(pid, tcc);
		}else if (mode.equals(LegModes.carMode)){
			AutoDriveComponent tcc = new AutoDriveComponent(event.getTime());
			currentComponents.put(pid, tcc);
		}else{
			log.error("Leg mode '" + mode + "' is not recognized/supported!");
		}
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id pid = event.getPersonId();
		String type = event.getActType();
		if (type.equals(LegModes.interactionAct)) return;
		
		TripChainComponent tcc = this.currentComponents.get(pid);
		if (tcc != null){
			ActivityComponent atcc = (ActivityComponent) tcc;
			atcc.finishComponent(event.getTime());
			this.addTripComponent(pid, atcc);
			this.currentComponents.remove(pid);
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id pid = event.getPersonId();
		String type = event.getActType();
		if (type.equals(LegModes.interactionAct)) return;
		ActivityComponent atcc = new ActivityComponent(event.getTime(), event.getActType());
		this.currentComponents.put(pid, atcc);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	private static final class LegModes{
		public static final String transitWalkMode = "transit_walk";
		public static final String inVehicleMode = "pt";
		public static final String carMode = "car";
		public static final String interactionAct = "pt interaction";
	}


	
	
}
