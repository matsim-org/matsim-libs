/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.services.params;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author steffenaxer
 */
public class DrtServiceParams extends ReflectiveConfigGroup {

	private static final Map<String, Supplier<ConfigGroup>> SUPPORTED_PARAMS;
	static {
		Map<String, Supplier<ConfigGroup>> map = new HashMap<>();
		map.put(TimeOfDayReachedTriggerParam.SET_NAME, TimeOfDayReachedTriggerParam::new);
		map.put(StopsReachedTriggerParam.SET_NAME, StopsReachedTriggerParam::new);
		map.put(MileageReachedTriggerParam.SET_NAME, MileageReachedTriggerParam::new);
		map.put(ChargingStartedTriggerParam.SET_NAME, ChargingStartedTriggerParam::new);
		SUPPORTED_PARAMS = Collections.unmodifiableMap(map);
	}

	public static final String SET_TYPE = "service";

	@NotNull
	@Parameter
	private String name;

	@NotNull
	@Positive
	@Parameter
	private double duration;

	@Positive
	@Parameter
	private int executionLimit = Integer.MAX_VALUE;

	@Parameter
	private boolean enableTaskStacking = false;

	public DrtServiceParams() {
		this(null);
	}

	public DrtServiceParams(String name)
	{
		super(SET_TYPE);
		this.setName(name);
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		if (SUPPORTED_PARAMS.containsKey(type))
		{
			return SUPPORTED_PARAMS.get(type).get();
		}
		throw new IllegalStateException("Unsupported ConfigGroup "+ type);
	}


	@Override
	public void addParameterSet(ConfigGroup configGroup) {
		if (configGroup instanceof AbstractServiceTriggerParam) {
			super.addParameterSet(configGroup);
		} else {
			throw new IllegalStateException("Unsupported ConfigGroup "+ configGroup.getName());
		}
	}

	public @NotNull String getServiceName() {
		return name;
	}

	public void setName(@NotNull String name) {
		this.name = name;
	}

	@NotNull
	@Positive
	public double getDuration() {
		return duration;
	}

	public void setDuration(@NotNull @Positive double duration) {
		this.duration = duration;
	}

	@Positive
	public int getExecutionLimit() {
		return executionLimit;
	}

	public void setExecutionLimit(@Positive int executionLimit) {
		this.executionLimit = executionLimit;
	}

	public boolean isEnableTaskStacking() {
		return enableTaskStacking;
	}

	public void setEnableTaskStacking(boolean enableTaskStacking) {
		this.enableTaskStacking = enableTaskStacking;
	}
}
