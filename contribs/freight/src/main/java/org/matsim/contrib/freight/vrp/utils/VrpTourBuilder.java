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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.TourImpl;

public class VrpTourBuilder {

	private TourImpl tour;

	private boolean tourStarted = false;

	private boolean tourEnded = false;

	private Set<Shipment> openShipments = new HashSet<Shipment>();

	private boolean checkConsistency = true;

	public VrpTourBuilder() {
		tour = new TourImpl();
	}

	public VrpTourBuilder(TourImpl tour) {
		this.tour = tour;
	}

	public Start scheduleStart(String locationId, double earliestDeparture, double latestDeparture) {
		if (tourStarted) {
			throw new IllegalStateException("tour has already started");
		}
		Start start = new Start(locationId, earliestDeparture, latestDeparture);
		tourStarted = true;
		tour.addActivity(start);
		return start;
	}

	public Collection<JobActivity> addShipment(Shipment shipment,int pickupIndex, int deliveryIndex) {
		Pickup pickup = new Pickup(shipment);
		Delivery delivery = new Delivery(shipment);
		tour.addActivity(deliveryIndex, delivery);
		tour.addActivity(pickupIndex, pickup);
		Collection<JobActivity> acts = new ArrayList<JobActivity>();
		acts.add(pickup);
		acts.add(delivery);
		return acts;
	}

	public boolean removeShipment(Shipment shipment) {
		return tour.removeJob(shipment);
	}

	public boolean removeService(Service service) {
		return tour.removeJob(service);
	}

	public JobActivity addService(Service service, int serviceActIndex) {
		Delivery delivery = new Delivery(service);
		tour.addActivity(serviceActIndex, delivery);
		return delivery;
	}

	public End scheduleEnd(String locationId, double earliestArrival, double latestArrival) {
		if (!tourStarted) {
			throw new IllegalStateException("tour must start before end");
		}
		if (openShipments.size() > 0) {
			throw new IllegalStateException("there are still open shipments");
		}
		End end = new End(locationId, earliestArrival, latestArrival);
		tour.addActivity(end);
		tourEnded = true;
		return end;
	}

	public Pickup schedulePickup(Shipment shipment) {
		Pickup pickup = new Pickup(shipment);
		if (checkConsistency) {
			if (openShipments.contains(shipment)) {
				throw new IllegalStateException("shipment already picked up");
			}
			openShipments.add(shipment);
		}
		tour.addActivity(pickup);
		return pickup;
	}

	public Delivery scheduleDelivery(Shipment shipment) {
		Delivery delivery = new Delivery(shipment);
		if (checkConsistency) {
			if (!openShipments.contains(shipment)) {
				throw new IllegalStateException(
						"shipment must be picked up first");
			}
			openShipments.remove(shipment);
		}
		tour.addActivity(delivery);
		return delivery;
	}

	public Pickup schedulePickupService(Service service) {
		return null;
	}

	public Delivery scheduleDeliveryService(Service service) {
		Delivery delivery = new Delivery(service);
		tour.addActivity(delivery);
		return delivery;
	}

	public TourImpl build() {
		if (!tourEnded) {
			throw new IllegalStateException("a tour must have an end");
		}
		return tour;
	}

}
