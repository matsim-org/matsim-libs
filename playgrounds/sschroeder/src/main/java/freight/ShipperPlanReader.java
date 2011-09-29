package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.mzilske.freight.TSPOffer;
import trbFolder.TRBShippersContractGenerator.TimeProfile;

public class ShipperPlanReader extends MatsimXmlParser{

	private static Logger logger = Logger.getLogger(ShipperPlanReader.class);
	
	private Collection<ShipperImpl> shippers;
	
	private ShipperImpl currentShipper = null;
	
	private Map<String,CommodityFlow> currentComFlows;
	
	private List<ScheduledCommodityFlow> currentScheduledFlows;
	
	private String currentComFlowId;

	private List<ShipperShipment> currentShipments;

	private CommodityFlow currentComFlow;

	private TSPOffer currentTspOffer;
	
	private Integer currentFrequency;
	
	private List<TimeProfile> currentTimeProfile;
	
	
	public ShipperPlanReader(Collection<ShipperImpl> shippers) {
		super();
		this.shippers = shippers;
	}

	public void read(String filename){

		logger.info("read shipper plans");
		this.setValidating(false);
		parse(filename);
		logger.info("done");

	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals("shipper")){
			currentShipper = ShipperUtils.createShipper(atts.getValue("id"), atts.getValue("linkId"));
		}
		if(name.equals("commodityFlows")){
			currentComFlows = new HashMap<String, CommodityFlow>();
		}
		if(name.equals("commodityFlow")){
			CommodityFlow comFlow = ShipperUtils.createCommodityFlow(atts.getValue("from"), atts.getValue("to"), getInt(atts.getValue("size")), getDouble(atts.getValue("value")));
			ShipperUtils.createAndAddContract(currentShipper, comFlow);
			currentComFlows.put(atts.getValue("id"), comFlow);
		}
		if(name.equals("scheduledFlows")){
			currentScheduledFlows = new ArrayList<ScheduledCommodityFlow>();
		}
		if(name.equals("scheduledFlow")){
			currentComFlowId = atts.getValue("comFlowId");
			currentComFlow = currentComFlows.get(currentComFlowId);
			currentTspOffer = makeTspOffer(atts.getValue("tspId"),atts.getValue("price"));
			currentShipments = new ArrayList<ShipperShipment>();
		}
		if(name.equals("shipment")){
			ShipperShipment shipment = ShipperUtils.createShipment(currentComFlow.getFrom(), currentComFlow.getTo(), getInt(atts.getValue("size")), 
					getDouble(atts.getValue("startPickup")), getDouble(atts.getValue("endPickup")), getDouble(atts.getValue("startDelivery")), 
					getDouble(atts.getValue("endDelivery")));
			currentShipments.add(shipment);
		}
		if(name.equals("frequency")){
			currentTimeProfile = new ArrayList<TimeProfile>();
			currentFrequency = getInt(atts.getValue("id"));
		}
		if(name.equals("timeProfile")){
			currentTimeProfile.add(new TimeProfile(getDouble(atts.getValue("startPickup")), getDouble(atts.getValue("endPickup")), 
					getDouble(atts.getValue("startDelivery")), getDouble(atts.getValue("endDelivery"))));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("scheduledFlow")){
			ScheduledCommodityFlow scheduledComFlow = ShipperUtils.createScheduledCommodityFlow(currentShipper.getId(), currentComFlow, currentShipments, currentTspOffer);
			currentScheduledFlows.add(scheduledComFlow);
		}
		if(name.equals("scheduledFlows")){
			ShipperPlan plan = ShipperUtils.createPlan(currentScheduledFlows);
			currentShipper.setSelectedPlan(plan);
		}
		if(name.equals("shipper")){
			shippers.add(currentShipper);
		}
		if(name.equals("frequency")){
			currentShipper.getShipperKnowledge().addTimeProfile(currentFrequency, currentTimeProfile);
			currentTimeProfile = null;
			currentFrequency = null;
		}
	}

	private TSPOffer makeTspOffer(String tspIdString, String priceString) {
		TSPOffer offer = new TSPOffer();
		Id tspId = makeId(tspIdString);
		Double price = Double.parseDouble(priceString);
		offer.setId(tspId);
		offer.setPrice(price);
		return offer;
	}

	private Id makeId(String tspIdString) {
		return new IdImpl(tspIdString);
	}

	private double getDouble(String value) {
		return Double.parseDouble(value);
	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

}
