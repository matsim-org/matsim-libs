package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;

/**
 * The attributes field can be used to assign a single attribute or several comma separated attributes to the parking.
 * This field can then later be used in the ReservedParkingManger to check, if a certain person is allowed to park there.
 * 
 * @author wrashid
 *
 */
public class ReservedParking extends Parking {
	public ReservedParking(Coord coord, String attributes) {
		super(coord);
		this.attributes=attributes;
	}

	public String getAttributes() {
		return attributes;
	}

	String attributes=null;
	

}
