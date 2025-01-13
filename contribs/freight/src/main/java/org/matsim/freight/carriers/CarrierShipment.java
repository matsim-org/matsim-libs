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
		private final int capacityDemand;

		//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
		//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
		//kturner dec'24
		private final Id<Link> pickupLinkId;
		private TimeWindow pickupStartingTimeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private double pickupDuration = 0.0;

		//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
		//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
		//kturner dec'24
		private final Id<Link> deliveryLinkId;
		private TimeWindow deliveryStartingTimeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private double deliveryDuration = 0.0;

		/**
		 * Returns a new shipment builder.
		 * <p>
		 * The builder is init with the shipment's origin (from), destination (to) and with the shipment's demand.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 *
		 * @param id 	the id of the shipment
		 * @param from 	the origin
		 * @param to 	the destination
		 * @param capacityDemand 	the demand (size; capacity needed) of the shipment
		 * @return 		the builder
		 */
		public static Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int capacityDemand){
			return new Builder(id, from, to, capacityDemand);
		}

		private Builder(Id<CarrierShipment> id, Id<Link> pickupLinkId, Id<Link> deliveryLinkId, int capacityDemand) {
			super();
			this.id = id;
			this.pickupLinkId = pickupLinkId;
			this.deliveryLinkId = deliveryLinkId;
			this.capacityDemand = capacityDemand;
		}

		/**
		 * Sets a time-window for the beginning of the pickup
		 * When not set, it is by default [0.0., Integer.MAX_VALUE].
		 * <p>
		 * Note that the time-window restricts the start-time of the shipment's pickup . If one works with hard time-windows (which means that
		 * time-windows must be met) than the pickup is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @param pickupStartingTimeWindow 	time-window for the beginning of the pickup activity
		 * @return 							the builder
		 */
		public Builder setPickupStartingTimeWindow(TimeWindow pickupStartingTimeWindow){
			this.pickupStartingTimeWindow = pickupStartingTimeWindow;
			return this;
		}

		/**
		 * Sets a time-window for the beginning of the pickup
		 * When not set, it is by default [0.0., Integer.MAX_VALUE].
		 * <p>
		 * Note that the time-window restricts the start-time of the shipment's pickup . If one works with hard time-windows (which means that
		 * time-windows must be met) than the pickup is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @deprecated since jan'25, use {@link #setPickupStartingTimeWindow(TimeWindow)} instead
		 *
		 * @param pickupStartingTimeWindow 	time-window for the beginning of the pickup activity
		 * @return 							the builder
		 */
		@Deprecated(since = "jan'25")
		public Builder setPickupStartsTimeWindow(TimeWindow pickupStartingTimeWindow){
			return setPickupStartingTimeWindow(pickupStartingTimeWindow);
		}

		/**
		 *  Sets the duration for the pickup activity.
		 *  When not set, it is by default 0.0.
		 *
		 * @param pickupDuration Duration of the pickup activity (in seconds).
		 * @return the Builder
		*/
		public Builder setPickupDuration(double pickupDuration){
			this.pickupDuration = pickupDuration;
			return this;
		}

		/**
		 * Sets a time-window for the beginning of the delivery
		 * When not set, it is by default [0.0., Integer.MAX_VALUE].
		 * <p>
		 * Note that the time-window restricts the start-time of the shipment's delivery . If one works with hard time-windows (which means that
		 * time-windows must be met) than the delivery is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @param deliveryStartingTimeWindow 	time-window for the beginning of the delivery activity
		 * @return 							the builder
		 */
		public Builder setDeliveryStartingTimeWindow(TimeWindow deliveryStartingTimeWindow){
			this.deliveryStartingTimeWindow = deliveryStartingTimeWindow;
			return this;
		}

		/**
		 * Sets a time-window for the beginning of the delivery
		 * When not set, it is by default [0.0., Integer.MAX_VALUE].
		 * <p>
		 * Note that the time-window restricts the start-time of the shipment's delivery . If one works with hard time-windows (which means that
		 * time-windows must be met) than the delivery is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @deprecated since jan'25, use {@link #setDeliveryStartingTimeWindow(TimeWindow)} instead
		 *
		 * @param deliveryStartingTimeWindow 	time-window for the beginning of the delivery activity
		 * @return 							the builder
		 */
		@Deprecated(since = "jan'25")
		public Builder setDeliveryStartsTimeWindow(TimeWindow deliveryStartingTimeWindow){
			return setDeliveryStartingTimeWindow(deliveryStartingTimeWindow);
		}

		/**
		 *  Sets the duration for the delivery activity.
		 *  When not set, it is by default 0.0.
		 *
		 * @param deliveryDuration Duration of the delivery activity (in seconds).
		 * @return the Builder
		 */
		public Builder setDeliveryDuration(double deliveryDuration){
			this.deliveryDuration = deliveryDuration;
			return this;
		}

		public CarrierShipment build(){
			return new CarrierShipment(this);
		}

		//*** deprecated methods ***


		/**
		 * @deprecated please inline and use {@link #setPickupStartingTimeWindow(TimeWindow)} instead
		 */
		@Deprecated(since = "dec'24")
		public Builder setPickupTimeWindow(TimeWindow pickupTW){
			return setPickupStartingTimeWindow(pickupTW);
		}

		/**
		 * @deprecated please inline and use {@link #setPickupDuration(double)} instead
		 */
		@Deprecated(since = "dec'24")
		public Builder setPickupServiceTime(double pickupServiceTime){
			return setPickupDuration(pickupServiceTime);
		}

		/**
		 * @deprecated please inline and use {@link #setDeliveryStartingTimeWindow(TimeWindow)} instead
		 */
		@Deprecated(since = "dec'24")
		public Builder setDeliveryTimeWindow(TimeWindow deliveryTW){
			return setDeliveryStartingTimeWindow(deliveryTW);
		}

		/**
		 * @deprecated please inline and use {@link #setDeliveryDuration(double)} instead
		 */
		@Deprecated(since = "dec'24")
		public Builder setDeliveryServiceTime(double deliveryServiceTime){
			return setDeliveryDuration(deliveryServiceTime);
		}
	}

	private final Id<CarrierShipment> id;
	private final int capacityDemand;

	//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
	//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
	//kturner dec'24
	private final Id<Link> pickupLinkId;
	private final TimeWindow pickupStartingTimeWindow;
	private double pickupDuration;

	//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
	//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
	//kturner dec'24
	private final Id<Link> deliveryLinkId;
	private final TimeWindow deliveryStartingTimeWindow;
	private double deliveryDuration;

	private final Attributes attributes = new AttributesImpl();


	private CarrierShipment(Builder builder) {
		id = builder.id;
		pickupLinkId = builder.pickupLinkId;
		deliveryLinkId = builder.deliveryLinkId;
		capacityDemand = builder.capacityDemand;
		pickupDuration = builder.pickupDuration;
		deliveryDuration = builder.deliveryDuration;
		pickupStartingTimeWindow = builder.pickupStartingTimeWindow;
		deliveryStartingTimeWindow = builder.deliveryStartingTimeWindow;
	}

	//* getters and setters

	public double getPickupDuration() {
		return pickupDuration;
	}

	public double getDeliveryDuration() {
		return deliveryDuration;
	}

	/**
	 * Do we really need the setter? We do have it in the builder.
	 * I do not see, why we should be able to update it, since most of the values are immutable.
	 * @deprecated Consider setting it using the Builder. This will maybe be removed and the field gets immutable.
	 * kturner, dec'24
	 */
	@Deprecated(since = "dec'24")
	public void setPickupDuration(double pickupDuration) {
		this.pickupDuration = pickupDuration;
	}

	/**
	* Do we really need the setter? We do have it in the builder.
	* I do not see, why we should be able to update it, since most of the values are immutable.
	* @deprecated Consider setting it using the Builder. This will maybe be removed and the field gets immutable.
	* kturner, dec'24
	*/
	@Deprecated(since = "dec'24")
	public void setDeliveryDuration(double deliveryDuration) {
		this.deliveryDuration = deliveryDuration;
	}

	@Override
	public Id<CarrierShipment> getId() {
		return id;
	}

	public Id<Link> getPickupLinkId() {
		return pickupLinkId;
	}

	public Id<Link> getDeliveryLinkId() {
		return deliveryLinkId;
	}

	/**
	 * @return the demand (size; capacity needed) of the shipment.
	 */
	@Override
	public int getCapacityDemand() {
		return capacityDemand;
	}

	public TimeWindow getPickupStartingTimeWindow() {
		return pickupStartingTimeWindow;
	}

	/**
	 * @deprecated since jan'25, use {@link #getPickupStartingTimeWindow()} instead
	 */
	@Deprecated(since = "jan'25")
	public TimeWindow getPickupStartsTimeWindow() {
		return getPickupStartingTimeWindow();
	}

	public TimeWindow getDeliveryStartingTimeWindow() {
		return deliveryStartingTimeWindow;
	}

	/**
	 * @deprecated since jan'25, use {@link #getDeliveryStartingTimeWindow()} instead
	 */
	@Deprecated(since = "jan'25")
	public TimeWindow getDeliveryStartsTimeWindow() {
		return getDeliveryStartingTimeWindow();
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	//*** deprecated methods ***

	/**
	 * @deprecated please inline and use {@link #getCapacityDemand()} instead
	 */
	@Deprecated(since = "dec'24")
	public int getSize() {
		return getCapacityDemand();
	}


	/**
	 * @deprecated please inline and use {@link #getPickupLinkId()} instead
	 */
	@Deprecated(since = "dec'24")
	public Id<Link> getFrom() {
		return getPickupLinkId();
	}

	/**
	 * @deprecated please inline and use {@link #getPickupStartingTimeWindow()} instead
	 */
	@Deprecated(since = "dec'24")
	public TimeWindow getPickupTimeWindow() {
		return getPickupStartingTimeWindow();
	}

	/**
	 * @deprecated please inline and use {@link #setPickupDuration(double)} instead
	 */
	@Deprecated(since = "dec'24")
	public void setPickupServiceTime(double pickupDuration) {
		setPickupDuration(pickupDuration);
	}

	/**
	 * @deprecated please inline and use {@link #getPickupDuration()} instead
	 */
	@Deprecated(since = "dec'24")
	public double getPickupServiceTime() {
		return getPickupDuration();
	}


	/**
	 * @deprecated please inline and use {@link #getDeliveryLinkId()} instead
	 */
	@Deprecated(since = "dec'24")
	public Id<Link> getTo() {
		return getDeliveryLinkId();
	}

	/**
	 * @deprecated please inline and use {@link #getDeliveryStartingTimeWindow()} instead
	 */
	@Deprecated(since = "dec'24")
	public TimeWindow getDeliveryTimeWindow() {
		return getDeliveryStartingTimeWindow();
	}

	/**
	 * @deprecated please inline and use {@link #getDeliveryDuration()} instead
	 */
	@Deprecated(since = "dec'24")
	public double getDeliveryServiceTime() {
		return getDeliveryDuration();
	}

	/**
	 * @deprecated please inline and use {@link #setDeliveryDuration(double)} instead
	 */
	@Deprecated(since = "dec'24")
	public void setDeliveryServiceTime(double deliveryDuration) {
		setDeliveryDuration(deliveryDuration);
	}

	// *** general methods ***

	@Override
	public String toString() {
		return "[id= "+ id.toString() + "][hash=" + this.hashCode() + "][pickupLinkId=" + pickupLinkId.toString() + "][deliveryLinkId=" + deliveryLinkId.toString() + "][capacityDemand=" + capacityDemand + "][pickupDuration=" + pickupDuration + "]" +
				"[deliveryDuration="+ deliveryDuration +"][pickupStartingTimeWindow="+ pickupStartingTimeWindow +"][deliveryStartingTimeWindow="+ deliveryStartingTimeWindow +"]";
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
