package playground.mkillat.pt_test;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class CompleteTransitRoute {

	Id driverId;
	Id vehicleId;
	Id transitLineId;
	Id transitRouteId;
	Id departureId;
	double transitDriverStartTime;
	List <Double> arrives;
	List <Double> departures;
	
	public CompleteTransitRoute (Id driverId, Id vehicleId, Id transitLineId, Id transitRouteId, Id departureId, double transitDriverStartTime,List <Double>arrives, List <Double> departures ){
		this.driverId=driverId;
		this.vehicleId=vehicleId;
		this.transitLineId=transitLineId;
		this.transitRouteId=transitRouteId;
		this.departureId=departureId;
		this.transitDriverStartTime=transitDriverStartTime;
		this.arrives=arrives;
		this.departures= departures;
	}

	@Override
	public String toString() {
		return "CompleteTransitRoute [driverId=" + driverId + ", vehicleId="
				+ vehicleId + ", transitLineId=" + transitLineId
				+ ", transitRouteId=" + transitRouteId + ", departureId="
				+ departureId + ", transitDriverStartTime="
				+ transitDriverStartTime + ", arrives=" + arrives
				+ ", departures=" + departures + "]";
	}


	
	

	
	
	
	
}
