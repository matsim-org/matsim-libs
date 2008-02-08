/* *********************************************************************** *
 * project: org.matsim.*
 * EUTReRoute.java
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.PlansCalcRoute;

/**
 * @author illenberger
 *
 */
public class EUTReRoute extends MultithreadedModuleA {

	private NetworkLayer network;
	
	private KStateLinkCostProvider provider;
	
	/**
	 * 
	 */
	public EUTReRoute(NetworkLayer network, KStateLinkCostProvider provider) {
		this.network = network;
		this.provider = provider;
	}

	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		EUTRouter router = new EUTRouter(network, provider, new CARAFunction(0));
		return new PlansCalcRoute(null, null, null, false, router, router);
	}

}
