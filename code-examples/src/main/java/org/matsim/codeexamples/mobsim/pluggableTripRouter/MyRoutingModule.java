/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.codeexamples.mobsim.pluggableTripRouter;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

import javax.inject.Inject;
import java.util.List;

/**
 * @author nagel
 *
 */
public class MyRoutingModule implements RoutingModule {

	private final Object iterationData;
	
	@Inject MyRoutingModule(MySimulationObserver observer) {
		this.iterationData = observer.getIterationData();
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		 System.out.println(iterationData);
		 return List.of();
	}
}
