package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TSPShipment.TimeWindow;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class TSPPlanReader extends MatsimXmlParser{

	private Logger logger = Logger.getLogger(TSPPlanReader.class);
	
	private Collection<TransportServiceProviderImpl> tsps;
	
	private Map<String,TSPShipment> currentShipments = null;
	
	private TransportServiceProviderImpl tsp = null;
	
	private Collection<TransportChain> currentChains = null;
	
	private TransportChainBuilder currentChainBuilder = null;
	
	public TSPPlanReader(Collection<TransportServiceProviderImpl> tsps) {
		super();
		this.tsps = tsps;
	}
	
	public void read(String filename){

		logger.info("read tsp plans");
		this.setValidating(false);
		parse(filename);
		logger.info("done");

	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals("transportServiceProvider")){
			tsp = TSPUtils.createTSP(atts.getValue("id"));
		}
		if(name.equals("transhipmentCentre")){
			TSPUtils.createAndAddTranshipmentCentre(tsp, atts.getValue("linkId"));
		}
		if(name.equals("shipments")){
			currentShipments = new HashMap<String, TSPShipment>();
		}
		if(name.equals("shipment")){
			String id = atts.getValue("id");
			Id from = makeId(atts.getValue("from"));
			Id to = makeId(atts.getValue("to"));
			int size = getInt(atts.getValue("size"));
			TSPShipment shipment = TSPUtils.createTSPShipment(from, to, size, 0, 24*3600, 0, 24*3600);
			currentShipments.put(id, shipment);
			TSPUtils.createAndAddTSPContract(tsp, shipment);
		}
		if(name.equals("transportChains")){
			currentChains = new ArrayList<TransportChain>();
		}
		if(name.equals("transportChain")){
			TSPShipment shipment = currentShipments.get(atts.getValue("shipmentId"));
			currentChainBuilder = new TransportChainBuilder(shipment);
		}
		if(name.equals("act")){
			Id location = makeId(atts.getValue("linkId"));
			String startTime = atts.getValue("start");
			String endTime = atts.getValue("end");
			Double start = null;
			Double end = null;
			TimeWindow tw = null;
			if(startTime != null && endTime != null){
				start = Double.parseDouble(startTime);
				end = Double.parseDouble(endTime);
				tw = new TimeWindow(start, end);
			}
			else{
				tw = new TimeWindow(0.0,Double.MAX_VALUE);
			}
			if(atts.getValue("type").equals("pickup")){	
				currentChainBuilder.schedulePickup(location, tw);
			}
			if(atts.getValue("type").equals("delivery")){
				currentChainBuilder.scheduleDelivery(location, tw);
			}
		}
		if(name.equals("leg")){
			CarrierOffer offer = new CarrierOffer();
			offer.setId(makeId(atts.getValue("carrierId")));
			currentChainBuilder.scheduleLeg(offer);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("transportChain")){
			currentChains.add(currentChainBuilder.build());
		}
		if(name.equals("transportServiceProvider")){
			TSPPlan plan = null;
			if(currentChains != null){
				plan = new TSPPlan(currentChains);
			}
			else{
				plan = new TSPPlan(Collections.EMPTY_LIST);
			}
			tsp.setSelectedPlan(plan);
			tsps.add(tsp);
			reset();
		}
	}

	private void reset() {
		currentShipments = null;
		currentChainBuilder = null;
		currentChains = null;
		tsp = null;
	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

	private Id makeId(String value) {
		return new IdImpl(value);
	}

}
