/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.events;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * Event for currents train position.
 */
public final class RailsimTrainStateEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimTrainStateEvent";

	public static final String ATTRIBUTE_EXACT_TIME = "exactTime";
	public static final String ATTRIBUTE_HEAD_LINK = "headLink";
	public static final String ATTRIBUTE_HEAD_POSITION = "headPosition";
	public static final String ATTRIBUTE_TAIL_LINK = "tailLink";
	public static final String ATTRIBUTE_TAIL_POSITION = "tailPosition";
	public static final String ATTRIBUTE_SPEED = "speed";
	public static final String ATTRIBUTE_ACCELERATION = "acceleration";
	public static final String ATTRIBUTE_TARGET_SPEED = "targetSpeed";

	/**
	 * Exact time with resolution of 0.001s.
	 */
	private final double exactTime;
	private final Id<Vehicle> vehicleId;
	private final Id<Link> headLink;
	private final double headPosition;
	private final Id<Link> tailLink;
	private final double tailPosition;
	private final double speed;
	private final double acceleration;
	private final double targetSpeed;

	public RailsimTrainStateEvent(double time, double exactTime, Id<Vehicle> vehicleId,
								  Id<Link> headLink, double headPosition,
								  Id<Link> tailLink, double tailPosition,
								  double speed, double acceleration, double targetSpeed) {
		super(time);
		this.exactTime = RailsimUtils.round(exactTime);
		this.vehicleId = vehicleId;
		this.headLink = headLink;
		this.headPosition = headPosition;
		this.tailLink = tailLink;
		this.tailPosition = tailPosition;
		this.speed = speed;
		this.acceleration = acceleration;
		this.targetSpeed = targetSpeed;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public double getExactTime() {
		return exactTime;
	}

	public double getHeadPosition() {
		return headPosition;
	}

	public double getTailPosition() {
		return tailPosition;
	}

	public double getSpeed() {
		return speed;
	}

	public double getTargetSpeed() {
		return this.targetSpeed;
	}

	public double getAcceleration() {
		return this.acceleration;
	}

	public Id<Link> getHeadLink() {
		return this.headLink;
	}

	public Id<Link> getTailLink() {
		return this.tailLink;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_EXACT_TIME, String.valueOf(exactTime));
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_HEAD_LINK, String.valueOf(headLink));
		attr.put(ATTRIBUTE_HEAD_POSITION, Double.toString(headPosition));
		attr.put(ATTRIBUTE_TAIL_LINK, String.valueOf(tailLink));
		attr.put(ATTRIBUTE_TAIL_POSITION, Double.toString(tailPosition));
		attr.put(ATTRIBUTE_SPEED, Double.toString(speed));
		attr.put(ATTRIBUTE_ACCELERATION, Double.toString(acceleration));
		attr.put(ATTRIBUTE_TARGET_SPEED, Double.toString(targetSpeed));
		return attr;
	}
}
