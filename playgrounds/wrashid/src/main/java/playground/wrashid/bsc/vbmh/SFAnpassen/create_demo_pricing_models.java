package playground.wrashid.bsc.vbmh.SFAnpassen;

import playground.wrashid.bsc.vbmh.vm_parking.Parking_Pricing_Model;
import playground.wrashid.bsc.vbmh.vm_parking.Pricing_Models;
import playground.wrashid.bsc.vbmh.vm_parking.Pricing_Models_Writer;

public class create_demo_pricing_models {
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		Pricing_Models pricing_models = new Pricing_Models();
		Pricing_Models_Writer writer = new Pricing_Models_Writer();
		
		
		Parking_Pricing_Model model1 = new Parking_Pricing_Model();
		pricing_models.add(model1);
		model1.id=0;
		model1.max_time_ev=24;
		model1.max_time_nev=24;
		model1.price_of_first_minute_ev=0;
		model1.price_of_first_minute_nev=1;
		model1.price_per_minute_ev=1/60.0;
		model1.price_per_minute_nev=2/60.0;
		
		
		Parking_Pricing_Model model2 = new Parking_Pricing_Model();
		pricing_models.add(model2);
		model2.id=3;
		model2.max_time_ev=24;
		model2.max_time_nev=24;
		model2.price_of_first_minute_ev=0;
		model2.price_of_first_minute_nev=0;
		model2.price_per_minute_ev=5/60.0;
		model2.price_per_minute_nev=5/60.0;
		
		writer.write(pricing_models, "input/parking_pricing_models_demo.xml");
		
		
		

	}

}
