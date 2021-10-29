/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpRoutingModuleProvider extends ModalProviders.AbstractProvider<DvrpMode, DvrpRoutingModule> {
	public enum Stage {ACCESS, MAIN, EGRESS}

	@Inject
	@Named(TransportMode.walk)
	private RoutingModule walkRouter;

	@Inject
	private TimeInterpretation timeInterpretation;

	public DvrpRoutingModuleProvider(String mode) {
		super(mode, DvrpModes::mode);
	}

	@Override
	public DvrpRoutingModule get() {
		Map<Stage, RoutingModule> stageRouters = getModalInstance(new TypeLiteral<Map<Stage, RoutingModule>>() {
		});
		RoutingModule mainRouter = Objects.requireNonNull(stageRouters.get(Stage.MAIN),
				"Main mode router must be explicitly bound");
		RoutingModule accessRouter = stageRouters.getOrDefault(Stage.ACCESS, walkRouter);
		RoutingModule egressRouter = stageRouters.getOrDefault(Stage.EGRESS, walkRouter);

		return new DvrpRoutingModule(mainRouter, accessRouter, egressRouter,
				getModalInstance(DvrpRoutingModule.AccessEgressFacilityFinder.class), getMode(), timeInterpretation);
	}
}
