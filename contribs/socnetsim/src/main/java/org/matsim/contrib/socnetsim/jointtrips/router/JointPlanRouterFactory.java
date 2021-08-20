/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanRouterFactory.java
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
package org.matsim.contrib.socnetsim.jointtrips.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.time_interpreter.TimeInterpreter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;

import com.google.inject.Inject;

/**
 * @author thibautd
 */
public class JointPlanRouterFactory implements PlanRoutingAlgorithmFactory {
	private final ActivityFacilities facilities;
	private final TimeInterpreter.Factory timeInterpreterFactory;

	@Inject
	public JointPlanRouterFactory(final Scenario sc, TimeInterpreter.Factory timeInterpreterFactory) {
		this( sc.getActivityFacilities(), timeInterpreterFactory );
	}

	public JointPlanRouterFactory(final ActivityFacilities facilities, TimeInterpreter.Factory timeInterpreterFactory) {
		this.facilities = facilities;
		this.timeInterpreterFactory = timeInterpreterFactory;
	}

	@Override
	public PlanAlgorithm createPlanRoutingAlgorithm(final TripRouter tripRouter) {
		return new JointPlanRouter( tripRouter , facilities, timeInterpreterFactory );
	}
}

