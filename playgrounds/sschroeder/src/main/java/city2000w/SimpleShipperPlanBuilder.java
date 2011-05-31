package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import freight.CommodityFlow;
import freight.ScheduledCommodityFlow;
import freight.ShipperContract;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;
import freight.TSPUtils;

import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPShipment;

public class SimpleShipperPlanBuilder {

	private TSPAgentTracker tspAgentTracker;
	
	private TLCostFunction tlcFunction;
	
	public SimpleShipperPlanBuilder(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public ShipperPlan buildPlan(ShipperImpl s) {
		Collection<ScheduledCommodityFlow> scheduledCommodityFlows = new ArrayList<ScheduledCommodityFlow>();
		for(ShipperContract sC : s.getContracts()){
			ScheduledCommodityFlow scheduledCommodityFlow = null;
			CommodityFlow comFlow = sC.getCommodityFlow();
			Collection<TSPShipment> tspShipments_Freq1 = new ArrayList<TSPShipment>(); 
			int lotsizeFor1 = makeTSPShipmentsAndGetLotsize(comFlow, 1, tspShipments_Freq1);
			Collection<TSPOffer> offersFor1 = tspAgentTracker.requestServices(tspShipments_Freq1);
			TSPOffer cheapestOfferFor1 = getCheapestOffer(offersFor1);
			double tlcFor1 = tlcFunction.getTLC(comFlow.getValue(),lotsizeFor1,cheapestOfferFor1.getPrice());
			
			Collection<TSPShipment> tspShipments_Freq5 = new ArrayList<TSPShipment>(); 
			int lotsizeFor5 = makeTSPShipmentsAndGetLotsize(comFlow, 5, tspShipments_Freq5);
			Collection<TSPOffer> offersFor5 = tspAgentTracker.requestServices(tspShipments_Freq5);
			TSPOffer cheapestOfferFor5 = getCheapestOffer(offersFor5);
			double tlcFor5 = tlcFunction.getTLC(comFlow.getValue(),lotsizeFor5,cheapestOfferFor5.getPrice());
			
			if(tlcFor1 < tlcFor5){
				scheduledCommodityFlow = ShipperUtils.createScheduledCommodityFlow(comFlow,makeShippersShipments(tspShipments_Freq1), cheapestOfferFor1);
			}
			else{
				scheduledCommodityFlow = ShipperUtils.createScheduledCommodityFlow(comFlow,makeShippersShipments(tspShipments_Freq5), cheapestOfferFor5);
			}
			scheduledCommodityFlows.add(scheduledCommodityFlow);
		}
		return ShipperUtils.createPlan(scheduledCommodityFlows);
	}

	private Collection<ShipperShipment> makeShippersShipments(Collection<TSPShipment> tspShipments) {
		Collection<ShipperShipment> shipments = new ArrayList<ShipperShipment>();
		for(TSPShipment tspShipment : tspShipments){
			shipments.add(ShipperUtils.createShipment(tspShipment));
		}
		return shipments;
	}

	private TSPOffer getCheapestOffer(Collection<TSPOffer> offers) {
		TSPOffer bestOffer = null;
		for(TSPOffer o : offers){
			if(bestOffer == null){
				bestOffer = o;
			}
			else{
				if(o.getPrice() < bestOffer.getPrice()){
					bestOffer = o;
				}
			}
		}
		return bestOffer;
	}

	private int makeTSPShipmentsAndGetLotsize(CommodityFlow comFlow, int frequency, Collection<TSPShipment> tspShipments) {
		int lotSize = comFlow.getSize() / frequency;
		for(int i=0;i<frequency;i++){
			tspShipments.add(TSPUtils.createTSPShipment(comFlow.getFrom(), comFlow.getTo(), lotSize, 
					i*24*3600, (i+1)*24*3600, i*24*3600, (i+1)*24*3600));
		}
		return lotSize;
	}

}
