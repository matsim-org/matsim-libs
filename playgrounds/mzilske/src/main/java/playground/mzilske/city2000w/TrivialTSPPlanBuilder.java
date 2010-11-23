package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TSPShipment.TimeWindow;


public class TrivialTSPPlanBuilder {

	public TSPPlan buildPlan(Collection<TSPContract> contracts,
			TSPCapabilities tspCapabilities) {
		Collection<TransportChain> chains = new ArrayList<TransportChain>();
		for(TSPContract c : contracts){
			
			for(TSPShipment s : c.getShipments()){
				TransportChainBuilder chainBuilder = new TransportChainBuilder(s);
				chainBuilder.schedulePickup(s.getFrom(),s.getPickUpTimeWindow());
				IdImpl carrierId = new IdImpl("hans");
				chainBuilder.scheduleLeg(carrierId);
				Id transshipmentCentre = pickTransshipmentCentre(tspCapabilities.getTransshipmentCentres());
				if(transshipmentCentre == null){
					chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				}
				else{
					chainBuilder.scheduleDelivery(transshipmentCentre, new TimeWindow(0.0,24*3600));
					chainBuilder.schedulePickup(transshipmentCentre, new TimeWindow(0.0,24*3600));
					Id lineHaulCarrier = new IdImpl("ulrich");
					chainBuilder.scheduleLeg(lineHaulCarrier);
					chainBuilder.scheduleDelivery(s.getTo(),s.getDeliveryTimeWindow());
				}
				chains.add(chainBuilder.build());
			}
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

	private Id pickTransshipmentCentre(List<Id> transshipmentCentres) {
		if(!transshipmentCentres.isEmpty()){
			Random random = new Random();
			int randIndex = random.nextInt(transshipmentCentres.size());
			return transshipmentCentres.get(randIndex);
		}
		return null;
	}
	
	

}
