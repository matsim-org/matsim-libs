/* *********************************************************************** *
 * project: org.matsim.*
 * XY2LinksMultithreaded.java
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

package org.matsim.plans.algorithms;

import org.matsim.network.NetworkLayer;
import org.matsim.replanning.modules.MultithreadedModuleA;

public class XY2LinksMultithreaded extends MultithreadedModuleA {

	private NetworkLayer network;

	public XY2LinksMultithreaded(NetworkLayer network) {
		network.connect();
		this.network = network;
	}
	
	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		return new XY2Links(network);
	}
}
