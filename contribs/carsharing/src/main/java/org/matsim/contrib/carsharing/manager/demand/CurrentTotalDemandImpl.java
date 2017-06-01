package org.matsim.contrib.carsharing.manager.demand;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * @author balac
 */
public class CurrentTotalDemandImpl implements CurrentTotalDemand {
	
	private Map<Id<Person>, CarsharingCurrentRentalsInfo> currentDemand = new HashMap<Id<Person>, CarsharingCurrentRentalsInfo>();
	private Network network;
	
	public CurrentTotalDemandImpl(Network network) {
		
		this.network = network;
	}
	
	@Override
	public boolean hasVehicleOnLink(Id<Person> personId, Link link, String type) {
		if (!currentDemand.containsKey(personId))
			return false;
		else
			return currentDemand.get(personId).hasVehicleOnLink(link, type);
	}
	
	@Override
	public CSVehicle getVehicleOnLink(Id<Person> personId, Link link, String type) {
		
		if (!currentDemand.containsKey(personId))
			return null;
		else
			return currentDemand.get(personId).getVehicleOnLink(link, type);
	}

	
	@Override
	public boolean addVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {
		if (!currentDemand.containsKey(personId)) {
			
			CarsharingCurrentRentalsInfo carsharingCurrentRentalsInfo = new CarsharingCurrentRentalsInfo(network);
			carsharingCurrentRentalsInfo.addType(type);
			currentDemand.put(personId, carsharingCurrentRentalsInfo);
			return carsharingCurrentRentalsInfo.addVehicle(link, vehicle, type);
			
			
		}
		else {
			
			if (currentDemand.get(personId).getCurrentRentals().containsKey(type))
			
				return currentDemand.get(personId).addVehicle(link, vehicle, type);
			else {
				currentDemand.get(personId).addType(type);

				return currentDemand.get(personId).addVehicle(link, vehicle, type);

			}

		}
	}
	
	@Override
	public boolean removeVehicle(Id<Person> personId, Link link, CSVehicle vehicle, String type) {
		CarsharingCurrentRentalsInfo personRentals = this.currentDemand.get(personId);

		if (personRentals != null) {
			return personRentals.removeVehicle(link, vehicle, type);
		}

		return false;
	}

	@Override
	public void reset() {
		currentDemand = new HashMap<Id<Person>, CarsharingCurrentRentalsInfo>();
		
	}
}
