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

public final class CarrierService implements CarrierJob {

	public static class Builder {

		private final Id<CarrierService> id;
		private int demand = 0;

		//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
		//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
		//kturner dec'24
		private final Id<Link> serviceLinkId;
		private TimeWindow serviceStartsTimeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private double serviceDuration = 0.0;

		public static Builder newInstance(Id<CarrierService> id, Id<Link> locationLinkId){
			return new Builder(id,locationLinkId);
		}

		private Builder(Id<CarrierService> id, Id<Link> serviceLinkId) {
			super();
			this.id = id;
			this.serviceLinkId = serviceLinkId;
		}

		public CarrierService build(){
			return new CarrierService(this);
		}


		/**
		 * Sets a time-window for the beginning of  the service
		 * When not set, it is by default [0.0., Integer.MAX_VALUE].
		 * <p>
		 * Note that the time-window restricts the start-time of the service (i.e. serviceActivity). If one works with hard time-windows (which means that
		 * time-windows must be met) than the service is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @param startTimeWindow 	time-window for the beginning of the service activity
		 * @return 					the builder
		 */
		public Builder setServiceStartTimeWindow(TimeWindow startTimeWindow){
			this.serviceStartsTimeWindow = startTimeWindow;
			return this;
		}

		/**
		 *  Sets the duration for the pickup activity.
		 *  When not set, it is by default 0.0.
		 *
		 * @param serviceDuration 	duration of the service
		 * @return 					the builder
		 */
		public Builder setServiceDuration(double serviceDuration){
			this.serviceDuration = serviceDuration;
			return this;
		}

		/**
		 * Sets the demand (size; capacity needed) of the service.
		 * When not set, it is by default 0.
		 * <p>
		 * IMO we can put this into the Builder directly instead of a separate method? kturner dec'24
		 *
		 * @param demand the demand (size; capacity needed) of the service
		 * @return the builder
		 */
		public Builder setDemand(int demand) {
			this.demand = demand;
			return this;
		}


		/**
		* Sets the demand (size; capacity needed) of the service.
		 * When not set, it is by default 0.
		 * <p>
		 * IMO we can put this into the Builder directly instead of a separate method? kturner dec'24
		 *
		 * @deprecated please use {@link #setDemand(int)} instead
		 *
		 * @param value the demand (size; capacity needed) of the service
		 * @return the builder
		*/
		@Deprecated(since = "dec'24")
		public Builder setCapacityDemand(int value) {
			return setDemand(value);
		}

	}


	private final Id<CarrierService> id;
	private final int demand;

	//IMO we could build a general class (CarrierActivity ???), containing the location, StartTimeWindow and Duration.
	//This could be used for both, CarrierService and CarrierShipment (Pickup and Delivery).
	//kturner dec'24
	private final Id<Link> serviceLinkId;
	private final TimeWindow serviceStartsTimeWindow;
	private final double serviceDuration;

	private final Attributes attributes = new AttributesImpl();

	private CarrierService(Builder builder){
		id = builder.id;
		serviceLinkId = builder.serviceLinkId;
		serviceDuration = builder.serviceDuration;
		serviceStartsTimeWindow = builder.serviceStartsTimeWindow;
		demand = builder.demand;
	}

	@Override
	public Id<CarrierService> getId() {
		return id;
	}

	public Id<Link> getServiceLinkId() {
		return serviceLinkId;
	}

	/**
	 * @deprecated please inline and use {@link #getServiceLinkId()} instead
	 */
	@Deprecated(since = "dec'24")
	public Id<Link> getLocationLinkId() {
		return getServiceLinkId();
	}

	public double getServiceDuration() {
		return serviceDuration;
	}

	public TimeWindow getServiceStartTimeWindow(){
		return serviceStartsTimeWindow;
	}

	/**
	 * @deprecated please inline and use {@link #getDemand()} instead
	 */
	@Deprecated(since = "dec'24")
	public int getCapacityDemand() {
		return getDemand();
	}

	/**
	 * @return the demand (size; capacity needed) of the service.
	 */
	@Override
	public int getDemand() {
		return demand;
	}


	@Override
	public Attributes getAttributes() {
		return attributes;
	}


	@Override
	public String toString() {
		return "[id=" + id + "][locationId=" + serviceLinkId + "][capacityDemand=" + demand + "][serviceDuration=" + serviceDuration + "][startTimeWindow=" + serviceStartsTimeWindow + "]";
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
		CarrierService other = (CarrierService) obj;
		if (id == null) {
			return other.id == null;
		} else return id.equals(other.id);
	}



}
