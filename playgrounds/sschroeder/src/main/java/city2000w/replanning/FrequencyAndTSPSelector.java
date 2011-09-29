package city2000w.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TimeWindow;
import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.events.OfferUtils;
import playground.mzilske.freight.events.QueryTSPOffersEvent;
import playground.mzilske.freight.events.Service;
import playground.mzilske.freight.events.ShipperTSPContractAcceptEvent;
import playground.mzilske.freight.events.ShipperTSPContractCanceledEvent;
import freight.CommodityFlow;
import freight.ScheduledCommodityFlow;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperShipment;
import freight.ShipperUtils;
import freight.TSPUtils;
import freight.TlcCostFunction;
import freight.utils.TimePeriod;
import freight.utils.TimePeriods;

public class FrequencyAndTSPSelector implements ShipperPlanStrategyModule {
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private TlcCostFunction costFunction;
	
	public List<Integer> frequencies = new ArrayList<Integer>();
	
	public TimePeriods timePeriods = new TimePeriods();
	
	public FrequencyAndTSPSelector(ShipperAgentTracker shipperAgentTracker) {
		super();
		this.shipperAgentTracker = shipperAgentTracker;
	}

	public void setCostFunction(TlcCostFunction costFunction) {
		this.costFunction = costFunction;
	}

	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleActor(ShipperImpl shipper) {
		ShipperPlan plan2replan = shipper.getSelectedPlan();
		int nOfFlowsToReplan = MatsimRandom.getRandom().nextInt(plan2replan.getScheduledFlows().size());
		List<ScheduledCommodityFlow> shuffeledList = new ArrayList<ScheduledCommodityFlow>(plan2replan.getScheduledFlows());
		Collections.shuffle(shuffeledList,MatsimRandom.getRandom());
		
		List<ScheduledCommodityFlow> scheduledCommodityFlows = new ArrayList<ScheduledCommodityFlow>();
		
		for(int i=0;i<nOfFlowsToReplan;i++){
			ScheduledCommodityFlow flow = shuffeledList.get(i);
			TSPOffer bestOffer = null;
			Integer bestFreq = null;
			Integer bestLotsize = null; 
			double bestTLC = Double.MAX_VALUE;
			for(Integer frequency : frequencies){
				int lotsize = Math.round(flow.getCommodityFlow().getSize()/frequency);
				TSPOffer offer = getOffer(flow.getCommodityFlow(),lotsize,frequency);
				double tlc = computeTLC(flow.getCommodityFlow(),lotsize,frequency,offer);
				if(tlc < bestTLC){
					bestTLC = tlc;
					bestOffer = offer;
					bestFreq = frequency;
					bestLotsize = lotsize;
				}
			}
			ScheduledCommodityFlow newFlow = scheduleFlow(shipper.getId(),flow.getCommodityFlow(),bestLotsize,bestFreq,bestOffer);
			informWorld(flow,newFlow);
			scheduledCommodityFlows.add(newFlow);
		}
		
		for(int i=nOfFlowsToReplan;i<plan2replan.getScheduledFlows().size();i++){
			scheduledCommodityFlows.add(shuffeledList.get(i));
		}
		shipper.setSelectedPlan(new ShipperPlan(scheduledCommodityFlows));
		assertEquals(plan2replan.getScheduledFlows().size(),11);
	}
	
	private TSPOffer getOffer(CommodityFlow commodityFlow, int lotsize, Integer frequency) {
		Service service = OfferUtils.createService(commodityFlow.getFrom(), commodityFlow.getTo(), lotsize, getDummyTW(), getDummyTW());
		Collection<Offer> offers = new ArrayList<Offer>();
		QueryTSPOffersEvent queryEvent = new QueryTSPOffersEvent(offers, service);
		shipperAgentTracker.processEvent(queryEvent);
		List<Offer> offerList = new ArrayList<Offer>(queryEvent.getOffers());
		Collections.sort(offerList, new Comparator<Offer>(){

			@Override
			public int compare(Offer arg0, Offer arg1) {
				if(arg0.getPrice() < arg1.getPrice()){
					return -1;
				}
				else{
					return 1;
				}
			}
		});
		TSPOffer bestOffer = (TSPOffer)offerList.get(0);
		return bestOffer;
	}

	public void setFrequencies(List<Integer> frequencies) {
		this.frequencies = frequencies;
	}

	public void setTimePeriods(TimePeriods timePeriods) {
		this.timePeriods = timePeriods;
	}

	private TimeWindow getDummyTW() {
		return new TimeWindow(0.0, Double.MAX_VALUE);
	}

	private double computeTLC(CommodityFlow commodityFlow, int lotsize, Integer frequency, TSPOffer offer) {
		return costFunction.getCosts(commodityFlow, lotsize, frequency, offer.getPrice());
	}

	private ScheduledCommodityFlow scheduleFlow(Id shipperId, CommodityFlow commodityFlow, int lotsize, Integer frequency, TSPOffer offer) {
		List<ShipperShipment> shipperShipments = new ArrayList<ShipperShipment>();
		List<Contract> tspContracts = new ArrayList<Contract>();
		for(int i=0; i<frequency; i++){
			TimePeriod timePeriod = timePeriods.getPeriods().get(i);
			ShipperShipment shipment = ShipperUtils.createShipment(commodityFlow.getFrom(), commodityFlow.getTo(), lotsize, timePeriod.start, timePeriod.end, timePeriod.start, timePeriod.end);
			shipperShipments.add(shipment);
		}
		for(ShipperShipment shipment : shipperShipments){
			TSPShipment tspShipment = TSPUtils.createTSPShipment(commodityFlow.getFrom(), commodityFlow.getTo(), shipment.getSize(),shipment.getPickupTimeWindow(), shipment.getDeliveryTimeWindow());
			TSPContract contract = new TSPContract(shipperId, offer.getId(), tspShipment, offer);
			tspContracts.add(contract);
		}
		ScheduledCommodityFlow flow = new ScheduledCommodityFlow(commodityFlow, shipperShipments, tspContracts);
		return flow;
	}

	private void informWorld(ScheduledCommodityFlow flow, ScheduledCommodityFlow newFlow) {
		for(Contract c : flow.getContracts()){
			shipperAgentTracker.processEvent(new ShipperTSPContractCanceledEvent(c));
		}
//		shipperAgentTracker.processEvent(new ScheduledCommodityFlowRemovedEvent(flow));
//		shipperAgentTracker.processEvent(new ScheduledCommodityFlowAddedEvent(newFlow));
		for(Contract c : newFlow.getContracts()){
			shipperAgentTracker.processEvent(new ShipperTSPContractAcceptEvent(c));
		}
	}

	private void assertEquals(int size, int i) {
		if(size != i){
			throw new IllegalStateException("this could not be. #comFlows in Plan = " + size);
		}
		
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}

}
