/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;


public class LocationChoice extends MultithreadedModuleA {

	private NetworkLayer network=null;
	private TravelCostI travelCostCalc=null;
	private TravelTimeI travelTimeCalc=null;


	public LocationChoice() {
	}

	public LocationChoice(
			final NetworkLayer network,
			final TravelCostI travelCostCalc,
			final TravelTimeI travelTimeCalc ) {

		this.init(network, travelCostCalc,travelTimeCalc );
	}


	private void init(
			final NetworkLayer network,
			final TravelCostI travelCostCalc,
			final TravelTimeI travelTimeCalc) {


		this.network=network;
		this.network.connect();
		this.travelCostCalc=travelCostCalc;
		this.travelTimeCalc=travelTimeCalc;
	}


	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		//return new RandomLocationMutator(this.network);
		return new GrowingCirclesLocationMutator(this.network);
		//return new PlanomatOptimizeLocations(this.network, this.travelCostCalc, this.travelTimeCalc);
	}
}
