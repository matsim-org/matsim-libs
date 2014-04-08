package playground.wrashid.bsc.vbmh.extendedPricingModels;

import playground.wrashid.bsc.vbmh.vmParking.ParkingPricingModel;

public class specialTestModel extends ParkingPricingModel {
	public int id = 0;
	
	public double calculateParkingPrice(double duration, boolean ev){
		duration = duration/3600; //von Sekunden auf Minuten
		double price = 0;
		System.out.println("test model benutzt");
		if (ev){
			
			price = 100;
		} else {
			//System.out.println(model.getPricePerMinuteNEV());
			price = 200;
		}
		
		return price;
	}

}
