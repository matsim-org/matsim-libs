package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TSPShipment.TimeWindow;


public class TransportChain {
	
	public static abstract class ChainElement{
		
	}
	
	public static class ChainLeg extends ChainElement{
		private Id carrierId;
		
		public ChainLeg(Id carrierId){
			this.carrierId = carrierId;
		}
		
		public Id getCarrierId(){
			return carrierId;
		}
		
		public String toString(){
			return "Chose carrier " + carrierId;
		}
	}
	
	public static abstract class ChainActivity extends ChainElement{
		
		public abstract String getActivityType();
		
		public abstract Id getLocation();
		
		public abstract TimeWindow getTimeWindow();
		
		public abstract TSPShipment getShipment();
		
		public String toString(){
			return getActivityType() + " of " + getShipment() + " at " + getLocation();
		}
	};
	
	public static class PickUp extends ChainActivity{

		private TSPShipment shipment;
		
		private Id location;
		
		private TimeWindow timeWindow;
		
		public PickUp(TSPShipment shipment, Id location, TimeWindow timeWindow) {
			super();
			this.shipment = shipment;
			this.location = location;
			this.timeWindow = timeWindow;
		}

		@Override
		public String getActivityType() {
			return "PickUp";
		}

		@Override
		public Id getLocation() {
			return location;
		}

		@Override
		public TSPShipment getShipment() {
			return shipment;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return timeWindow;
		}
		
		public String toString(){
			return "PickUp at " + location;
		}
	}
	
	public static class Delivery extends ChainActivity{

		private TSPShipment shipment;
		
		private Id location;
		
		private TimeWindow timeWindow;

		public Delivery(TSPShipment shipment, Id location, TimeWindow timeWindow) {
			super();
			this.shipment = shipment;
			this.location = location;
			this.timeWindow = timeWindow;
		}

		@Override
		public String getActivityType() {
			return "Delivery";
		}

		@Override
		public Id getLocation() {
			return location;
		}

		@Override
		public TSPShipment getShipment() {
			return shipment;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return timeWindow;
		}
		
		public String toString(){
			return "Delivery at " + location;
		}
	}
	
	private Collection<ChainElement> chainElements;

	private TSPShipment shipment;
	
	public TransportChain(TSPShipment shipment, Collection<ChainElement> chainElements) {
		this.chainElements = chainElements;
		this.shipment = shipment;
	}

	public Collection<ChainElement> getChainElements() {
		return Collections.unmodifiableCollection(chainElements);
	}
	
	public Collection<ChainLeg> getLegs(){
		Collection<ChainLeg> legs = new ArrayList<ChainLeg>();
		for(ChainElement e : chainElements){
			if(e.getClass().isInstance(ChainLeg.class)){
				legs.add((ChainLeg)e);
			}
		}
		return Collections.unmodifiableCollection(legs);
	}
	
	public Collection<ChainActivity> getActivities(){
		Collection<ChainActivity> activities = new ArrayList<ChainActivity>();
		for(ChainElement e : chainElements){
			if(e.getClass().isInstance(ChainActivity.class)){
				activities.add((ChainActivity)e);
			}
		}
		return Collections.unmodifiableCollection(activities);
	}

	public TSPShipment getShipment() {
		return shipment;
	}
	
	public String toString(){
		String chain = "Chain of shipment(" + getShipment() + "):";
		boolean first = true;
		for(ChainElement ce : getChainElements()){
			if(first){
				chain += ce;
				first = false;
			}
			else{
				chain += ";" + ce;
			}
		}
		return chain;
	}
}
