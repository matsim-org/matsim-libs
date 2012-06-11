package playground.mkillat.pt_test;

import org.matsim.api.core.v01.Id;

public class StopInformation {

	Id id;
	double arrival;
	double departure;
	
	public StopInformation (Id id, double ariival, double departure){
		this.id=id;
		this.arrival=ariival;
		this.departure=departure;
	}

	@Override
	public String toString() {
		return "StopInformation [id=" + id + ", arrival=" + arrival
				+ ", departure=" + departure + "]";
	}
	
	
	
}
