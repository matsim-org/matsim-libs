/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;

/**
 * One time-dependent configuration of a DRT service, i.e. one service time window and the set of stops served within
 * that window. Bookings with a desired departure time outside all configured windows are not served.
 *
 * @author nkuehnel / MOIA
 */
public final class DrtServiceConfigurationParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "serviceConfiguration";

	public static final String START_TIME = "startTime";
	public static final String END_TIME = "endTime";

	@Parameter
	@Comment("Name of this service configuration. Used in logging and error messages. Must be unique.")
	@NotBlank
	private String serviceConfigurationName;

	@Comment("Start of the service time window (inclusive). Undefined means that the window is open at its beginning."
			+ " Times beyond 24h (e.g. '25:00:00') are allowed, there is no wrap-around at midnight.")
	private OptionalTime startTime = OptionalTime.undefined();

	@Comment("End of the service time window (exclusive). Undefined means that the window is open at its end."
			+ " Times beyond 24h (e.g. '25:00:00') are allowed, there is no wrap-around at midnight.")
	private OptionalTime endTime = OptionalTime.undefined();

	@Parameter
	@Comment("Name of the stop network served in this time window, i.e. the value of the 'stopNetworks' attribute of"
			+ " the stop facilities (or links, for serviceAreaBased) belonging to it. Null (default) means that all"
			+ " stops are served. Only meaningful if operationalScheme is not door2door.")
	@Nullable
	private String stopNetwork = null;

	public DrtServiceConfigurationParams() {
		super(SET_NAME);
	}

	public DrtServiceConfigurationParams(String serviceConfigurationName) {
		this();
		this.serviceConfigurationName = serviceConfigurationName;
	}

	public String getServiceConfigurationName() {
		return serviceConfigurationName;
	}

	public void setServiceConfigurationName(String serviceConfigurationName) {
		this.serviceConfigurationName = serviceConfigurationName;
	}

	public OptionalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = OptionalTime.defined(startTime);
	}

	public void setStartTime(OptionalTime startTime) {
		this.startTime = startTime;
	}

	@StringSetter(START_TIME)
	private void setStartTime(String startTime) {
		this.startTime = Time.parseOptionalTime(startTime);
	}

	@StringGetter(START_TIME)
	private String getStartTimeAsString() {
		return Time.writeTime(startTime);
	}

	public OptionalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = OptionalTime.defined(endTime);
	}

	public void setEndTime(OptionalTime endTime) {
		this.endTime = endTime;
	}

	@StringSetter(END_TIME)
	private void setEndTime(String endTime) {
		this.endTime = Time.parseOptionalTime(endTime);
	}

	@StringGetter(END_TIME)
	private String getEndTimeAsString() {
		return Time.writeTime(endTime);
	}

	@Nullable
	public String getStopNetwork() {
		return stopNetwork;
	}

	public void setStopNetwork(@Nullable String stopNetwork) {
		this.stopNetwork = stopNetwork;
	}
}
