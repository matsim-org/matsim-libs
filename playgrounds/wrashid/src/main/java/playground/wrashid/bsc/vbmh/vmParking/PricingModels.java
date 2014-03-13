package playground.wrashid.bsc.vbmh.vmParking;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Keeps all pricing model objects.
 * !! Berechnet Parkpreise >> Evntl. besser ins einzelne Model auslagern.
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



@XmlRootElement
public class PricingModels {
	private static List<ParkingPricingModel> parkingprices = new LinkedList<ParkingPricingModel>();
	
	@XmlElement(name = "Parking_Pricing_Model")
	public List<ParkingPricingModel> getParking_Pricing_Models() {
		return parkingprices;
	}
	
	public void add(ParkingPricingModel model){
		parkingprices.add(model);

	}

	public ParkingPricingModel get_model(int model_id){
		// Alle in Map schreiben zum beschleunigen?
		for (ParkingPricingModel model : parkingprices){
			if (model.id==model_id){
				return model;	
			}
		}
		
		//nichts gefunden
		return null;
		
	}
	
	public double calculateParkingPrice(double duration, boolean ev,int model_id){
		double price = 0;
		ParkingPricingModel model = get_model(model_id);
		if (ev){
			price = model.getPriceOfFirstMinuteEV() + duration * model.getPricePerMinuteEV();
		} else {
			price = model.getPriceOfFirstMinuteNEV() + duration * model.getPricePerMinuteNEV();
		}
			
		
		
		return price;
	}
	
	
}
