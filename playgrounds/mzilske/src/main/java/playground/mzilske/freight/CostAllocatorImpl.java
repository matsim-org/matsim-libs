package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

public class CostAllocatorImpl {
	
	private Logger logger = Logger.getLogger(CostAllocatorImpl.class);
	
	private Network network;

	private CarrierImpl carrier;
	
	public CostAllocatorImpl(CarrierImpl carrier, Network network) {
		this.carrier = carrier;
		this.network = network;
	}

	public List<Tuple<Shipment,Double>> allocateCost(List<Shipment> shipments, Double costs){
		List<Tuple<Shipment,Double>> shipmentCostTuples = new ArrayList<Tuple<Shipment,Double>>();
		int totalSize = 0;
		double totalBeeLineDistance = 0.0;
		double totalSizeTimesDistance = 0.0;
		for(Shipment s : shipments){
			totalSize += s.getSize();
			totalBeeLineDistance += beeLineDistance(s) + depotToStart(s) + endToDepot(s);
			totalSizeTimesDistance += s.getSize() * (beeLineDistance(s) + depotToStart(s) + endToDepot(s));
		}
		for(Shipment s : shipments){
			double shipmentCost = (((double) s.getSize() * (beeLineDistance(s)+ depotToStart(s) + endToDepot(s))) / totalSizeTimesDistance)*costs;
			shipmentCostTuples.add(new Tuple<Shipment,Double>(s,shipmentCost));
		}
		return shipmentCostTuples;
	}

	private double endToDepot(Shipment s) {
		Coord from = network.getLinks().get(s.getTo()).getCoord();
		Coord to = network.getLinks().get(carrier.getDepotLinkId()).getCoord();
		double dist = CoordUtils.calcDistance(from, to);
		return dist;
	}

	private double depotToStart(Shipment s) {
		Coord from = network.getLinks().get(carrier.getDepotLinkId()).getCoord();
		Coord to = network.getLinks().get(s.getFrom()).getCoord();
		double dist = CoordUtils.calcDistance(from, to);
		return dist;
	}

	private double beeLineDistance(Shipment s) {
		Coord from = network.getLinks().get(s.getFrom()).getCoord();
		Coord to = network.getLinks().get(s.getTo()).getCoord();
		double dist = CoordUtils.calcDistance(from, to);
		return dist;
	}

}
