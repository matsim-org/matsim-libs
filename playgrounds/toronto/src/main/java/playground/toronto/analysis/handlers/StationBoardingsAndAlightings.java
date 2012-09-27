package playground.toronto.analysis.handlers;

import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Counts all agents boarding to or alighting from all transit facilities during a specified time period.
 * 
 * @author pkucirek
 *
 */
public class StationBoardingsAndAlightings implements
		VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {

	private final double startTime;
	private final double endTime;
	
	private HashMap<Id, Id> vehicleStopCache;
	private HashMap<Id, Integer> facilityBoardings;
	private HashMap<Id, Integer> facilityAlightings;
	private HashSet<Id> facilities;
	
	public StationBoardingsAndAlightings(){
		this.startTime = 0;
		this.endTime = Double.MAX_VALUE;
		init();
	}
	
	public StationBoardingsAndAlightings(double start, double end){
		this.startTime = start;
		this.endTime = end;
		init();
	}
	
	private void init(){
		this.facilities = new HashSet<Id>();
		this.facilityAlightings = new HashMap<Id, Integer>();
		this.facilityBoardings = new HashMap<Id, Integer>();
		this.vehicleStopCache = new HashMap<Id, Id>();
	}
	
	public HashSet<Id> getActiveStops(){
		return this.facilities;
	}
	
	public Integer getAlightingsAtStop(Id stopId){
		return this.facilityAlightings.get(stopId);
	}
	
	public Integer getBoardingsAtStop(Id stopId){
		return this.facilityBoardings.get(stopId);
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleStopCache.clear();
		this.facilities.clear();
		this.facilityAlightings.clear();
		this.facilityBoardings.clear();
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicleStopCache.containsKey(event.getVehicleId())
				&& event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			//Skips persons exiting vehicles not at stops and not in the time period.
			
			Id facilityId = this.vehicleStopCache.get(event.getVehicleId());
			Integer i;
			if (!this.facilityBoardings.containsKey(facilityId)){
				i = new Integer(1);
			}else{
				i = this.facilityBoardings.get(facilityId) + 1;
			}
			this.facilityBoardings.put(facilityId, i);
			
			this.facilities.add(facilityId);
		}

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleStopCache.containsKey(event.getVehicleId())
				&& event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			//Skips persons entering vehicles not at stops and not in the time period.
			
			Id facilityId = this.vehicleStopCache.get(event.getVehicleId());
			Integer i;
			if (!this.facilityAlightings.containsKey(facilityId)){
				i = new Integer(1);
			}else{
				i = this.facilityAlightings.get(facilityId) + 1;
			}
			this.facilityAlightings.put(facilityId, i);
			
			this.facilities.add(facilityId);
		}

	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleStopCache.remove(event.getVehicleId());

	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleStopCache.put(event.getVehicleId(), event.getFacilityId());
	}

}
