package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;

public class PreferredParking extends ParkingImpl {

	public PreferredParking(Coord coord, String attributes) {
		super(coord);
		this.attributes=attributes;
	}

	public String getAttributes() {
		return attributes;
	}

	String attributes=null;
}
