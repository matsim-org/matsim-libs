package playground.vbmh.SFAnpassen;

import playground.vbmh.vmParking.ParkingPricingModel;
import playground.vbmh.vmParking.PricingModels;
import playground.vbmh.vmParking.PricingModelsWriter;

public class CreateDemoPricingModels {
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		PricingModels pricing_models = new PricingModels();
		PricingModelsWriter writer = new PricingModelsWriter();
		
		
		ParkingPricingModel model1 = new ParkingPricingModel();
		pricing_models.add(model1);
		model1.id=0;
		model1.setMaxTimeEV(24);
		model1.setMaxTimeNEV(24);
		model1.setPriceOfFirstMinuteEV(0);
		model1.setPriceOfFirstMinuteNEV(1);
		model1.setPricePerMinuteEV(1/60.0);
		model1.setPricePerMinuteNEV(2/60.0);
		
		
		ParkingPricingModel model2 = new ParkingPricingModel();
		pricing_models.add(model2);
		model2.id=3;
		model2.setMaxTimeEV(24);
		model2.setMaxTimeNEV(24);
		model2.setPriceOfFirstMinuteEV(0);
		model2.setPriceOfFirstMinuteNEV(0);
		model2.setPricePerMinuteEV(5/60.0);
		model2.setPricePerMinuteNEV(5/60.0);
		
		writer.write(pricing_models, "input/parking_pricing_models_demo.xml");
		
		
		

	}

}
