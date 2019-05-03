/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.util.Collection;

import javax.validation.Valid;

import org.matsim.contrib.dvrp.run.ConfigConsistencyCheckers;
import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<DrtConfigGroup> {
	public static final String GROUP_NAME = "multiModeDrt";

	@SuppressWarnings("deprecation")
	public static MultiModeDrtConfigGroup get(Config config) {
		return (MultiModeDrtConfigGroup)config.getModule(GROUP_NAME);
	}

	public MultiModeDrtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (!ConfigConsistencyCheckers.areModesUnique(this)) {
			throw new RuntimeException("DRT modes in MultiModeDrtConfigGroup are not unique");
		}
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(DrtConfigGroup.GROUP_NAME)) {
			return new DrtConfigGroup();
		}
		throw new IllegalArgumentException(type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<@Valid DrtConfigGroup> getModalElements() {
		return (Collection<DrtConfigGroup>)getParameterSets(DrtConfigGroup.GROUP_NAME);
	}
}
