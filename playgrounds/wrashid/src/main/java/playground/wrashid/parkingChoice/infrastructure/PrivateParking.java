package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

public class PrivateParking extends Parking {

	public PrivateParking(Coord coord) {
		super(coord);
		// TODO Auto-generated constructor stub
	}

	ActivityFacility activityFacility=null;
	ActInfo belongsToAct=null;
	
	
}
