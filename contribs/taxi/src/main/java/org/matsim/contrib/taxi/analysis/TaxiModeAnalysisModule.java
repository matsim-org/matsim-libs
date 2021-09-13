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

/**
 *
 */
package org.matsim.contrib.taxi.analysis;

import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;

/**
 * @author michalm (Michal Maciejewski)
 */
public class TaxiModeAnalysisModule extends AbstractDvrpModeModule {
	private final TaxiConfigGroup taxiCfg;

	public TaxiModeAnalysisModule(TaxiConfigGroup taxiCfg) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
	}

	@Override
	public void install() {
		bindModal(TaxiEventSequenceCollector.class).toProvider(
				modalProvider(getter -> new TaxiEventSequenceCollector(taxiCfg.getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(TaxiEventSequenceCollector.class));

		bindModal(ExecutedScheduleCollector.class).toProvider(
				modalProvider(getter -> ExecutedScheduleCollector.createWithDefaultTaskCreator(taxiCfg.getMode())));
		addEventHandlerBinding().to(modalKey(ExecutedScheduleCollector.class));
	}
}
