package vwExamples.utils.tripAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class LinkTravelTime {
	Id<Vehicle> vehicleId;
	double linkEnterTime;
	double linkLeaveTime;
	Id<Link> linkId;
	
	LinkTravelTime(Id<Vehicle> vehicleId,double linkEnterTime,double linkLeaveTime,Id<Link> linkId)
	{
		this.vehicleId = vehicleId;
		this.linkEnterTime = linkEnterTime;
		this.linkLeaveTime = linkLeaveTime;
		this.linkId = linkId;
		
	}
}
