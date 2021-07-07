/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteStop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.pt.transitSchedule;

import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Describes the stop within a route of a transit line. Specifies also at
 * what time a headway is expected at the stop as offset from the route start.
 *
 * @author mrieser
 */
public class TransitRouteStopImpl implements TransitRouteStop {

	private TransitStopFacility stop;
	private final OptionalTime departureOffset;
	private final OptionalTime arrivalOffset;
	private boolean awaitDepartureTime = false;

	private TransitRouteStopImpl(Builder builder) {
		stop = builder.stop;
		departureOffset = builder.departureOffset;
		arrivalOffset = builder.arrivalOffset;
		setAwaitDepartureTime(builder.awaitDepartureTime);
	}

	@Override
	public TransitStopFacility getStopFacility() {
		return this.stop;
	}

	@Override
	public void setStopFacility(final TransitStopFacility stopFacility) {
		this.stop = stopFacility;
	}

	@Override
	public OptionalTime getDepartureOffset() {
		return this.departureOffset;
	}

	@Override
	public OptionalTime getArrivalOffset() {
		return this.arrivalOffset;
	}

	@Override
	public boolean isAwaitDepartureTime() {
		return this.awaitDepartureTime;
	}

	@Override
	public void setAwaitDepartureTime(final boolean awaitDepartureTime) {
		this.awaitDepartureTime = awaitDepartureTime;
	}

	/**
	 * TransitRouteStops are typical Value Objects, so we consider two stops equal if they are equal field-wise.
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TransitRouteStopImpl)) {
			return false;
		}
		TransitRouteStopImpl other = (TransitRouteStopImpl) obj;
		if (this.stop == null) {
			if (other.getStopFacility() != null) {
				return false;
			}
		} else {
			if (!stop.equals(other.getStopFacility())) {
				return false;
			}
		}
		if (!this.departureOffset.equals(other.getDepartureOffset())) {
			return false;
		} 
		if (!this.arrivalOffset.equals(other.getArrivalOffset())) {
			return false;
		}
		if (this.awaitDepartureTime != other.isAwaitDepartureTime()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return stop.hashCode();
	}
	
	@Override
	public String toString() {
		return "[TransitRouteStop stop=" + this.stop.getId() + " offset=" + this.departureOffset +" ]";
	}

	public static final class Builder implements TransitRouteStop.Builder<Builder> {
		private TransitStopFacility stop;
		private OptionalTime departureOffset = OptionalTime.undefined();
		private OptionalTime arrivalOffset = OptionalTime.undefined();
		private boolean awaitDepartureTime;

		public Builder() {
		}

		public Builder(TransitRouteStopImpl copy) {
			this.stop = copy.getStopFacility();
			this.departureOffset = copy.getDepartureOffset();
			this.arrivalOffset = copy.getArrivalOffset();
			this.awaitDepartureTime = copy.isAwaitDepartureTime();
		}

		public Builder stop(TransitStopFacility val) {
			stop = val;
			return this;
		}

		public Builder departureOffset(double val) {
			departureOffset = OptionalTime.defined(val);
			return this;
		}

		public Builder arrivalOffset(double val) {
			arrivalOffset = OptionalTime.defined(val);
			return this;
		}

		public Builder departureOffset(OptionalTime val) {
			departureOffset = val;
			return this;
		}

		public Builder arrivalOffset(OptionalTime val) {
			arrivalOffset = val;
			return this;
		}

		public Builder awaitDepartureTime(boolean val) {
			awaitDepartureTime = val;
			return this;
		}

		public TransitRouteStopImpl build() {
			return new TransitRouteStopImpl(this);
		}
	}
}
