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
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author steffenaxer
 */
public abstract class AbstractServiceTriggerParam extends ReflectiveConfigGroup {

	@NotNull
	@Parameter
	public String name;

	public AbstractServiceTriggerParam(String name)
	{
		super(name);
		this.name = name;
	}

	@Override
	public void addParameterSet(ConfigGroup configGroup) {
		if (configGroup instanceof AbstractServiceTriggerParam) {
			if(!this.getParameterSets().isEmpty())
			{
				throw new IllegalStateException("Adding more than one parameter is not allowed.");
			}
			super.addParameterSet(configGroup);
		} else {
			throw new IllegalArgumentException();
		}
	}

}
