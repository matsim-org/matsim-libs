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

package org.matsim.freight.carriers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * A shipment from one location to another, with certain size and other constraints such as time-windows and service-times.
 *
 * <p>Use the builder to build a shipment.
 * @code CarrierShipment.Builder.newInstance(from,to,size)
 *
 * @author sschroeder
 *
 */
public final class CarrierShipment implements CarrierJob {

	/**
	 * A builder that builds shipments.
	 *
	 * @author sschroeder
	 *
	 */
	public static class Builder {

		private final Id<CarrierShipment> id;
		private final int demand;

		private final Id<Link> pickupLinkId;
		private TimeWindow pickupStartsTimeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private double pickupDuration = 0.0;

		private final Id<Link> deliveryLinkId;
		private TimeWindow deliveryStartsTimeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private double deliveryDuration = 0.0;


		/**
		 * @deprecated Please use Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) instead.
		 * <p>
		 * Returns a new shipment builder.
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 *
		 * @param from 	the origin
		 * @param to 	the destination
		 * @param size 	size of the shipment
		 * @return 		the builder
		 */
		@Deprecated
		public static Builder newInstance(Id<Link> from, Id<Link> to, int size){
			var id = Id.create(CarrierConstants.SHIPMENT +"_" + from.toString() + "_" + to.toString(), CarrierShipment.class);
			return new Builder(id, from,to,size);
		}

		/**
		 * Returns a new shipment builder.
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 *
		 * @param id 	the id of the shipment
		 * @param from 	the origin
		 * @param to 	the destination
		 * @param size 	size of the shipment
		 * @return 		the builder
		 */
		public static Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size){
			return new Builder(id, from,to,size);
		}

		/**
		 * @deprecated Please use Builder (Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) instead.
		 */
		@Deprecated
		public Builder(Id<Link> pickupLinkId, Id<Link> deliveryLinkId, int demand) {
			super();
			this.id = Id.create(CarrierConstants.SHIPMENT +"_" + pickupLinkId.toString() + "_" + deliveryLinkId.toString(), CarrierShipment.class);
			this.pickupLinkId = pickupLinkId;
			this.deliveryLinkId = deliveryLinkId;
			this.demand = demand;
		}

		public Builder(Id<CarrierShipment> id, Id<Link> pickupLinkId, Id<Link> deliveryLinkId, int demand) {
			super();
			this.id = id;
			this.pickupLinkId = pickupLinkId;
			this.deliveryLinkId = deliveryLinkId;
			this.demand = demand;
		}

		public Builder setPickupTimeWindow(TimeWindow pickupTW){
			this.pickupStartsTimeWindow = pickupTW;
			return this;
		}

		public Builder setDeliveryTimeWindow(TimeWindow deliveryTW){
			this.deliveryStartsTimeWindow = deliveryTW;
			return this;
		}

		public Builder setPickupServiceTime(double pickupServiceTime){
			this.pickupDuration = pickupServiceTime;
			return this;
		}

		public Builder setDeliveryServiceTime(double deliveryServiceTime){
			this.deliveryDuration = deliveryServiceTime;
			return this;
		}

		public CarrierShipment build(){
			return new CarrierShipment(this);
		}
	}

	private final Id<CarrierShipment> id;
	private final int demand;

	private final Id<Link> pickupLinkId;
	private final TimeWindow pickupStartsTimeWindow;
	private double pickupDuration;

	private final Id<Link> deliveryLinkId;
	private final TimeWindow deliveryStartsTimeWindow;
	private double deliveryDuration;

	private final Attributes attributes = new AttributesImpl();


	private CarrierShipment(Builder builder) {
		id = builder.id;
		pickupLinkId = builder.pickupLinkId;
		deliveryLinkId = builder.deliveryLinkId;
		demand = builder.demand;
		pickupDuration = builder.pickupDuration;
		deliveryDuration = builder.deliveryDuration;
		pickupStartsTimeWindow = builder.pickupStartsTimeWindow;
		deliveryStartsTimeWindow = builder.deliveryStartsTimeWindow;
	}

	public double getPickupServiceTime() {
		return pickupDuration;
	}

	public void setPickupServiceTime(double pickupDuration) {
		this.pickupDuration = pickupDuration;
	}

	public double getDeliveryServiceTime() {
		return deliveryDuration;
	}

	public void setDeliveryServiceTime(double deliveryDuration) {
		this.deliveryDuration = deliveryDuration;
	}

	@Override
	public Id<CarrierShipment> getId() {
		return id;
	}

	public Id<Link> getFrom() {
		return pickupLinkId;
	}

	public Id<Link> getTo() {
		return deliveryLinkId;
	}

	/**
	 * @deprecated please inline and use {@link #getDemand()} instead
	 */
	@Deprecated(since = "dez 2024")
	public int getSize() {
		return getDemand();
	}

	/**
	 * @return the demand (size; capacity needed) of the shipment.
	 */
	@Override
	public int getDemand() {
		return demand;
	}

	public TimeWindow getPickupTimeWindow() {
		return pickupStartsTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryStartsTimeWindow;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "[id= "+ id.toString() + "][hash=" + this.hashCode() + "][from=" + pickupLinkId.toString() + "][to=" + deliveryLinkId.toString() + "][size=" + demand + "][pickupServiceTime=" + pickupDuration + "]" +
				"[deliveryServiceTime="+ deliveryDuration +"][pickupTimeWindow="+ pickupStartsTimeWindow +"][deliveryTimeWindow="+ deliveryStartsTimeWindow +"]";
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
