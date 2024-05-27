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
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;


public final class CarrierService implements Attributable {

	public static class Builder {

		public static Builder newInstance(Id<CarrierService> id, Id<Link> locationLinkId){
			return new Builder(id,locationLinkId);
		}

		private final Id<CarrierService> id;
		private final Id<Link> locationLinkId;
		private String name = "service";

		private double serviceTime = 0.0;
		private TimeWindow timeWindow = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		private int capacityDemand = 0;

		private Builder(Id<CarrierService> id, Id<Link> locationLinkId) {
			super();
			this.id = id;
			this.locationLinkId = locationLinkId;
		}

		public Builder setName(String name){
			this.name = name;
			return this;
		}

		/**
		 * By default it is [0.0,Integer.MaxValue].
		 *
		 * @param serviceDuration
		 * @return
		 */
		public Builder setServiceDuration(double serviceDuration){
			this.serviceTime = serviceDuration;
			return this;
		}

		/**
		 * Sets a time-window for the service.
		 *
		 * <p>Note that the time-window restricts the start-time of the service (i.e. serviceActivity). If one works with hard time-windows (which means that
		 * time-windows must be met) than the service is allowed to start between startTimeWindow.getStart() and startTimeWindow.getEnd().
		 *
		 * @param startTimeWindow
		 * @return
		 */
		public Builder setServiceStartTimeWindow(TimeWindow startTimeWindow){
			this.timeWindow = startTimeWindow;
			return this;
		}

		public CarrierService build(){
			return new CarrierService(this);
		}

		public Builder setCapacityDemand(int value) {
			this.capacityDemand = value;
			return this;
		}

	}


	private final Id<CarrierService> id;

	private final Id<Link> locationId;

	private final String name;

	private final double serviceDuration;

	private final TimeWindow timeWindow;

	private final int demand;

	private final Attributes attributes = new AttributesImpl();

	private CarrierService(Builder builder){
		id = builder.id;
		locationId = builder.locationLinkId;
		serviceDuration = builder.serviceTime;
		timeWindow = builder.timeWindow;
		demand = builder.capacityDemand;
		name = builder.name;
	}

	public Id<CarrierService> getId() {
		return id;
	}

	public Id<Link> getLocationLinkId() {
		return locationId;
	}

	public double getServiceDuration() {
		return serviceDuration;
	}

	public TimeWindow getServiceStartTimeWindow(){
		return timeWindow;
	}

	public int getCapacityDemand() {
		return demand;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	/**
	 * @return the name
	 */
	public String getType() {
		return name;
	}

	@Override
	public String toString() {
		return "[id=" + id + "][locationId=" + locationId + "][capacityDemand=" + demand + "][serviceDuration=" + serviceDuration + "][startTimeWindow=" + timeWindow + "]";
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
