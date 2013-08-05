/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.ptSubModes.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author droeder
 *
 */
class ReRoutePtSubModeStrategy extends AbstractMultithreadedModule{
	
	private ReplanningContext rc;

	/**
	 * <code>PlanStrategyModule</code> which reroutes pt-legs and stores pt-submodes.
	 * Aborts if the controler is not an instance of instance of <code>PtSubModeControler</code>
	 * @param sc
	 */
	protected ReRoutePtSubModeStrategy(Scenario sc, ReplanningContext rc) {
		super(sc.getConfig().global());
		this.rc = rc;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanRouter(this.rc.getTripRouter());
	}
}
