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

package org.matsim.contrib.drt.extension.estimator.run;

import java.util.Collection;
import java.util.Optional;
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
public final class MultiModeDrtEstimatorConfigGroup extends ReflectiveConfigGroup implements MultiModal<DrtEstimatorConfigGroup> {
	public static final String GROUP_NAME = "drtEstimators";

	/**
	 * @param config
	 * @return MultiModeDrtConfigGroup if exists. Otherwise fails
	 */
	public static MultiModeDrtEstimatorConfigGroup get(Config config) {
		return (MultiModeDrtEstimatorConfigGroup)config.getModule(GROUP_NAME);
	}

	private final Supplier<DrtEstimatorConfigGroup> drtConfigSupplier;

	public MultiModeDrtEstimatorConfigGroup() {
		this(DrtEstimatorConfigGroup::new);
	}

	public MultiModeDrtEstimatorConfigGroup(Supplier<DrtEstimatorConfigGroup> drtConfigSupplier) {
		super(GROUP_NAME);
		this.drtConfigSupplier = drtConfigSupplier;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(config.getModule(DrtEstimatorConfigGroup.GROUP_NAME) == null,
				"In the multi-mode DRT setup, DrtEstimatorConfigGroup must not be defined at the config top level");
		MultiModals.requireAllModesUnique(this);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(DrtEstimatorConfigGroup.GROUP_NAME)) {
			return drtConfigSupplier.get();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof DrtEstimatorConfigGroup) {
			super.addParameterSet(set);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	public void addParameterSet(DrtEstimatorConfigGroup set) {
		addParameterSet((ConfigGroup) set);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<DrtEstimatorConfigGroup> getModalElements() {
		return (Collection<DrtEstimatorConfigGroup>)getParameterSets(DrtEstimatorConfigGroup.GROUP_NAME);
	}

	/**
	 * Find estimator config for specific mode.
	 */
	public Optional<DrtEstimatorConfigGroup> getModalElement(String mode) {
		return getModalElements().stream().filter(m -> m.getMode().equals(mode)).findFirst();
	}

}
