package org.matsim.contrib.carsharing.manager;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.FFVehicle;
/** 
 * 
 * @author balac
 */
public class CSPersonVehiclesContainer {
	
	private Map<Id<Link>, FFVehicle> ffvehicleIdLocation = new HashMap<Id<Link>, FFVehicle>();
	
	public Map<Id<Link>, FFVehicle> getFfvehicleIdLocation() {
		return ffvehicleIdLocation;
	}

	public void setFfvehicleIdLocation(Map<Id<Link>, FFVehicle> ffvehicleIdLocation) {
		this.ffvehicleIdLocation = ffvehicleIdLocation;
	}

	public boolean hasVehicleOnLink(Link link) {
		
		if (ffvehicleIdLocation.containsKey(link.getId()))
			return true;
		else
			return false;
	}
	
	public FFVehicle getVehicleOnLink(Link link) {
		
		return ffvehicleIdLocation.get(link.getId());
	}


}
