/**
 * 
 */
package freight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainActivity;
import playground.mzilske.freight.TransportChain.ChainElement;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChain.Delivery;
import playground.mzilske.freight.TransportChain.PickUp;
import playground.mzilske.freight.TransportServiceProviderImpl;

/**
 * @author stefan
 *
 */
public class TSPPlanWriter extends MatsimXmlWriter{
	
	private Logger log = Logger.getLogger(TSPPlanWriter.class);
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders;
	
	private Integer shipmentCounter = 0;
	
	private Map<String,TSPShipment> tspShipments;
	

	public TSPPlanWriter(
			Collection<TransportServiceProviderImpl> transportServiceProviders) {
		super();
		this.transportServiceProviders = transportServiceProviders;
	}
	
	public void write(String filename){
		try{
			openFile(filename);
			writeXmlHead();
			startTSPs(writer);
			for(TransportServiceProviderImpl tsp : transportServiceProviders){
				startTSP(tsp,writer);
				if(!tsp.getTspCapabilities().getTransshipmentCentres().isEmpty()){
					startTranshipmentCentres(writer);
					for(Id id : tsp.getTspCapabilities().getTransshipmentCentres()){
						startAndEndTranshipmentCentre(id, writer);
					}
					endTranshipmentCentres(writer);
				}
				if(!tsp.getContracts().isEmpty()){
					startShipments(writer);
					for(TSPContract contract : tsp.getContracts()){
						startAndEndShipment(contract.getShipment(), writer);
					}
					endShipments(writer);
				}
				if(tsp.getSelectedPlan() != null){
					startTransportChains(writer);
					for(TransportChain chain : tsp.getSelectedPlan().getChains()){
						startAndEndChain(chain, writer);
					}
					endTransportChains(writer);
				}
				endTSP(writer);
			}
			endTSPs(writer);
			close();
		}
		catch (IOException e) {
			e.printStackTrace();
			log.error(e);
			System.exit(1);
		}
	}
	
	private void startTSP(TransportServiceProviderImpl tsp,BufferedWriter writer) throws IOException {
		writer.write(tab(2) + "<transportServiceProvider id=" + quotation() + tsp.getId() + quotation() + ">" + newLine());
	}

	private String tab(int nOfTabs) { 
		String tabs = null;
		for(int i=0;i<nOfTabs;i++){
			if(tabs == null){
				tabs = "\t";
			}
			else{
				tabs += "\t";
			}
		}
		return tabs; 
	}
	
	private String quotation(){
		return "\"";
	}

	
	private void startAndEndChain(TransportChain chain, BufferedWriter writer) throws IOException {
		for(ChainElement element : chain.getChainElements()){
			if(element instanceof ChainActivity){
				writer.write(tab(5) + "<act type=" + quotation());
				ChainActivity act = (ChainActivity)element;
				if(element instanceof PickUp){
					writer.write("pickup" + quotation() + " ");
				}
				else if(element instanceof Delivery){
					writer.write("delivery" + quotation() + " ");
				}
				writer.write("linkId=" + quotation() + act.getLocation() + quotation() + " ");
				writer.write("start=" + quotation() + act.getTimeWindow().getStart() + quotation() + " ");
				writer.write("end=" + quotation() + act.getTimeWindow().getEnd() + quotation());
				writer.write("/>" + newLine());
			}
			if(element instanceof ChainLeg){
				ChainLeg leg = (ChainLeg)element;
//				leg.
			}
		}
		
	}

	private void startTransportChains(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "<transportChains>" + newLine());
		
	}

	private void endTransportChains(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "</transportChains>" + newLine());
	}

	private void endShipments(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "</shipments>" + newLine());
		
	}

	private void startShipments(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "<shipments>" + newLine());
		
	}

	private String newLine() {
		return "\n";
	}

	private void startAndEndShipment(TSPShipment shipment, BufferedWriter writer) throws IOException {
		writer.write(tab(4)+"<shipment ");
		writer.write("id=" + quotation() + tspShipments.get(shipment) + quotation() + " ");
		writer.write("from=" + quotation() + shipment.getFrom().toString() + quotation() + " ");
		writer.write("to=" + quotation() + shipment.getTo().toString() + quotation() + " ");
		writer.write("size=" + quotation() + shipment.getSize() + quotation() + "/>" + newLine());
	}

	private void endTranshipmentCentres(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "</transhipmentCentres>" + newLine());
	}

	private void startTranshipmentCentres(BufferedWriter writer) throws IOException {
		writer.write(tab(3) + "<transhipmentCentres>" + newLine());
	}

	private void startAndEndTranshipmentCentre(Id id, BufferedWriter writer) throws IOException {
		writer.write(tab(4)+ "<transhipmentCentre linkId=" + quotation() + id.toString() + quotation() + "/>" + newLine());
	}

	private String makeShipmentId() {
		shipmentCounter++;
		return shipmentCounter.toString();
	}

	private void endTSP(BufferedWriter writer) throws IOException {
		writer.write(tab(2) + "</transportServiceProvider>" + newLine());
		
	}

	private void endTSPs(BufferedWriter writer) throws IOException {
		writer.write(tab(1) + "</transportServiceProviders>" + newLine());
		
	}

	private void startTSPs(BufferedWriter writer) throws IOException {
		writer.write(tab(1) + "<transportServiceProviders>" + newLine());
	}

}
