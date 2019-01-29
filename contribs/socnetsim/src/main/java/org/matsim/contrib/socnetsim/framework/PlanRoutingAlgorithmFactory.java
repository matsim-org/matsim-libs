/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRoutingAlgorithmFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework;

import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;

/**
 * If this proves to be useful, we could think of putting this in org.matsim.core.router.*
 * and use it to replace the createRoutingAlgorithm() method of Controler.
 * <br>
 * I am not sure what is better: pass a trip router as a parameter to the create method,
 * or pass a TripRouterFactory at the constructor, and make the factory a
 * "PlanAlgorithmFactory" without parameters, which could also be used in
 * AbstractMultithreadedModule.
 *
 * @author thibautd
 */
public interface PlanRoutingAlgorithmFactory {
	public PlanAlgorithm createPlanRoutingAlgorithm(TripRouter tripRouter);
}

