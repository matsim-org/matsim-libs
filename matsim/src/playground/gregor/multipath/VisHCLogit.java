/* *********************************************************************** *
 * project: org.matsim.*
 * VisHCLogit.java
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

package playground.gregor.multipath;

import org.matsim.network.NetworkLayer;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.vis.routervis.RouterNetStateWriter;
import org.matsim.utils.vis.routervis.VisLeastCostPathCalculator;

public class VisHCLogit extends ProbabilsticShortestPath implements LeastCostPathCalculator, VisLeastCostPathCalculator {

	public VisHCLogit(NetworkLayer network, TravelCostI costFunction,
			TravelTimeI timeFunction, RouterNetStateWriter writer) {
		super(network, costFunction, timeFunction);
		// TODO Auto-generated constructor stub
	}
	

}
