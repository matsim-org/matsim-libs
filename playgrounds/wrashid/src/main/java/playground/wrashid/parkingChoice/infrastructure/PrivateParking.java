package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;

public class PrivateParking extends ParkingImpl {

	public PrivateParking(Coord coord, ActInfo actInfo) {
		super(coord);
		this.belongsToAct=actInfo;
	}

	ActInfo belongsToAct=null;
	
	public ActInfo getActInfo(){
		return belongsToAct;
	}
	
	@Override
	public String toString() {
		return super.toString() + " [belongsToAct=" + belongsToAct + "]" ;
	}
	
}
