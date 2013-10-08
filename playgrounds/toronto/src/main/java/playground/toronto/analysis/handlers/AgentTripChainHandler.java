package playground.toronto.analysis.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

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
	PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler{

	private static final Logger log = Logger.getLogger(AgentTripChainHandler.class);
	
	//Main components
	private HashMap<Id, List<Trip>> personTripChainMap;
	private Link2ZoneMap linkToZoneMap;
	private HashSet<Id> driverIds;
	
	//Internal sets for assembling trip chains
	private HashMap<Id, Trip> tripsBuffer;
	private HashMap<Id, TripComponent> componentsBuffer;
	private HashMap<Id, Tuple<Id,Id>> vehicleIdLineIdMap;
	
	double finalTime = 0;
	
	////////////////////////////////////////////////////////////////////////////////
	public AgentTripChainHandler(){
		this.personTripChainMap = new HashMap<Id, List<Trip>>();
		this.componentsBuffer = new HashMap<Id, TripComponent>();
		this.tripsBuffer = new HashMap<Id, Trip>();
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
		Trip trip = this.tripsBuffer.get(pid);
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
	
	public int getTripsInTimePeriod(double start, double end){
		int result = 0;
		
		for (List<Trip> tour : this.personTripChainMap.values()){
			for (Trip trip : tour){
				if (trip.getStartTime() >= start && trip.getStartTime() <= end) result++;
			}
		}
		
		return result;
	}
	public int getTransitTripsInTimePeriod(double start, double end){
		int result = 0;
		
		for (List<Trip> tour : this.personTripChainMap.values()){
			for (Trip trip : tour){
				if (trip.getAutoDriveTime() >0) continue;
				if (trip.getStartTime() >= start && trip.getStartTime() <= end) result++;
			}
		}
		
		return result;
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
	
	public HashMap<String, Integer> getStuckTripStats(){
		HashMap<String, Integer> stats = new HashMap<String, Integer>();
		
		int stuckTrips = this.tripsBuffer.size();
		int stuckWalk = 0;
		int stuckWait = 0;
		int stuckInVeh = 0;
		int stuckDrive = 0;
		
		for (TripComponent tc : this.componentsBuffer.values()){
			if (tc instanceof WalkComponent) stuckWalk++;
			else if (tc instanceof WaitForTransitComponent) stuckWait++;
			else if (tc instanceof InTransitVehicleComponent) stuckInVeh++;
			else if (tc instanceof AutoDriveComponent) stuckDrive++;
		}
		
		stats.put("all", stuckTrips);
		stats.put("walk", stuckWalk);
		stats.put("wait", stuckWait);
		stats.put("veh", stuckInVeh);
		stats.put("drive", stuckDrive);
		
		return stats;
	}
	
	public int allWalkTrips(){
		int c = 0;
		for (List<Trip> tour : this.personTripChainMap.values()){
			for (Trip t : tour){
				if (t.getAutoDriveTime() > 0)  continue;
				if (t.getIVTT() == 0.0) c++;
			}
		}
		return c;
	}
	
	public int nonWalkTrips(){
		int c = 0;
		for (List<Trip> tour : this.personTripChainMap.values()){
			for (Trip t : tour){
				if (t.getAutoDriveTime() > 0)  continue;
				if (t.getWalkTime() == 0.0) c++;
			}
		}
		return c;
	}
	
	public HashMap<String, Double> getAvgComponentsByTime(double start, double end){
		 HashMap<String, Double> stats = new HashMap<String, Double>();
		
		int totalTrips = 0;
		double walkSum = 0.0;
		double waitSum = 0.0;
		double rideSum = 0.0;
		
		for (List<Trip> tour : this.personTripChainMap.values()){
			for (Trip t : tour){
				if (t.getAutoDriveTime() > 0) continue; //skip auto trips
				if (t.getStartTime() < start || t.getStartTime() > end) continue;
				totalTrips++;
				walkSum += t.getWalkTime();
				waitSum += t.getWaitTime();
				rideSum += t.getIVTT();
			}
		}
		
		stats.put("total", (double) totalTrips);
		stats.put("walk.avg", walkSum / totalTrips);
		stats.put("wait.avg", waitSum / totalTrips);
		stats.put("ivtt.avg", rideSum / totalTrips);
		
		return stats;
	}
	
	public void exportTripsTable(String fileName) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		bw.write("pid,trip.number,start.time,end.time,ivtt.time,wait.time,walk.time");
		
		for (Entry<Id, List<Trip>> tour : this.personTripChainMap.entrySet()){
			int i = 0;
			for (Trip trip : tour.getValue()){
				if (trip.getAutoDriveTime() > 0 ) continue; //skip auto trips
				
				bw.newLine();
				bw.write(tour.getKey().toString() + "," + i++ + "," + Time.writeTime(trip.getStartTime()) + "," +
						Time.writeTime(trip.getEndTime()) + "," + trip.getIVTT() + ","
						+ trip.getWaitTime() + "," + trip.getWalkTime());
			}
		}
		bw.close();
		log.info("Trips exported to " + fileName);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(int iteration) {
		this.componentsBuffer.clear();
		this.tripsBuffer.clear();
		this.componentsBuffer.clear();
		this.driverIds.clear();
		this.vehicleIdLineIdMap.clear();
		this.finalTime = 0;
		this.personTripChainMap.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id pid = event.getPersonId();
		TripComponent tcc = this.componentsBuffer.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}
		tcc.finishComponent(event.getTime());
		this.addTripComponent(pid, tcc);
		this.componentsBuffer.remove(pid);
		
		if (event.getTime() > this.finalTime) finalTime = event.getTime();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String mode = event.getLegMode();
		Id pid = event.getPersonId();

		if (mode.equals(LegModes.transitWalkMode)){
			WalkComponent tcc = new WalkComponent(event.getTime());
			componentsBuffer.put(pid, tcc);
		} else if (mode.equals(LegModes.inVehicleMode)){
			//the in-vehicle component begins with a wait time.
			WaitForTransitComponent tcc = new WaitForTransitComponent(event.getTime());
			componentsBuffer.put(pid, tcc);
		}else if (mode.equals(LegModes.carMode)){
			AutoDriveComponent tcc = new AutoDriveComponent(event.getTime());
			componentsBuffer.put(pid, tcc);
		}else{
			log.error("Leg mode '" + mode + "' is not recognized/supported!");
		}
		
		if (event.getTime() > this.finalTime) finalTime = event.getTime();
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!event.getActType().equals(LegModes.interactionAct)){ //Skip events marked 'pt-interaction'

			Id pid = event.getPersonId();
			Trip trip = new Trip(pid);
			
			if (this.linkToZoneMap != null){
				trip.zone_o = this.linkToZoneMap.getZoneOfLink(event.getLinkId());
			}
			this.tripsBuffer.put(pid, trip);
			
			if (event.getTime() > this.finalTime) finalTime = event.getTime();
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equals(LegModes.interactionAct)){
			Id pid = event.getPersonId();
			Trip trip = this.tripsBuffer.get(pid);
			if (this.linkToZoneMap != null){
				trip.zone_d = this.linkToZoneMap.getZoneOfLink(event.getLinkId());
			}
			this.addTrip(pid, trip);
			this.tripsBuffer.remove(pid);
			
			if (event.getTime() > this.finalTime) finalTime = event.getTime();
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
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!this.vehicleIdLineIdMap.containsKey(event.getVehicleId()))
			return; //Skips vehicles which are not transit vehicles
		if (this.driverIds.contains(event.getPersonId()))
			return; //skips transit drivers entering vehicles

		Id pid = event.getPersonId();
		TripComponent tcc = this.componentsBuffer.get(pid);
		if (tcc == null){
			log.error("Could not find valid trip chain component for agent " + pid + "!");
			return;
		}else if (tcc instanceof WaitForTransitComponent){
			//End wait episode
			WaitForTransitComponent wftc = (WaitForTransitComponent) tcc;
			wftc.finishComponent(event.getTime());
			this.addTripComponent(pid, wftc);
			componentsBuffer.remove(pid); 
			
			//Start an in-vehicle episode
			InTransitVehicleComponent itvc = new InTransitVehicleComponent(event.getTime());
			Tuple<Id, Id> tup = this.vehicleIdLineIdMap.get(event.getVehicleId());
			if (tup != null) itvc.setRouteInfo(tup.getFirst(), tup.getSecond());
			componentsBuffer.put(pid, itvc);
		}
		
		if (event.getTime() > this.finalTime) finalTime = event.getTime();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.driverIds.add(event.getDriverId());
		this.vehicleIdLineIdMap.put(event.getVehicleId(), new Tuple<Id, Id>(event.getTransitLineId(), event.getTransitRouteId()));
		
		if (event.getTime() > this.finalTime) finalTime = event.getTime();
	}
	
}
