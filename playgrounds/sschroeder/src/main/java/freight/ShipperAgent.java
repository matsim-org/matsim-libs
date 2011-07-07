package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPShipment;

/**
 * 
 * @author stefan
 *
 */

public class ShipperAgent {
	
	private ShipperImpl shipper;

	public ShipperAgent(ShipperImpl shipper) {
		super();
		this.shipper = shipper;
	}

	public List<TSPContract> createTSPContracts() {
		List<TSPContract> contracts = new ArrayList<TSPContract>();
		for(ScheduledCommodityFlow sComFlow : shipper.getSelectedPlan().getScheduledFlows()){
			Collection<TSPShipment> tspShipments = new ArrayList<TSPShipment>();
			for(ShipperShipment s : sComFlow.getShipments()){
				TSPShipment shipment = TSPUtils.createTSPShipment(s.getFrom(), s.getTo(), s.getSize(), 
						s.getPickTimeWindow().getStart(), s.getPickTimeWindow().getEnd(), s.getDeliveryTimeWindow().getStart(), 
						s.getDeliveryTimeWindow().getEnd());
				tspShipments.add(shipment);
				TSPContract tspContract = TSPUtils.createTSPContract(shipment,sComFlow.getTspOffer());
				contracts.add(tspContract);
			}
		}
		return contracts;
	}
}
