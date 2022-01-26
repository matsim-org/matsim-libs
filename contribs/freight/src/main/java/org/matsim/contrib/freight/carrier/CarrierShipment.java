/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A shipment from one location to another, with certain size and other constraints such as time-windows and service-times.
 * 
 * <p>Use the builder to build a shipment. 
 * @code CarrierShipment.Builder.newInstance(from,to,size) 
 * 
 * @author sschroeder
 *
 */
public final class CarrierShipment implements Attributable {

	/**
	 * A builder that builds shipments.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * @Deprecated Please use Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) instead.
		 * 
		 * Returns a new shipment builder.
		 * 
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 * 
		 * @param from
		 * @param to
		 * @param size
		 * @return the builder
		 */
		@Deprecated
		public static Builder newInstance(Id<Link> from, Id<Link> to, int size){
			return new Builder(from,to,size);
		}
		
		/**
		 * Returns a new shipment builder.
		 * 
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 * 
		 * @param id
		 * @param from
		 * @param to
		 * @param size
		 * @return the builder
		 */
		public static Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size){
			return new Builder(id, from,to,size);
		}
		
		Id<CarrierShipment> id;
		Id<Link> from;
		Id<Link> to;
		int size;
		TimeWindow pickTW = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		TimeWindow delTW = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		double pickServiceTime = 0.0;
		double delServiceTime = 0.0;
		
		/**
		 * @Deprecated Please use Builder (Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) instead.
		 */
		@Deprecated 
		public Builder(Id<Link> from, Id<Link> to, int size) {
			super();
			this.from = from;
			this.to = to;
			this.size = size;
		}
		
		public Builder(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) {
			super();
			this.id = id;
			this.from = from;
			this.to = to;
			this.size = size;
		}
		
		public Builder setPickupTimeWindow(TimeWindow pickupTW){
			this.pickTW = pickupTW;
			return this;
		}
		
		public Builder setDeliveryTimeWindow(TimeWindow deliveryTW){
			this.delTW = deliveryTW;
			return this;
		}
		
		public Builder setPickupServiceTime(double pickupServiceTime){
			this.pickServiceTime = pickupServiceTime;
			return this;
		}
		
		public Builder setDeliveryServiceTime(double deliveryServiceTime){
			this.delServiceTime = deliveryServiceTime;
			return this;
		}
		
		public CarrierShipment build(){
			return new CarrierShipment(this);
		}
	}
	
	private final Id<CarrierShipment> id;
	private final Id<Link> from;
	private final Id<Link> to;
	private final int size;
	private final TimeWindow pickupTimeWindow;
	private final TimeWindow deliveryTimeWindow;
	private double pickupServiceTime;
	private double deliveryServiceTime;
	private final Attributes attributes = new Attributes();


	private CarrierShipment(Builder builder) {
		id = builder.id;
		from = builder.from;
		to = builder.to;
		size = builder.size;
		pickupServiceTime = builder.pickServiceTime;
		deliveryServiceTime = builder.delServiceTime;
		pickupTimeWindow = builder.pickTW;
		deliveryTimeWindow = builder.delTW;
	}

	public double getPickupServiceTime() {
		return pickupServiceTime;
	}

	public void setPickupServiceTime(double pickupServiceTime) {
		this.pickupServiceTime = pickupServiceTime;
	}

	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	public void setDeliveryServiceTime(double deliveryServiceTime) {
		this.deliveryServiceTime = deliveryServiceTime;
	}

	public Id<CarrierShipment> getId() {
		return id;
	}
	public Id<Link> getFrom() {
		return from;
	}

	public Id<Link> getTo() {
		return to;
	}

	public int getSize() {
		return size;
	}

	public TimeWindow getPickupTimeWindow() {
		return pickupTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "[id= "+ id.toString() + "][hash=" + this.hashCode() + "][from=" + from.toString() + "][to=" + to.toString() + "][size=" + size + "][pickupServiceTime=" + pickupServiceTime + "]" +
				"[deliveryServiceTime="+deliveryServiceTime+"][pickupTimeWindow="+pickupTimeWindow+"][deliveryTimeWindow="+deliveryTimeWindow+"]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarrierShipment other = (CarrierShipment) obj;
		if (id == null) {
			return other.id == null;
		} else return id.equals(other.id);
	}


}
