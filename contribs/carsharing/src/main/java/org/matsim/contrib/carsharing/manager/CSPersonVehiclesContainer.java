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
		else if (type.equals("oneway")) {
			if (vehicleInfoPerPerson.get(personId) != null)
				return vehicleInfoPerPerson.get(personId).getOwvehicleIdLocation();
		}
		else if (type.equals("twoway")) {
			if (vehicleInfoPerPerson.get(personId) != null)
				return vehicleInfoPerPerson.get(personId).getTwvehicleIdLocation();
		}
		return null;
	}
	
	public Map<Id<Link>, CSVehicle> getOriginLinkForTW(Id<Person> personId) {
		return vehicleInfoPerPerson.get(personId).getTwvehicleOriginLink();
		
	}
	@Override
	public void addOriginForTW(Id<Person> personId, Link link, CSVehicle vehicle) {
		
		this.vehicleInfoPerPerson.get(personId).twvehicleOriginLink.put(link.getId(), vehicle);
		
	}

	public boolean hasVehicleOnLink(Link link, Id<Person> personId, String type) {
		if (type.equals("freefloating")) {
			if (vehicleInfoPerPerson.get(personId).getFfvehicleIdLocation().containsKey(link.getId()))
				return true;
		}
		else if (type.equals("oneway")) {
			if (vehicleInfoPerPerson.get(personId).getOwvehicleIdLocation().containsKey(link.getId()))
				return true;
		}
		else if (type.equals("twoway")) {
			if (vehicleInfoPerPerson.get(personId).getTwvehicleIdLocation().containsKey(link.getId()))
				return true;
		}
		return false;
	}
	
	public CSVehicle getVehicleOnLink(Link link, Id<Person> personId, String type) {
		if (type.equals("freefloating"))
			return vehicleInfoPerPerson.get(personId).getFfvehicleIdLocation().get(link.getId());
		else if (type.equals("oneway"))
			return vehicleInfoPerPerson.get(personId).getOwvehicleIdLocation().get(link.getId());
		else if (type.equals("twoway"))
			return vehicleInfoPerPerson.get(personId).getTwvehicleIdLocation().get(link.getId());
		else return null;
		
	}

	@Override
	public boolean addVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {

		if (type.equals("freefloating"))
			this.vehicleInfoPerPerson.get(personId).ffvehicleIdLocation.put(link.getId(), vehicle);
		else if (type.equals("oneway"))
			this.vehicleInfoPerPerson.get(personId).owvehicleIdLocation.put(link.getId(), vehicle);
		else if (type.equals("twoway"))
			this.vehicleInfoPerPerson.get(personId).twvehicleIdLocation.put(link.getId(), vehicle);
		
		return true;
	}
	@Override
	public boolean removeVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {
		if (type.equals("freefloating"))
			this.vehicleInfoPerPerson.get(personId).ffvehicleIdLocation.remove(link.getId());
		else if (type.equals("oneway"))
			this.vehicleInfoPerPerson.get(personId).owvehicleIdLocation.remove(link.getId());
		else if (type.equals("twoway"))
			this.vehicleInfoPerPerson.get(personId).twvehicleIdLocation.remove(link.getId());
		return true;
	}
	
	private class VehicleInfo {
		private Map<Id<Link>, CSVehicle> ffvehicleIdLocation = new HashMap<Id<Link>, CSVehicle>();
		private Map<Id<Link>, CSVehicle> owvehicleIdLocation = new HashMap<Id<Link>, CSVehicle>();
		private Map<Id<Link>, CSVehicle> twvehicleIdLocation = new HashMap<Id<Link>, CSVehicle>();
		private Map<Id<Link>, CSVehicle> twvehicleOriginLink = new HashMap<Id<Link>, CSVehicle>();
		public Map<Id<Link>, CSVehicle> getFfvehicleIdLocation() {
			return ffvehicleIdLocation;
		}
		
		public Map<Id<Link>, CSVehicle> getOwvehicleIdLocation() {
			return owvehicleIdLocation;
		}
		
		public Map<Id<Link>, CSVehicle> getTwvehicleIdLocation() {
			return twvehicleIdLocation;
		}

		public Map<Id<Link>, CSVehicle> getTwvehicleOriginLink() {
			return twvehicleOriginLink;
		}

		
		
	}

	@Override
	public void reset() {
		vehicleInfoPerPerson = new HashMap<Id<Person>, VehicleInfo>();
		
	}


}
