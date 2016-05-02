/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.parallel.analysis;

import org.matsim.api.core.v01.events.LinkEnterEvent;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * This class extends the abstract analysis tool for the specific scenario that
 * we called parallel scenario.
 * 
 * @see scenarios.illustrative.analysis.TtAbstractAnalysisTool
 * 
 * @author tthunig
 * 
 */
public final class TtAnalyzeParallel extends TtAbstractAnalysisTool {
	
	@Override
	protected int determineRoute(LinkEnterEvent linkEnterEvent) {
		// in the parallel scenario the route is unique if one gets a link enter
		// event of link 2_3, 2_7, 5_4, 5_8, 10_3, 10_4, 11_7, 11_8
		int route = -1;
		switch (linkEnterEvent.getLinkId().toString()) {
		case "2_3":
			// upper route of A-B relation
			route = 0;
			break;
		case "2_7":
			// lower route of A-B relation
			route = 1;
			break;
		case "5_4":
			// upper route of B-A relation
			route = 2;
			break;
		case "5_8":
			// lower route of B-A relation
			route = 3;
			break;
		case "10_3":
			// left route of C-D relation
			route = 4;
			break;
		case "10_4":
			// right route of C-D relation
			route = 5;
			break;
		case "11_7":
			// left route of D-C relation
			route = 6;
			break;
		case "11_8":
			// right route of D-C relation
			route = 7;
			break;
		default:
			break;
		}
		return route;
	}

	@Override
	protected void defineNumberOfRoutes() {
		setNumberOfRoutes(8);
	}

}
