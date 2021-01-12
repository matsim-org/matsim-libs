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
public final class MultiModeTaxiConfigGroup extends ReflectiveConfigGroup implements MultiModal<TaxiConfigGroup> {

	public static final String GROUP_NAME = "multiModeTaxi";

	/**
	 * @param config
	 * @return MultiModeTaxiConfigGroup if exists. Otherwise fails
	 */
	public static MultiModeTaxiConfigGroup get(Config config) {
		return (MultiModeTaxiConfigGroup)config.getModule(GROUP_NAME);
	}

	private final Supplier<TaxiConfigGroup> taxiConfigGroupSupplier;

	public MultiModeTaxiConfigGroup() {
		this(TaxiConfigGroup::new);
	}

	public MultiModeTaxiConfigGroup(Supplier<TaxiConfigGroup> taxiConfigGroupSupplier) {
		super(GROUP_NAME);
		this.taxiConfigGroupSupplier = taxiConfigGroupSupplier;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(config.getModule(TaxiConfigGroup.GROUP_NAME) == null,
				"In the multi-mode taxi setup, TaxiConfigGroup must not be defined at the config top level");
		MultiModals.requireAllModesUnique(this);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(TaxiConfigGroup.GROUP_NAME)) {
			return taxiConfigGroupSupplier.get();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof TaxiConfigGroup) {
			super.addParameterSet(set);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<TaxiConfigGroup> getModalElements() {
		return (Collection<TaxiConfigGroup>)getParameterSets(TaxiConfigGroup.GROUP_NAME);
	}
}
