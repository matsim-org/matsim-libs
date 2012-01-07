package org.matsim.contrib.freight.vrp.basics;

import java.util.HashSet;
import java.util.Set;

public class VrpTourBuilder {
	
	private Tour tour;
	
	private boolean tourStarted = false;
	
	private boolean tourEnded = false;
	
	private Set<Shipment> openShipments = new HashSet<Shipment>();
	
	public VrpTourBuilder() {
		tour = new Tour();
	}
	
	public void scheduleStart(String locationId, double earliestDeparture, double latestDeparture){
		if(tourStarted){
			throw new IllegalStateException("tour has already started");
		}
		Start start = new Start(locationId);
		start.setEarliestArrTime(earliestDeparture);
		start.setLatestArrTime(latestDeparture);
		tourStarted = true;
		tour.getActivities().add(start);
	}
	
	public void scheduleEnd(String locationId, double earliestArrival, double latestArrival){
		if(!tourStarted){
			throw new IllegalStateException("tour must start before end");
		}
		if(openShipments.size() > 0){
			throw new IllegalStateException("there are still open shipments");
		}
		End end = new End(locationId);
		end.setEarliestArrTime(earliestArrival);
		end.setLatestArrTime(latestArrival);
		tour.getActivities().add(end);
		tourEnded = true;
	}

	public void schedulePickup(Shipment shipment){
		if(openShipments.contains(shipment)){
			throw new IllegalStateException("shipment already picked up");
		}
		Pickup pickup = new Pickup(shipment);
		openShipments.add(shipment);
		tour.getActivities().add(pickup);
	}
	
	public void scheduleDelivery(Shipment shipment){
		if(!openShipments.contains(shipment)){
			throw new IllegalStateException("shipment must be picked up first");
		}
		Delivery delivery = new Delivery(shipment);
		openShipments.remove(shipment);
		tour.getActivities().add(delivery);
	}
	
	public void scheduleService(Service service){
		
	}
	
	public void scheduleActivity(TourActivity activity){
		if(activity instanceof Start){
			scheduleStart(activity.getLocationId(), activity.getEarliestArrTime(), activity.getLatestArrTime());
		}
		if(activity instanceof End){
			scheduleEnd(activity.getLocationId(), activity.getEarliestArrTime(), activity.getLatestArrTime());
		}
		if(activity instanceof Pickup){
			schedulePickup((Shipment)((Pickup) activity).getJob());
		}
		if(activity instanceof Delivery){
			scheduleDelivery((Shipment)((Delivery) activity).getJob());
		}
		
	}
	
	public Tour build(){
		if(!tourEnded){
			throw new IllegalStateException("a tour must have an end");
		}
		return tour;
	}
	
}
