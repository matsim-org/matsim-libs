/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DReRoutePlanStrategyModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

public class Sim2DReRoutePlanStrategyModule extends AbstractMultithreadedModule {

	private final Controler controller;

	public Sim2DReRoutePlanStrategyModule(Controler controller) {
		super(controller.getConfig().global());
		this.controller = controller;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Sim2DReRoutePlanAlgorithm(this.controller);
	}

}
