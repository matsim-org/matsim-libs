package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

public class CostAllocator {
	private Logger logger = Logger.getLogger(CostAllocator.class);
	public List<Tuple<Shipment,Double>> allocateCost(List<Shipment> shipments, Double costs){
		List<Tuple<Shipment,Double>> shipmentCostTuples = new ArrayList<Tuple<Shipment,Double>>();
		int totalSize = 0;
		for(Shipment s : shipments){
			totalSize += s.getSize();
		}
		logger.debug("totSize="+totalSize);
		logger.debug("costs="+costs);
		for(Shipment s : shipments){
			double shipmentCost = Math.round((double)s.getSize()/totalSize*costs);
			shipmentCostTuples.add(new Tuple<Shipment,Double>(s,shipmentCost));
		}
		return shipmentCostTuples;
	}

}
