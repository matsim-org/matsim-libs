package playground.wrashid.parkingSearch.planLevel.parkingPrice;

public class ParkingPrice1 extends ParkingPrice{

	
	public double getPrice(double startParkingTime, double endParkingTime) {
		checkPreCond_startPSmallerThanEndPTime(startParkingTime, endParkingTime);
		
		return (endParkingTime-startParkingTime);
	}

}
