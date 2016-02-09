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
package scenarios.illustrative.braess.analysis;

import org.matsim.api.core.v01.events.LinkEnterEvent;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * This class extends the abstract analysis tool for the specific scenario of
 * Braess' example.
 * 
 * @see scenarios.illustrative.analysis.TtAbstractAnalysisTool
 * 
 * @author tthunig
 * 
 */
public final class TtAnalyzeBraess extends TtAbstractAnalysisTool {
	
	@Override
	protected int determineRoute(LinkEnterEvent linkEnterEvent) {
		// in the braess scenario the route is unique if one gets a link enter
		// event of link 2_4, 3_4 or 3_5.
		int route = -1;
		switch (linkEnterEvent.getLinkId().toString()) {
		case "2_4":
		case "2_24":
			// the person uses the lower route
			route = 2;
			break;
		case "3_4": // the person uses the middle route
			route = 1;
			break;
		case "3_5": // the person uses the upper route
			route = 0;
			break;
		default:
			break;
		}
		return route;
	}

	@Override
	protected void defineNumberOfRoutes() {
		setNumberOfRoutes(3);
	}

}
