/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.routing;
/*
 * created by jbischoff, 20.11.2018
 */

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;

public class MultiModeDrtMainModeIdentifier implements MainModeIdentifier {

	private final MainModeIdentifier delegate = new MainModeIdentifierImpl();
	private final Map<String, String> stageActivityTypeToDrtMode;
	private final Map<String, String> fallbackModeToDrtMode;

	@Inject
	public MultiModeDrtMainModeIdentifier(MultiModeDrtConfigGroup drtCfg) {
		stageActivityTypeToDrtMode = drtCfg.getModalElements()
				.stream()
				.map(DrtConfigGroup::getMode)
				.collect(Collectors.toMap(s -> new DrtStageActivityType(s).drtStageActivity, s -> s));
		fallbackModeToDrtMode = drtCfg.getModalElements()
				.stream()
				.map(DrtConfigGroup::getMode)
				.collect(Collectors.toMap(TripRouter::getFallbackMode, s -> s));
	}

	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		for (PlanElement pe : tripElements) {
			if (pe instanceof Activity) {
				String type = stageActivityTypeToDrtMode.get(((Activity)pe).getType());
				if (type != null) {
					return type;
				}
			} else if (pe instanceof Leg) {
				String mode = fallbackModeToDrtMode.get(((Leg)pe).getMode());
				if (mode != null) {
					return mode;
				}
			}
		}
		return delegate.identifyMainMode(tripElements);
	}
}
