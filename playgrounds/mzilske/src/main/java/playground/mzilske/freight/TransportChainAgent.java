/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Iterator;
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

	private List<Shipment> shipments = new ArrayList<Shipment>();

	private int currentShipmentIndex;
	
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
	public List<Contract> createCarrierShipments(){
		List<Contract> shipments = new ArrayList<Contract>();
		Id from = null;
		Id to = null;
		TimeWindow pickUpTimeWindow = null;
		TimeWindow deliveryTimeWindow = null;
		Offer acceptedOffer = null;
		
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
				Shipment shipment = createAndRegisterShipment(from,to,tpChain.getShipment().getSize(), pickUpTimeWindow,deliveryTimeWindow);
				shipments.add(new Contract(shipment,acceptedOffer));				
			}
			if(element instanceof ChainLeg){
				ChainLeg leg = (ChainLeg)element;
				acceptedOffer = leg.getAcceptedOffer();
			}
		}
		initIterator();
		return shipments;
	}
		
	private void initIterator() {
		currentShipmentIndex = 0;
	}

	public void informCost(double cost){
		this.cost += cost;
	}
	
	double getCost() {
		return cost;
	}
	
	void reset(){
		cost = 0.0;
	}

	private Shipment createAndRegisterShipment(Id from, Id to, Integer size,
			TimeWindow pickUpTimeWindow, TimeWindow deliveryTimeWindow) {
		Shipment shipment = new Shipment(from,to,size,pickUpTimeWindow,deliveryTimeWindow);
		shipments.add(shipment);
		return shipment;
	}

	public void informPickup(Shipment shipment, double time) {
		if (currentShipmentIndex > shipments.size() - 1) {
			throw new RuntimeException("We have another pickup after our transport chain is already complete.");
		}
		if (shipments.get(currentShipmentIndex) != shipment) {
			throw new RuntimeException("We have a pickup where the preceding delivery is still missing.");
		}
		checkShipmentPickupTime(shipment, time);
	}

	private void checkShipmentPickupTime(Shipment shipment, double time) {
		// TODO Auto-generated method stub
		
	}

	public void informDelivery(Shipment shipment, double time) {
		try{
			if (shipments.get(currentShipmentIndex) != shipment) {
				throw new RuntimeException("We are having a delivery where the preceding delivery is still missing.");
			}
			checkShipmentDeliveryTime(shipment, time);
			currentShipmentIndex++;
		}
		catch(IndexOutOfBoundsException e){
			for(Shipment s : shipments){
				logger.error(s);
			}
			logger.error("Shipment="+shipment+";time="+time+";currentShipmentIndex="+currentShipmentIndex);
			//throw new IndexOutOfBoundsException(e.toString());
			/*
			 * informDelivery is called more than one even if the transportChain consist of one shipment only,
			 * which can just be delivered once
			 * 
			 */
		}
	}

	private void checkShipmentDeliveryTime(Shipment shipment, double time) {
		
	}

	public int getNumberOfStopps() {
		int nOfStopps = getTpChain().getChainTriples().size() - 1;
		return nOfStopps;
	}

	public boolean hasSucceeded() {
		if (currentShipmentIndex == shipments.size()) {
			return true;
		} else {
			return false;
		}
	}
	
	public double getFees(){
		double costs = 0.0;
		for(ChainElement e : tpChain.getChainElements()){
			if(e instanceof ChainLeg){
				costs += ((ChainLeg) e).getAcceptedOffer().getPrice();
			}
		}
		return costs;
	}
	
}
