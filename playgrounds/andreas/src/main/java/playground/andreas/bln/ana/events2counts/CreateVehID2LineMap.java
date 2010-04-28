package playground.andreas.bln.ana.events2counts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;

public class CreateVehID2LineMap {
	
	public static Map<Id, Id> createVehID2LineMap(TransitSchedule transitSchedule){
		
		Map<Id, Id> veh2LineMap = new HashMap<Id, Id>();
		
		for (Entry<Id, TransitLine> lineEntry : transitSchedule.getTransitLines().entrySet()) {
			for (Entry<Id, TransitRoute> routeEntry : lineEntry.getValue().getRoutes().entrySet()) {
				for (Entry<Id, Departure> departureEntry : routeEntry.getValue().getDepartures().entrySet()){
					veh2LineMap.put(departureEntry.getValue().getVehicleId(), lineEntry.getKey());
				}
			}
		}
		
		return veh2LineMap;
	}

}
