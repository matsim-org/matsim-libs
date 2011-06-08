package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

public class PrivateParking extends Parking {

	public PrivateParking(Coord coord, ActInfo actInfo) {
		super(coord);
		this.belongsToAct=actInfo;
	}

	ActInfo belongsToAct=null;
	
	public ActInfo getCorrespondingActInfo(){
		return belongsToAct;
	}
	
	
}
