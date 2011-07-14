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
package playground.thibautd.jointtripsoptimizer.replanning.reroute;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Module returning instances of {@link JointReRouteAlgo}
 * @author thibautd
 */
public class JointReRouteModule extends AbstractMultithreadedModule {

	private final Controler controler;

	public JointReRouteModule(final Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointReRouteAlgo(controler);
	}
}

