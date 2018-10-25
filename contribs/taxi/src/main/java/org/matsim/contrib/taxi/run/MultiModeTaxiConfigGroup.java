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

package org.matsim.contrib.taxi.run;

import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeTaxiConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "multiModeTaxi";

	@SuppressWarnings("deprecation")
	public static MultiModeTaxiConfigGroup get(Config config) {
		return (MultiModeTaxiConfigGroup)config.getModule(GROUP_NAME);
	}

	public MultiModeTaxiConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(TaxiConfigGroup.GROUP_NAME)) {
			return new TaxiConfigGroup();
		}
		throw new IllegalArgumentException(type);
	}

	@SuppressWarnings("unchecked")
	public Collection<TaxiConfigGroup> getTaxiConfigGroups() {
		return (Collection<TaxiConfigGroup>)getParameterSets(TaxiConfigGroup.GROUP_NAME);
	}
}
