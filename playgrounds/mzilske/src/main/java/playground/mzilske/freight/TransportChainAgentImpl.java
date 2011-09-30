/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TransportChain.ChainElement;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChain.Delivery;
import playground.mzilske.freight.TransportChain.PickUp;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.Shipment;

/**
 * @author stscr
 *
 */
public class TransportChainAgentImpl implements TransportChainAgent {
	private static Logger logger = Logger.getLogger(TransportChainAgentImpl.class);
	
	private double cost = 0.0;

	private TransportChain tpChain;

	private List<CarrierShipment> shipments = new ArrayList<CarrierShipment>();
	
	private List<Contract> contracts = new ArrayList<Contract>();

	private int currentShipmentIndex;
	
	public TransportChainAgentImpl(TransportChain tpChain) {
		this.tpChain = tpChain;
	}
	
	public TransportChain getTransportChain() {
		return tpChain;
	}
	
	public void addCarrierContract(Contract contract){
		contracts.add(contract);
		shipments.add((CarrierShipment)contract.getShipment());
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
	
	public void reset(){
		cost = 0.0;
		initIterator();
	}

	private CarrierShipment createAndRegisterShipment(Id from, Id to, Integer size,TimeWindow pickUpTimeWindow, TimeWindow deliveryTimeWindow) {
		CarrierShipment shipment = new CarrierShipment(from,to,size,pickUpTimeWindow,deliveryTimeWindow);
		shipments.add(shipment);
		return shipment;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportChainAgent#informPickup(playground.mzilske.freight.Shipment, double)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportChainAgent#informDelivery(playground.mzilske.freight.Shipment, double)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportChainAgent#getNumberOfTranshipments()
	 */
	@Override
	public int getNumberOfTranshipments() {
		int nOfStopps = getTransportChain().getChainTriples().size() - 1;
		return nOfStopps;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportChainAgent#hasSucceeded()
	 */
	@Override
	public boolean hasSucceeded() {
		if (currentShipmentIndex == shipments.size()) {
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportChainAgent#getFees()
	 */
	@Override
	public double getFees(){
		double costs = 0.0;
		for(ChainElement e : tpChain.getChainElements()){
			if(e instanceof ChainLeg){
				costs += ((ChainLeg) e).getAcceptedOffer().getPrice();
			}
		}
		return costs;
	}
	
	public Collection<CarrierShipment> getShipments(){
		return shipments;
	}

	public Collection<Contract> getCarrierContracts() {
		return contracts;
	}

	@Override
	public boolean hasShipment(Shipment shipment) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
