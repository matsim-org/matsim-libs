package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import playground.wrashid.lib.GeneralLib;

public class ParkingPrice1 extends ParkingPrice{

	
	public double getPrice(double startParkingTime, double endParkingTime) {
		return GeneralLib.getIntervalDuration(startParkingTime,endParkingTime);
	}

}
