package vwExamples.peoplemoverVWExample.CustomRebalancing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;



public class ZonalIdleVehicleCollectorMy implements ActivityStartEventHandler, ActivityEndEventHandler {

	private Map<String,LinkedList<Id<Vehicle>>> vehiclesPerZone = new HashMap<>();
	private Map<Id<Vehicle>,String> zonePerVehicle = new HashMap<>();
	private final DrtZonalSystem zonalSystem; 
	
	public Map<Id<Vehicle>,Integer> vehicleIdleMap = new HashMap<>();
	

	@Inject
	public ZonalIdleVehicleCollectorMy(EventsManager events, DrtZonalSystem zonalSystem) {
		events.addHandler(this);
		this.zonalSystem = zonalSystem;
		for (String z : zonalSystem.getZones().keySet()){
			vehiclesPerZone.put(z,new LinkedList<Id<Vehicle>>());
		}
	}
	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone!=null){
				Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
				vehiclesPerZone.get(zone).add(vid);
				zonePerVehicle.put(vid, zone);
			}
			
			Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
			vehicleIdleMap.put(vid, (int) event.getTime());
		}
		
		if (event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)){
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone!=null){
				Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
				zonePerVehicle.remove(vid);
				vehiclesPerZone.get(zone).remove(vid);
			}
			
			Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
			vehicleIdleMap.remove(vid);
		}
		
	}



	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
			String zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone!=null){
				
				Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
				zonePerVehicle.remove(vid);
				vehiclesPerZone.get(zone).remove(vid);

			}
		
		
			Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
			vehicleIdleMap.remove(vid);
		}	
	}
	
	public LinkedList<Id<Vehicle>> getIdleVehiclesPerZone(String zone){
		return this.vehiclesPerZone.get(zone);
	}
	
	@Override
	public void reset(int iteration){
	vehicleIdleMap.clear();
	}
	

}



