package freight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;

import trbFolder.TRBShippersContractGenerator.TimeProfile;
import utils.XmlWriterUtils;


public class ShipperPlanWriter extends MatsimXmlWriter{
	
	private Logger logger = Logger.getLogger(ShipperPlanWriter.class);
	
	private Collection<ShipperImpl> shippers;
	
	private Integer currentComFlowCounter = 0;
	
	private Map<CommodityFlow,String> internalComFlowMap;

	public ShipperPlanWriter(Collection<ShipperImpl> shippers) {
		super();
		this.shippers = shippers;
	}
	
	public void write(String filename){
		try{
			logger.info("write shipper plans");
			openFile(filename);
			writeXmlHead();
			startShippers(writer);
			for(ShipperImpl shipper : shippers){
				startShipper(shipper,writer);
				startAndEndCommodityFlows(shipper.getContracts(),writer);
				startAndEndKnowledge(shipper.getShipperKnowledge(),writer);
				startAndEndScheduledCommodityFlows(shipper.getSelectedPlan(),writer);
				endShipper(writer);
			}
			endShippers(writer);
			close();
			logger.info("done");
		}
		catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void startAndEndKnowledge(ShipperKnowledge shipperKnowledge,BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(3) + "<knowledge>" + XmlWriterUtils.newLine());
		for(Integer freq : shipperKnowledge.getTimeProfileMap().keySet()){
			writer.write(XmlWriterUtils.tabs(4) + "<frequency id=" + XmlWriterUtils.inQuotation(freq) + ">" + XmlWriterUtils.newLine());
			for(TimeProfile timeProfile : shipperKnowledge.getTimeProfileMap().get(freq)){
				writer.write(XmlWriterUtils.tabs(5) + "<timeProfile ");
				writer.write("startPickup=" + XmlWriterUtils.inQuotation(timeProfile.pickupStart));
				writer.write(" endPickup=" + XmlWriterUtils.inQuotation(timeProfile.pickupEnd));
				writer.write(" startDelivery=" + XmlWriterUtils.inQuotation(timeProfile.deliveryStart));
				writer.write(" endDelivery=" + XmlWriterUtils.inQuotation(timeProfile.deliveryEnd) + "/>" + XmlWriterUtils.newLine());
			}
			writer.write(XmlWriterUtils.tabs(4) + "</frequency>" + XmlWriterUtils.newLine());
		}
		writer.write(XmlWriterUtils.tabs(3) + "</knowledge>" + XmlWriterUtils.newLine());
	}

	private void startShippers(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(1) + "<shippers>" + XmlWriterUtils.newLine());
	}

	private void startShipper(ShipperImpl shipper, BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(2) + "<shipper ");
		writer.write("id=" + XmlWriterUtils.inQuotation(shipper.getId().toString()) + " ");
		writer.write("linkId=" + XmlWriterUtils.inQuotation(shipper.getLocationId().toString()) + ">" + XmlWriterUtils.newLine());
		
	}

	private void startCommodityFlows(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(3) + "<commodityFlows>" + XmlWriterUtils.newLine());
		
	}

	private void startAndEndCommodityFlows(Collection<ShipperContract> contracts, BufferedWriter writer) throws IOException {
		internalComFlowMap = new HashMap<CommodityFlow, String>();
		startCommodityFlows(writer);
		for(ShipperContract c : contracts){
			startAndEndCommodityFlow(c.getCommodityFlow());
		}
		endCommodityFlows(writer);
	}

	private void startAndEndCommodityFlow(CommodityFlow commodityFlow) throws IOException {
		String internalComFlowId = getInternalComFlowId();
		internalComFlowMap.put(commodityFlow, internalComFlowId);
		writer.write(XmlWriterUtils.tabs(4) + "<commodityFlow ");
		writer.write("id=" + XmlWriterUtils.inQuotation(internalComFlowId) + " ");
		writer.write("from=" + XmlWriterUtils.inQuotation(commodityFlow.getFrom().toString()) + " ");
		writer.write("to=" + XmlWriterUtils.inQuotation(commodityFlow.getTo().toString()) + " ");
		writer.write("size=" + XmlWriterUtils.inQuotation(commodityFlow.getSize()) + " ");
		writer.write("value=" + XmlWriterUtils.inQuotation(commodityFlow.getValue()) + "/>" + XmlWriterUtils.newLine());
	}

	private void endCommodityFlows(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(3) + "</commodityFlows>" + XmlWriterUtils.newLine());
		
	}

	private void startAndEndScheduledCommodityFlows(ShipperPlan selectedPlan, BufferedWriter writer) throws IOException {
		if(selectedPlan == null){
			return;
		}
		startScheduledCommodityFlows(writer);
		for(ScheduledCommodityFlow sCF : selectedPlan.getScheduledFlows()){
			startScheduledFlow(sCF, writer);
			startAndEndShipments(sCF.getShipments(), writer);
			endScheduledFlow(writer);
		}
		endScheduledCommodityFlows(writer);
	}

	private void startScheduledCommodityFlows(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(3) + "<scheduledFlows>" + XmlWriterUtils.newLine());
	}

	private void startScheduledFlow(ScheduledCommodityFlow sCF, BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(4) + "<scheduledFlow ");
		writer.write("comFlowId=" + XmlWriterUtils.inQuotation(getComFlowId(sCF.getCommodityFlow())) + " ");
		writer.write("tspId=" + XmlWriterUtils.inQuotation(sCF.getContracts().iterator().next().getOffer().getId().toString()) + " ");
		writer.write("price=" + XmlWriterUtils.inQuotation(sCF.getContracts().iterator().next().getOffer().getPrice()) + ">" + XmlWriterUtils.newLine());
	}

	private void startAndEndShipments(Collection<ShipperShipment> shipments, BufferedWriter writer) throws IOException {
		for(ShipperShipment s : shipments){
			writer.write(XmlWriterUtils.tabs(5) + "<shipment ");
			writer.write("size=" + XmlWriterUtils.inQuotation(s.getSize()) + " ");
			writer.write("startPickup=" + XmlWriterUtils.inQuotation(s.getPickupTimeWindow().getStart()) + " ");
			writer.write("endPickup=" + XmlWriterUtils.inQuotation(s.getPickupTimeWindow().getEnd()) + " ");
			writer.write("startDelivery=" + XmlWriterUtils.inQuotation(s.getDeliveryTimeWindow().getStart()) + " ");
			writer.write("endDelivery=" + XmlWriterUtils.inQuotation(s.getDeliveryTimeWindow().getEnd()) + "/>" + XmlWriterUtils.newLine());
		}
		
	}

	private void endScheduledFlow(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(4) + "</scheduledFlow>" + XmlWriterUtils.newLine());
		
	}

	private void endScheduledCommodityFlows(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(3) + "</scheduledFlows>" + XmlWriterUtils.newLine());
		
	}

	private void endShipper(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(2) + "</shipper>" + XmlWriterUtils.newLine());
		resetComFlowCounter();
	}

	private void resetComFlowCounter() {
		currentComFlowCounter = 0;
	}

	private void endShippers(BufferedWriter writer) throws IOException {
		writer.write(XmlWriterUtils.tabs(1) + "</shippers>" + XmlWriterUtils.newLine());
	}

	private String getComFlowId(CommodityFlow commodityFlow) {
		return internalComFlowMap.get(commodityFlow);
	}

	private String getInternalComFlowId() {
		currentComFlowCounter++;
		return currentComFlowCounter.toString();
	}

}
