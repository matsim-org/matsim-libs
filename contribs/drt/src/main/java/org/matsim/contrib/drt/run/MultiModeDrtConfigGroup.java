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
import java.util.function.Supplier;

import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class MultiModeDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<DrtConfigGroup> {
	public static final String GROUP_NAME = "multiModeDrt";

	/**
	 * @param config
	 * @return MultiModeDrtConfigGroup if exists. Otherwise fails
	 */
	public static MultiModeDrtConfigGroup get(Config config) {
		return (MultiModeDrtConfigGroup)config.getModule(GROUP_NAME);
	}

	private final Supplier<DrtConfigGroup> drtConfigSupplier;

	public MultiModeDrtConfigGroup() {
		this(DrtConfigGroup::new);
	}

	public MultiModeDrtConfigGroup(Supplier<DrtConfigGroup> drtConfigSupplier) {
		super(GROUP_NAME);
		this.drtConfigSupplier = drtConfigSupplier;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(config.getModule(DrtConfigGroup.GROUP_NAME) == null,
				"In the multi-mode DRT setup, DrtConfigGroup must not be defined at the config top level");
		MultiModals.requireAllModesUnique(this);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(DrtConfigGroup.GROUP_NAME)) {
			return drtConfigSupplier.get();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	/**
	 * @deprecated -- use typed {@link #addDrtConfigGroup(DrtConfigGroup) instead}
	 */
	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof DrtConfigGroup) {
			super.addParameterSet(set);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
		// the above is a runtime check that allows only a single type.  Would be better to disallow completely and replace by typed adder (see next).  kai, jun'25
	}
	public void addDrtConfigGroup( DrtConfigGroup drtConfigGroup ) {
		addParameterSet( drtConfigGroup );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<DrtConfigGroup> getModalElements() {
		return (Collection<DrtConfigGroup>)getParameterSets(DrtConfigGroup.GROUP_NAME);
	}
}
