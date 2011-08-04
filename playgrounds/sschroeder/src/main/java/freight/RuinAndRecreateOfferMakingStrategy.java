package freight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.OfferMaker;

public class RuinAndRecreateOfferMakingStrategy implements OfferMaker{
	
	List<OfferMaker> offerMakers = new ArrayList<OfferMaker>();
	
	List<Double> weights = new ArrayList<Double>();

	private CarrierImpl carrier;
	
	private OfferRecorder offerRecorder;
	
	public void setOfferRecorder(OfferRecorder offerRecorder) {
		this.offerRecorder = offerRecorder;
	}

	public RuinAndRecreateOfferMakingStrategy(CarrierImpl carrier) {
		this.carrier = carrier;
	}

	public void addStrategy(OfferMaker offerMaker, Double weight){
		offerMakers.add(offerMaker);
		weights.add(weight);
	}

	@Override
	public void init() {
		
		
	}

	@Override
	public void reset() {
		
		
	}

	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize,Double memorizedPrice) {
		
		return null;
	}

	@Override
	public CarrierOffer requestOffer(Id from, Id to, int shimpentSize,Double startPickup, Double endPickup, Double startDelivery,Double endDelivery, Double memorizedPrice) {
		assertPickupTimes(from,startPickup,0.0,86400.0);
		if(memorizedPrice != null){
			if(MatsimRandom.getRandom().nextDouble() < 0.6){
				CarrierOffer offer = new CarrierOffer();
				offer.setId(carrier.getId());
				offer.setPrice(memorizedPrice);
				return offer;
			}
		}
		OfferMaker om = selectOM();
		CarrierOffer requestOffer = om.requestOffer(from, to, shimpentSize, startPickup, endPickup, startDelivery, endDelivery, null);
		offerRecorder.add(carrier.getId(),from,to,shimpentSize,requestOffer.getPrice(),om.getClass().toString());
		return requestOffer;
	}

	private void assertPickupTimes(Id from, Double startPickup, double d, double e) {
		if(from.toString().equals("industry")){
			if(startPickup == d || startPickup == e){
				return;
			}
			else{
				throw new IllegalStateException("this should not be");
			}
		}
		
	}

	private OfferMaker selectOM() {
		double randNr = MatsimRandom.getRandom().nextDouble();
		double currentWeight = 0.0;
		for(Double d : weights){
			currentWeight += d;
			if(randNr < currentWeight){
				int index = weights.indexOf(d);
				return offerMakers.get(index);
			}
		}
		return null;
	}
	
	  

}
