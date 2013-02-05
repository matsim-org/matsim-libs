/* *********************************************************************** *
 * project: org.matsim.*
 * JointReRouteModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.replanning.modules.reroute;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;

/**
 * Module returning instances of {@link JointReRouteAlgo}
 * @author thibautd
 */
public class JointReRouteModule extends AbstractMultithreadedGenericStrategyModule<JointPlan> {

	private final Controler controler;

	public JointReRouteModule(final Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;
	}

	@Override
	public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
		return new JointReRouteAlgo(controler);
	}
}

