package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.api.core.v01.Id;

public class ParkingPriceMapping1 implements ParkingPriceMapping {


	public ParkingPrice getParkingPrice(Id facilityId) {
			return new ParkingPrice1();
	}

}
