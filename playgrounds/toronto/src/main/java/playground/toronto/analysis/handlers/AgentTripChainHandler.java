package playground.toronto.analysis.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.utils.collections.Tuple;

import playground.toronto.analysis.ODMatrix;
import playground.toronto.analysis.tripchains.AutoDriveComponent;
import playground.toronto.analysis.tripchains.InTransitVehicleComponent;
import playground.toronto.analysis.tripchains.Trip;
import playground.toronto.analysis.tripchains.TripComponent;
import playground.toronto.analysis.tripchains.WaitForTransitComponent;
import playground.toronto.analysis.tripchains.WalkComponent;
import playground.toronto.mapping.Link2ZoneMap;

/**
 * A handler for handling trips (legs) as lists of TripComponents. I wrote this because 
 * I want to be sure that the transit trip-time components I am analyzing are correct.
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
public class AgentTripChainHandler implements TransitDriverStartsEventHandler, ActivityStartEventHandler, ActivityEndEventHandler,
	AgentDepartureEventHandler, PersonEntersVehicleEventHandler, AgentArrivalEventHandler{

	private static final Logger log = Logger.getLogger(AgentTripChainHandler.class);
	
	//Main components
	private HashMap<Id, List<Trip>> personTripChainMap;
	private Link2ZoneMap linkToZoneMap;
	private HashSet<Id> driverIds;
	
	//Internal sets for assembling trip chains
	private HashMap<Id, Trip> currentTrips;
	private HashMap<Id, TripComponent> currentComponents;
	private HashMap<Id, Tuple<Id,Id>> vehicleIdLineIdMap;
	
	////////////////////////////////////////////////////////////////////////////////
	public AgentTripChainHandler(){
		this.personTripChainMap = new HashMap<Id, List<Trip>>();
		this.currentComponents = new HashMap<Id, TripComponent>();
		this.currentTrips = new HashMap<Id, Trip>();
		this.linkToZoneMap = null;	
		this.driverIds = new HashSet<Id>();
		this.vehicleIdLineIdMap = new HashMap<Id, Tuple<Id,Id>>();
	}
	
	public void setLinkZoneMap(Link2ZoneMap map){
		this.linkToZoneMap = map;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////
	private void addTrip(Id pid, Trip trip){
		if (!this.personTripChainMap.containsKey(pid)) 
			this.personTripChainMap.put(pid, new ArrayList<Trip>());
		this.personTripChainMap.get(pid).add(trip);
	}
	
	private void addTripComponent(Id pid, TripComponent tcc){
		if (this.driverIds.contains(pid)) return;
		Trip trip = this.currentTrips.get(pid);
		if (trip == null){
			log.error("Could not find valid trip for agent " + pid + "!");
			return;
		}
		trip.addComponent(tcc);
	}

	///////////////////////////////////////////////////////////////////////////////
	
	public Trip getTrip(Id pid, int t){
		return this.personTripChainMap.get(pid).get(t);
	}
	
	public int getTripSize(Id pid){
		List<Trip> q = this.personTripChainMap.get(pid);
		if (q == null) return 0;
		return q.size();
	}
	
	public ODMatrix getAvgTransitWalkTimeODM(){
		return this.getComponentAverageTime(WalkComponent.class, 0, Double.MAX_VALUE, false);
	}
	public ODMatrix getAvgTransitWalkTimeODM(double startTime, double endTime, boolean boundedByTripEndTimes){
		return this.getComponentAverageTime(WalkComponent.class, startTime, endTime, boundedByTripEndTimes);
	}

	public ODMatrix getAvgTransitWaitTimeODM(){
		return this.getComponentAverageTime(WaitForTransitComponent.class, 0, Double.MAX_VALUE, false);
	}
	public ODMatrix getAvgTransitWaitTimeODM(double startTime, double endTime, boolean boundedByTripEndTimes){
		return this.getComponentAverageTime(WaitForTransitComponent.class, startTime, endTime, boundedByTripEndTimes);
	}
	
	public ODMatrix getAvgTransitInVehicleTimeODM(){
		return this.getComponentAverageTime(InTransitVehicleComponent.class, 0, Double.MAX_VALUE, false);
	}
	public ODMatrix getAvgTransitInVehicleTimeODM(double startTime, double endTime, boolean boundedByTripEndTimes){
		return this.getComponentAverageTime(InTransitVehicleComponent.class, startTime, endTime, boundedByTripEndTimes);
	}
	
	public ODMatrix getAvgAutoDriveTimeODM(){
		return this.getComponentAverageTime(AutoDriveComponent.class,0, Double.MAX_VALUE, false);
	}
	public ODMatrix getAvgAutoDriveTimeODM(double startTime, double endTime, boolean boundedByTripEndTimes){
		return this.getComponentAverageTime(AutoDriveComponent.class, startTime, endTime, boundedByTripEndTimes);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(int iteration) {
		this.currentComponents = new HashMap<Id, TripComponent>();
		this.currentTrips = new HashMap<Id, Trip>();
		this.currentComponents = new HashMap<Id, TripComponent>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id pid = event.getPersonId();
		TripComponent tcc = this.currentComponents.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}
		tcc.finishComponent(event.getTime());
		this.addTripComponent(pid, tcc);
		this.currentComponents.remove(pid);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!this.vehicleIdLineIdMap.containsKey(event.getVehicleId()))
			return; //Skips vehicles which are not transit vehicles
		if (this.driverIds.contains(event.getPersonId()))
			return; //skips transit drivers entering vehicles

		Id pid = event.getPersonId();
		TripComponent tcc = this.currentComponents.get(pid);
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
			Tuple<Id, Id> tup = this.vehicleIdLineIdMap.get(event.getVehicleId());
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
		if (!event.getActType().equals(LegModes.interactionAct)){ //Skip events marked 'pt-interaction'

			Id pid = event.getPersonId();
			Trip trip = new Trip(pid);
			
			if (this.linkToZoneMap != null){
				trip.zone_o = this.linkToZoneMap.getZoneOfLink(event.getLinkId());
			}
			this.currentTrips.put(pid, trip);
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equals(LegModes.interactionAct)){
			Id pid = event.getPersonId();
			Trip trip = this.currentTrips.get(pid);
			if (this.linkToZoneMap != null){
				trip.zone_d = this.linkToZoneMap.getZoneOfLink(event.getLinkId());
			}
			this.addTrip(pid, trip);
			this.currentTrips.remove(pid);
		}
	}
	///////////////////////////////////////////////////////////////////////////////
	
	private ODMatrix getComponentAverageTime(Class c, final double startTime, final double endTime, 
			final boolean boundedByTripEnd){
		if (this.linkToZoneMap == null) 
			throw new NullPointerException("Cannot get OD matrices if no Link2ZoneMap exists!");
		
		ODMatrix sumOfComponentTimes = new ODMatrix(this.linkToZoneMap.getSetOfZone());
		ODMatrix countOfFrequencies = new ODMatrix(this.linkToZoneMap.getSetOfZone());
		
		for (List<Trip> chain : this.personTripChainMap.values()){
			for (Trip trip : chain){
				//Skips trips outside the time period.
				if (trip.getStartTime() < startTime || trip.getStartTime() > endTime) continue;
				if ((trip.getEndTime() < startTime || trip.getEndTime() > endTime) && boundedByTripEnd) continue;
				
				String origin = trip.zone_o.toString();
				String destination = trip.zone_d.toString();
				double t = trip.getComponentTime(c);
				
				sumOfComponentTimes.incrementODPair(origin, destination, t);
				countOfFrequencies.incrementODPair(origin, destination);
			}
		}
		
		return sumOfComponentTimes.divideBy(countOfFrequencies);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	private static final class LegModes{
		public static final String transitWalkMode = "transit_walk";
		public static final String inVehicleMode = "pt";
		public static final String carMode = "car";
		public static final String interactionAct = "pt interaction";
	}


	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.driverIds.add(event.getDriverId());
		this.vehicleIdLineIdMap.put(event.getVehicleId(), new Tuple<Id, Id>(event.getTransitLineId(), event.getTransitRouteId()));
		
	}
	
}
