package playground.mzilske.withinday;

import org.matsim.api.core.v01.Id;

public interface World {

	ActivityPlane getActivityPlane();

	TeleportationPlane getTeleportationPlane();

	double getTime();

	void done();

	Id getLocation();

	RoadNetworkPlane getRoadNetworkPlane();

}
