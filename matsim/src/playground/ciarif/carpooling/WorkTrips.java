package playground.ciarif.carpooling;

import java.util.ArrayList;

public class WorkTrips {
	
	private ArrayList<WorkTrip> workTrips = new ArrayList<WorkTrip>();
	
	public final boolean addTrip(final WorkTrip workTrip) {
		if (workTrip == null) { return false; }
		//if (this.retailers.containsKey(retailer.getId())) { return false; }
		//this.retailers.put(retailer.getId(),retailer);
		this.workTrips.add(workTrip);
		return true;
	}
	
	public ArrayList<WorkTrip> getWorkTrips() {
		return this.workTrips;
	}
}
