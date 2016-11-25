package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;

public class ParkingPrice1 extends ParkingPrice{

	
	public double getPrice(double startParkingTime, double endParkingTime) {
		return GeneralLib.getIntervalDuration(startParkingTime,endParkingTime);
	}

}
