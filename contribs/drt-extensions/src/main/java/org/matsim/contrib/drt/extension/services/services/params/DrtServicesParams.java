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

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Optional;

/**
 * @author steffenaxer
 */
public class DrtServicesParams extends ReflectiveConfigGroup {
	public static final String SET_TYPE = "services";

	public DrtServicesParams()
	{
		super(SET_TYPE);
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		if (type.equals(DrtServiceParams.SET_TYPE))
		{
			return new DrtServiceParams();
		}
		throw new IllegalStateException("Unsupported ConfigGroup "+ type);
	}

	public List<DrtServiceParams> getServices()
	{
		return this.getParameterSets(DrtServiceParams.SET_TYPE).stream().map(s ->(DrtServiceParams) s ).toList();
	}

	public Optional<DrtServiceParams> getService(String serviceName)
	{
		return this.getParameterSets(DrtServiceParams.SET_TYPE).stream().map(s ->(DrtServiceParams) s ).filter(s -> s.name.equals(serviceName)).findFirst();
	}


	@Override
	public void addParameterSet(ConfigGroup configGroup) {
		if (configGroup instanceof DrtServiceParams) {
			super.addParameterSet(configGroup);
		} else {
			throw new IllegalArgumentException("Unsupported ConfigGroup "+ configGroup.getName());
		}
	}
}
