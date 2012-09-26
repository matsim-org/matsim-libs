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

public class Shipment implements Job {

	private String fromId;

	private String toId;

	private int size;

	private double pickupServiceTime;

	private double deliveryServiceTime;

	private TimeWindow pickupTW;

	private TimeWindow deliveryTW;

	private String id;

	public Shipment(String id, String fromId, String toId, int size) {
		super();
		this.fromId = fromId;
		this.toId = toId;
		this.size = size;
		this.id = id;
	}

	public TimeWindow getPickupTW() {
		return pickupTW;
	}

	public void setPickupTW(TimeWindow pickupTW) {
		this.pickupTW = pickupTW;
	}

	public TimeWindow getDeliveryTW() {
		return deliveryTW;
	}

	public void setDeliveryServiceTime(double deliveryServiceTime) {
		this.deliveryServiceTime = deliveryServiceTime;
	}

	public void setDeliveryTW(TimeWindow deliveryTW) {
		this.deliveryTW = deliveryTW;
	}

	public String getFromId() {
		return fromId;
	}

	public void setPickupServiceTime(double pickupServiceTime) {
		this.pickupServiceTime = pickupServiceTime;
	}

	public double getPickupServiceTime() {
		return pickupServiceTime;
	}

	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	public String getToId() {
		return toId;
	}

	@Override
	public int getCapacityDemand() {
		return size;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "[id=" + id + "][from=" + fromId + "][to=" + toId + "[size="
				+ size + "]";
	}

}
