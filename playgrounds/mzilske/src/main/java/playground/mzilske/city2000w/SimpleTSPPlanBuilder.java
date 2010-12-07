package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;


public class SimpleTSPPlanBuilder {

	private Collection<CarrierImpl> carriers;
	
	private List<Id> transshipmentCentres;
	
	public void setCarriers(Collection<CarrierImpl> carriers) {
		this.carriers = carriers;
	}

	public void setTransshipmentCentres(List<Id> transshipmentCentres) {
		this.transshipmentCentres = transshipmentCentres;
	}

	public TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			for(TSPShipment s : c.getShipments()){
				TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
				chainBuilder.schedulePickup(s.getFrom(), s.getPickUpTimeWindow());
				for (Id transshipmentCentre : transshipmentCentres) {
					chainBuilder.scheduleLeg(pickCarrier().getId());
					chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
					chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(0.0,24*3600));
				}
				chainBuilder.scheduleLeg(pickCarrier().getId());
				chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				chains.add(chainBuilder.build());
			}
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private CarrierImpl pickCarrier() {
		List<CarrierImpl> carrierList = new ArrayList<CarrierImpl>(carriers);
		if(!carrierList.isEmpty()){
			Random random = new Random();
			int randIndex = random.nextInt(carrierList.size());
			return carrierList.get(randIndex);
		}
		return null;
	}
	
	

}
