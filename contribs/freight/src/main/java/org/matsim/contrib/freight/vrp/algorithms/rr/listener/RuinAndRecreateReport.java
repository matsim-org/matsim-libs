/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.utils.RouteUtils;

public class RuinAndRecreateReport implements AlgorithmEndsListener {

	private Long round(double time) {
		return Math.round(time);
	}

	@Override
	public void informAlgorithmEnds(VehicleRoutingProblemSolution currentSolution) {
		System.out.println("totalCosts=" + round(currentSolution.getTotalCost()));
		System.out.println("#vehicles=" + RouteUtils.getNuOfActiveRoutes(currentSolution.getRoutes()));
		System.out.println("transportCosts="+ round(RouteUtils.getTransportCosts(currentSolution.getRoutes())));
		System.out.println("transportTime=" + round(RouteUtils.getTransportTime(currentSolution.getRoutes())));
	}

}
