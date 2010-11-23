/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.mzilske.freight.Shipment.TimeWindow;
import playground.mzilske.freight.TransportChain.ChainElement;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChain.Delivery;
import playground.mzilske.freight.TransportChain.PickUp;

/**
 * @author stscr
 *
 */
public class TransportChainAgent {
	private static Logger logger = Logger.getLogger(TransportChainAgent.class);
	
	private double cost = 0.0;

	private TransportChain tpChain;

	public TransportChainAgent(TransportChain tpChain) {
		this.tpChain = tpChain;
	}
	
	TransportChain getTpChain() {
		return tpChain;
	}

	/**
	 * Creates shipments for carriers. The TransportServiceProvider-Shipment is divided into several Carrier-Shipments through the transport-chain. 
	 * A transport-chain is a list of activities (like pickUp and delivery) and legs. For each leg (including the carrierId) and its corresponding 
	 * activities a shipment is created. It is stored in a list with tuples of carrierIds and shipments. 
	 * @return
	 */
	public List<Tuple<Id,Shipment>> createCarrierShipments(){
		List<Tuple<Id,Shipment>> shipments = new ArrayList<Tuple<Id,Shipment>>();
		Id from = null;
		Id to = null;
		TimeWindow pickUpTimeWindow = null;
		TimeWindow deliveryTimeWindow = null;
		Id carrierId = null;
		
		for(ChainElement element : tpChain.getChainElements()){
			if(element instanceof PickUp){
				PickUp pickUp = (PickUp)element;
				from = pickUp.getLocation();
				pickUpTimeWindow = new TimeWindow(pickUp.getTimeWindow().getStart(),
						pickUp.getTimeWindow().getEnd());
			}
			if(element instanceof Delivery){
				Delivery delivery = (Delivery)element;
				to = delivery.getLocation();
				deliveryTimeWindow = new TimeWindow(delivery.getTimeWindow().getStart(),
						delivery.getTimeWindow().getEnd());
				shipments.add(new Tuple<Id,Shipment>(carrierId,createShipment(from,to,tpChain.getShipment().getSize(),
						pickUpTimeWindow,deliveryTimeWindow)));				
			}
			if(element instanceof ChainLeg){
				ChainLeg leg = (ChainLeg)element;
				carrierId = leg.getCarrierId();
			}
		}
		return shipments;
	}
		
	public void informCost(double cost){
		this.cost += cost;
	}
	
	double getCost() {
		return cost;
	}
	
	void reset(){
		logger.info("set cost from " + cost + " to 0");
		cost = 0.0;
	}

	private Shipment createShipment(Id from, Id to, Integer size,
			TimeWindow pickUpTimeWindow, TimeWindow deliveryTimeWindow) {
		return new Shipment(from,to,size,pickUpTimeWindow,deliveryTimeWindow);
	}
}
