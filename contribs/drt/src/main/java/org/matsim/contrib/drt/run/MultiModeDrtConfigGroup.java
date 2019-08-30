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

import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class MultiModeDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<DrtConfigGroup> {
	public static final String GROUP_NAME = "multiModeDrt";

	@SuppressWarnings("deprecation")
	public static MultiModeDrtConfigGroup get(Config config) {
//		return (MultiModeDrtConfigGroup)config.getModule(GROUP_NAME);
		return ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class ) ;
		// yyyy I think that this method should be inlined and then removed to become consistent with how it is done elsewhere.  kai, aug'19
	}

	public MultiModeDrtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(DrtConfigGroup.get(config) == null,
				"In the multi-mode DRT setup, DrtConfigGroup must not be defined at the config top level");
		MultiModals.requireAllModesUnique(this);
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
