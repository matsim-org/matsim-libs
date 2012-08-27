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
package org.matsim.contrib.freight.vrp.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;

public class VrpTourBuilder {
	
	private TourImpl tour;
	
	private boolean tourStarted = false;
	
	private boolean tourEnded = false;
	
	private Set<Shipment> openShipments = new HashSet<Shipment>();
	
	private boolean checkConsistency = true;
	
	public VrpTourBuilder() {
		tour = new TourImpl();
	}
	
	public VrpTourBuilder(boolean checkConsistency){
		this.checkConsistency = checkConsistency;
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
		Pickup pickup = new Pickup(shipment);
		if(checkConsistency){
			if(openShipments.contains(shipment)){
				throw new IllegalStateException("shipment already picked up");
			}
			openShipments.add(shipment);
		}
		tour.getActivities().add(pickup);
		return pickup;
	}
	
	public Delivery scheduleDelivery(Shipment shipment){
		Delivery delivery = new Delivery(shipment);
		if(checkConsistency){
			if(!openShipments.contains(shipment)){
				throw new IllegalStateException("shipment must be picked up first");
			}
			openShipments.remove(shipment);
		}
		tour.getActivities().add(delivery);
		return delivery;
	}
	
	public Pickup schedulePickupService(Service service){
		return null;
	}
	
	public Delivery scheduleDeliveryService(Service service){
		Delivery delivery = new Delivery(service);
		tour.getActivities().add(delivery);
		return delivery;
	}
	
	public TourImpl build(){
		if(!tourEnded){
			throw new IllegalStateException("a tour must have an end");
		}
		return tour;
	}
	
}
