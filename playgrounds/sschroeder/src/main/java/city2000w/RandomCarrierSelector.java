package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TSPShipment.TimeWindow;

public class RandomCarrierSelector {

	private Collection<CarrierImpl> carriers;
	
	private Collection<Id> tscs;
	
	public void setCarriers(Collection<CarrierImpl> carriers) {
		this.carriers = carriers;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.tscs = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts,TSPCapabilities tspCapabilities) {
		if(contracts.isEmpty()){
			return getEmptyPlan(tspCapabilities);
		}
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			TSPShipment s = c.getShipment();
			TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
			chainBuilder.schedulePickup(s.getFrom(), s.getPickUpTimeWindow());
			Id lastLocation = s.getFrom();
			TimeWindow lastPickupTW = s.getPickUpTimeWindow();
			for (Id transshipmentCentre : tspCapabilities.getTransshipmentCentres()) {
				TimeWindow deliveryTW = new TimeWindow(lastPickupTW.getStart(),lastPickupTW.getStart() + 2*3600);
				CarrierOffer offer = pickRandomOffer();
				chainBuilder.scheduleLeg(offer);
				chainBuilder.scheduleDelivery(transshipmentCentre, deliveryTW);
				TimeWindow pickupTW = new TimeWindow(deliveryTW.getEnd(),24*3600);
				chainBuilder.schedulePickup(transshipmentCentre, pickupTW);
				lastPickupTW = pickupTW;
			}
			CarrierOffer offer = pickRandomOffer();
			chainBuilder.scheduleLeg(offer);
			chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
			chains.add(chainBuilder.build());

		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}
	
	private CarrierOffer pickRandomOffer() {
		CarrierOffer o = new CarrierOffer();
		Id carrierId = pickRandomCarrier();
		o.setId(carrierId);
		return o;
	}

	private Id pickRandomCarrier() {
		List<CarrierImpl> carrierList = new ArrayList<CarrierImpl>(carriers);
		Collections.shuffle(carrierList,MatsimRandom.getRandom());
		return carrierList.get(0).getId();
	}

	private TSPPlan getEmptyPlan(TSPCapabilities tspCapabilities) {
		TSPPlan plan = new TSPPlan(Collections.EMPTY_LIST);
		return plan;
	}


}
