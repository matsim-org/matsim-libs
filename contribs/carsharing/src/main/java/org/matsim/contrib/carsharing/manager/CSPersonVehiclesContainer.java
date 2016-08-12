package org.matsim.contrib.carsharing.manager;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * 
 * @author balac
 */
public class CSPersonVehiclesContainer implements CSPersonVehicle{
	
	
	private Map<Id<Person>, VehicleInfo> vehicleInfoPerPerson = new HashMap<Id<Person>, VehicleInfo>();
	

	@Override
	public void addNewPersonInfo(Id<Person> personId) {

		this.vehicleInfoPerPerson.put(personId, new VehicleInfo());
	}
	
	public Map<Id<Link>, CSVehicle> getVehicleLocationForType(Id<Person> personId, String type) {
		
		if (type.equals("freefloating")) {
			if (vehicleInfoPerPerson.get(personId) != null)
				return vehicleInfoPerPerson.get(personId).getFfvehicleIdLocation();
		}
			
		return null;
	}

	

	public boolean hasVehicleOnLink(Link link, Id<Person> personId) {
		
		if (vehicleInfoPerPerson.get(personId).getFfvehicleIdLocation().containsKey(link.getId()))
			return true;
		else
			return false;
	}
	
	public CSVehicle getVehicleOnLink(Link link, Id<Person> personId) {
		
		return vehicleInfoPerPerson.get(personId).getFfvehicleIdLocation().get(link.getId());
	}

	@Override
	public boolean addVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {

		this.vehicleInfoPerPerson.get(personId).ffvehicleIdLocation.put(link.getId(), vehicle);
		
		return true;
	}
	@Override
	public boolean removeVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {
		if (type=="freefloating")
			this.vehicleInfoPerPerson.get(personId).ffvehicleIdLocation.remove(link.getId());
		
		return true;
	}
	
	private class VehicleInfo {
		private Map<Id<Link>, CSVehicle> ffvehicleIdLocation = new HashMap<Id<Link>, CSVehicle>();
		private Map<Id<Link>, CSVehicle> owvehicleIdLocation = new HashMap<Id<Link>, CSVehicle>();

		public Map<Id<Link>, CSVehicle> getFfvehicleIdLocation() {
			return ffvehicleIdLocation;
		}
		
	}

	@Override
	public void reset() {
		vehicleInfoPerPerson = new HashMap<Id<Person>, VehicleInfo>();
		
	}


}
