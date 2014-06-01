package playground.vbmh.extendedPricingModels;

import playground.vbmh.vmParking.ParkingPricingModel;
import playground.vbmh.vmParking.ParkingSpot;

public class specialTestModel extends ParkingPricingModel {
	public int id = 0;
	
	public double calculateParkingPrice(double duration, boolean ev, ParkingSpot spot){
		duration = duration/3600; //seconds to hours
		double occupancyEV = spot.parking.getOccupancyEVSpots();
		double price = 0;
		System.out.println("test model in use");
		if (ev){
			
			price = 10;
		} else {

			price = 20;
		}
		
		if(spot.isCharge()){
			price += 5*occupancyEV;
		}
		return price;
	}

}
