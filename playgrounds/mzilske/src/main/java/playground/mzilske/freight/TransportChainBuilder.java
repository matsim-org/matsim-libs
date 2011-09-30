package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TransportChain.ChainElement;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChain.Delivery;
import playground.mzilske.freight.TransportChain.PickUp;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierOffer;


public class TransportChainBuilder {
	
	private static Logger logger = Logger.getLogger(TransportChainBuilder.class);
	
	private Collection<ChainElement> chainElements = new ArrayList<ChainElement>();
	
	private TSPShipment shipment;
	
	private boolean firstPickUp = true;
	
	private Delivery lastDelivery = null;
	
	private boolean openPickUp = false;
	
	private boolean openLeg = false;
	
	public TransportChainBuilder(TSPShipment shipment) {
		this.shipment = shipment;
		if(shipment == null){
			throw new IllegalStateException("tspShipment cannot be null.");
		}
	}
	
	public void scheduleLeg(CarrierContract contract){
		if(!openPickUp){
			throw new IllegalStateException("No shipment has been picked up. Thus, cannot create a leg.");
		}
		if(openLeg){
			throw new IllegalStateException("Cannot create a leg, because a leg is still open");
		}
		ChainLeg leg = new ChainLeg(contract);
		chainElements.add(leg);
		openLeg = true;
	}

	public void scheduleLeg(CarrierOffer offer){
		if(!openPickUp){
			throw new IllegalStateException("No shipment has been picked up. Thus, cannot create a leg.");
		}
		if(openLeg){
			throw new IllegalStateException("Cannot create a leg, because a leg is still open");
		}
		ChainLeg leg = createLeg(offer);
		chainElements.add(leg);
		openLeg = true;
	}
	
	public void scheduleLeg(CarrierOffer offer, double duration){
		if(!openPickUp){
			throw new IllegalStateException("No shipment has been picked up. Thus, cannot create a leg.");
		}
		if(openLeg){
			throw new IllegalStateException("Cannot create a leg, because a leg is still open");
		}
		ChainLeg leg = createLeg(offer);
		leg.setDuration(duration);
		chainElements.add(leg);
		openLeg = true;
	}
	
	public void schedulePickup(Id location, TimeWindow timeWindow){
		if(firstPickUp){
			if(!location.equals(shipment.getFrom())){
				throw new IllegalStateException("First pick up must be startLocation of transport chain.");
			}
			firstPickUp = false;
		}
		if(openPickUp){
			throw new IllegalStateException("A pickUp is still open.");
		}
		if(lastDelivery != null){
			if(!location.equals(lastDelivery.getLocation())){
				throw new IllegalStateException("Pick up must be at last delivery location");
			}
		}
		PickUp pickUp = createPickup(shipment, location, timeWindow);
		chainElements.add(pickUp);
		openPickUp = true;
	}

	public void scheduleDelivery(Id location, TimeWindow timeWindow){
		if(!openPickUp){
			throw new IllegalStateException("Shipment has no pickUp schedule yet");
		}
		if(!openLeg){
			throw new IllegalStateException("Missing leg.");
		}
		Delivery delivery = createDelivery(shipment,location,timeWindow);
		chainElements.add(delivery);
		openPickUp = false;
		openLeg = false;
		lastDelivery = delivery;		
	}
	
	public TransportChain build(){
		if(openPickUp){
			throw new IllegalStateException("A pickUp is still open.");
		}
		if(!lastDelivery.getLocation().equals(lastDelivery.getShipment().getTo())){
			throw new IllegalStateException("Final delivery must be at destination of shipment.");
		}
		return new TransportChain(shipment,chainElements);
	}

	private ChainLeg createLeg(CarrierOffer offer) {
		return new ChainLeg(offer);
	}

	private PickUp createPickup(TSPShipment shipment, Id location, TimeWindow timeWindow) {
		return new PickUp(shipment,location,timeWindow);
	
	}

	private Delivery createDelivery(TSPShipment shipment, Id location, TimeWindow timeWindow) {
		return new Delivery(shipment,location,timeWindow);
	}

	public void schedule(ChainElement chainElement) {
		if (chainElement instanceof PickUp) {
			PickUp pickup = (PickUp) chainElement;
			schedulePickup(pickup.getLocation(), pickup.getTimeWindow());
		} else if (chainElement instanceof Delivery) {
			Delivery delivery = (Delivery) chainElement;
			scheduleDelivery(delivery.getLocation(), delivery.getTimeWindow());
		} else if (chainElement instanceof ChainLeg) {
			ChainLeg chainLeg = (ChainLeg) chainElement;
			scheduleLeg(chainLeg.getAcceptedOffer(), chainLeg.getDuration());
		}
	}
	
}
