/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
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
	
	public Start scheduleStart(String locationId, double earliestDeparture, double latestDeparture){
		if(tourStarted){
			throw new IllegalStateException("tour has already started");
		}
		Start start = new Start(locationId);
		start.setEarliestOperationStartTime(earliestDeparture);
		start.setLatestOperationStartTime(latestDeparture);
		tourStarted = true;
		tour.getActivities().add(start);
		return start;
	}
	
	public End scheduleEnd(String locationId, double earliestArrival, double latestArrival){
		if(!tourStarted){
			throw new IllegalStateException("tour must start before end");
		}
		if(openShipments.size() > 0){
			throw new IllegalStateException("there are still open shipments");
		}
		End end = new End(locationId);
		end.setEarliestOperationStartTime(earliestArrival);
		end.setLatestOperationStartTime(latestArrival);
		tour.getActivities().add(end);
		tourEnded = true;
		return end;
	}

	public Pickup schedulePickup(Shipment shipment){
		if(openShipments.contains(shipment)){
			throw new IllegalStateException("shipment already picked up");
		}
		Pickup pickup = new Pickup(shipment);
		openShipments.add(shipment);
		tour.getActivities().add(pickup);
		return pickup;
	}
	
	public Delivery scheduleDelivery(Shipment shipment){
		if(!openShipments.contains(shipment)){
			throw new IllegalStateException("shipment must be picked up first");
		}
		Delivery delivery = new Delivery(shipment);
		openShipments.remove(shipment);
		tour.getActivities().add(delivery);
		return delivery;
	}
	
	public void scheduleService(Service service){
		
	}
	
	public TourActivity copyAndScheduleActivity(TourActivity activity){
		TourActivity ta = null;
		if(activity instanceof Start){
			ta = scheduleStart(activity.getLocationId(), activity.getEarliestOperationStartTime(), activity.getLatestOperationStartTime());
		}
		else if(activity instanceof End){
			ta = scheduleEnd(activity.getLocationId(), activity.getEarliestOperationStartTime(), activity.getLatestOperationStartTime());
		}
		else if(activity instanceof Pickup){
			ta = schedulePickup((Shipment)((Pickup) activity).getJob());
		}
		else if(activity instanceof Delivery){
			ta = scheduleDelivery((Shipment)((Delivery) activity).getJob());
		}
		else{
			throw new IllegalStateException("does not support tourActivity " + activity.toString());
		}
		return ta;
		
	}
	
	public Tour build(){
		if(!tourEnded){
			throw new IllegalStateException("a tour must have an end");
		}
		return tour;
	}
	
}
