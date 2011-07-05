 package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPOffer;
import freight.CommodityFlow;
import freight.ScheduledCommodityFlow;
import freight.ShipperContract;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;

public class SimpleShipperPlanBuilder {

	private TSPAgentTracker tspAgentTracker;
	
	private TLCostFunction tlcFunction;
	
	private int[] sizes = { 5, 10, 15 };
	
	double timePeriod = 3600*24;
	
	public SimpleShipperPlanBuilder(TSPAgentTracker tspAgentTracker) {
		this.tspAgentTracker = tspAgentTracker;
	}

	public ShipperPlan buildPlan(ShipperImpl s) {
		Collection<ScheduledCommodityFlow> scheduledCommodityFlows = new ArrayList<ScheduledCommodityFlow>();
		for(ShipperContract sC : s.getContracts()){
			CommodityFlow comFlow = sC.getCommodityFlow(); 
			double minCost = Double.MAX_VALUE;
			ScheduledCommodityFlow bestScheduledCommodityFlow = null;
			for(int shipmentSize : sizes){
				Collection<TSPOffer> offers = tspAgentTracker.requestService(comFlow.getFrom(), comFlow.getTo(), shipmentSize);
				TSPOffer cheapestOffer = getCheapestOffer(offers);
				double cost = tlcFunction.getTLC(comFlow.getValue(), shipmentSize, cheapestOffer.getPrice());
				if(cost < minCost){
					minCost = cost;
					bestScheduledCommodityFlow = ShipperUtils.createScheduledCommodityFlow(comFlow,makeShipments(comFlow,comFlow.getSize()/shipmentSize,shipmentSize,timePeriod), cheapestOffer);
				}
			}
			scheduledCommodityFlows.add(bestScheduledCommodityFlow);
		}
		return ShipperUtils.createPlan(scheduledCommodityFlows);
	}
	
	private Collection<ShipperShipment> makeShipments(CommodityFlow comFlow, int frequency, int shipmentSize, double timePeriod){
		Collection<ShipperShipment> shipments = new ArrayList<ShipperShipment>();
		for(int i=0;i<frequency;i++){
			shipments.add(ShipperUtils.createShipment(comFlow.getFrom(), comFlow.getTo(), shipmentSize,
					i*timePeriod, (i+1)*timePeriod, i*timePeriod, (i+1)*timePeriod));
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

}
