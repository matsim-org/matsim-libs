package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.contrib.parking.lib.GeneralLib;

public class ParkingPrice2 extends ParkingPrice {

	public double getPrice(double startParkingTime, double endParkingTime) {
		return GeneralLib.getIntervalDuration(startParkingTime,endParkingTime)*2;
	}

}
