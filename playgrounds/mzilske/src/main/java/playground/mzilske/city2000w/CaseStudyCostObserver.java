package playground.mzilske.city2000w;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.CarrierCostListener;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TSPCostListener;
import playground.mzilske.freight.TSPShipment;

public class CaseStudyCostObserver implements CarrierCostListener, TSPCostListener {

	private List<Tuple<TSPShipment,Double>> tspShipments = new ArrayList<Tuple<TSPShipment,Double>>();
	
	private List<Tuple<Shipment,Double>> shipments = new ArrayList<Tuple<Shipment,Double>>();
	
	private List<List<Tuple<TSPShipment,Double>>> tspShipmentCostCollector = new ArrayList<List<Tuple<TSPShipment,Double>>>();
	
	private List<List<Tuple<Shipment,Double>>> shipmentCostCollector = new ArrayList<List<Tuple<Shipment,Double>>>();
	
	private String outFile;
	
	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	@Override
	public void informCost(TSPShipment shipment, Double cost) {
		tspShipments.add(new Tuple<TSPShipment,Double>(shipment,cost));
	}

	@Override
	public void informCost(Shipment shipment, Double cost) {
		shipments.add(new Tuple<Shipment,Double>(shipment,cost));
	}
	
	public void reset(){
		List<Tuple<TSPShipment,Double>> copiedTspShipments = new ArrayList<Tuple<TSPShipment,Double>>();
		for(Tuple<TSPShipment,Double> t : tspShipments){
			copiedTspShipments.add(t);
		}
		tspShipmentCostCollector.add(copiedTspShipments);
		
		List<Tuple<Shipment,Double>> copiedShipments = new ArrayList<Tuple<Shipment,Double>>();
		for(Tuple<Shipment,Double> t : shipments){
			copiedShipments.add(t);
		}
		shipmentCostCollector.add(copiedShipments);
		tspShipments.clear();
		shipments.clear();
	}
	
	public void writeStats() throws FileNotFoundException, IOException{
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		for(int i=50;i<tspShipmentCostCollector.size();i++){
			List<Tuple<TSPShipment,Double>> tspShipments = tspShipmentCostCollector.get(i);
			List<Tuple<Shipment,Double>> shipments = shipmentCostCollector.get(i);
			for(Tuple<TSPShipment,Double> t : tspShipments){
				writer.write(i+";TSPShipment;"+t.getFirst().getFrom() + ";" + t.getFirst().getTo() + ";" + t.getSecond());
				writer.write("\n");
			}
			for(Tuple<Shipment,Double> t : shipments){
				writer.write(i+";Shipment;"+t.getFirst().getFrom() + ";" + t.getFirst().getTo() + ";" + t.getSecond());
				writer.write("\n");
			}
		}
		writer.close();
	}

}
