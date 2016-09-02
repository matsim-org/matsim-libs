package org.matsim.contrib.carsharing.manager.demand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
/** 
 * @author balac
 */
public class AgentRentals{
	private Map<String, RentalInfo> statsPerVehicle = new HashMap<String, RentalInfo>();

	private ArrayList<RentalInfo> arr = new ArrayList<RentalInfo>();	
	private Id<Person> personId;
	public AgentRentals(Id<Person> personId) {

		this.personId = personId;
	
	}
	public Map<String, RentalInfo> getStatsPerVehicle() {
		return statsPerVehicle;
	}
	public void setStatsPerVehicle(Map<String, RentalInfo> statsPerVehicle) {
		this.statsPerVehicle = statsPerVehicle;
	}
	public ArrayList<RentalInfo> getArr() {
		return arr;
	}
	public void setArr(ArrayList<RentalInfo> arr) {
		this.arr = arr;
	}
	
}
