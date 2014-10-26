package playground.andreas.bln.ana.events2counts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

public class CreateVehID2LineMap {
	
	private final static Logger log = Logger.getLogger(CreateVehID2LineMap.class);
	
	public static Map<Id<Vehicle>, Id<TransitLine>> createVehID2LineMap(TransitSchedule transitSchedule){
		
		Map<Id<Vehicle>, Id<TransitLine>> veh2LineMap = new HashMap<>();
		
		for (Entry<Id<TransitLine>, TransitLine> lineEntry : transitSchedule.getTransitLines().entrySet()) {
			for (Entry<Id<TransitRoute>, TransitRoute> routeEntry : lineEntry.getValue().getRoutes().entrySet()) {
				for (Entry<Id<Departure>, Departure> departureEntry : routeEntry.getValue().getDepartures().entrySet()){
					
					// check if vehicle is already in list and if this is the case check its line
					
					if(veh2LineMap.get(departureEntry.getValue().getVehicleId()) != null){
						// check its line
						if(!veh2LineMap.get(departureEntry.getValue().getVehicleId()).toString().equalsIgnoreCase(lineEntry.getKey().toString())){
							log.warn("Veh " + departureEntry.getValue().getVehicleId() + " serves to different lines (" + lineEntry.getKey().toString() + ", " + veh2LineMap.get(departureEntry.getValue().getVehicleId()) + "). Don't know what to do");
						}
						
					} else {
						veh2LineMap.put(departureEntry.getValue().getVehicleId(), lineEntry.getKey());
					}
				}
			}
		}
		
		return veh2LineMap;
	}

}
