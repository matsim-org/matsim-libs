package playground.mkillat.pt_test;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class TransitScheduleTime {

	double departureTime;
	List <StopInformation> haltezeit;
	
	public TransitScheduleTime (double departureTime, List <StopInformation> haltzeit ){
		this.departureTime=departureTime;
		this.haltezeit=haltzeit;

	}

	@Override
	public String toString() {
		return "TransitScheduleTime [departureTime=" + departureTime
				+ ", haltezeit=" + haltezeit + "]";
	}
	
	
	
}
