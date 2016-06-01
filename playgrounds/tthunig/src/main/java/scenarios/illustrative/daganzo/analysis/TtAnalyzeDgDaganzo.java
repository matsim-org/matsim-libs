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
package scenarios.illustrative.daganzo.analysis;

import org.matsim.api.core.v01.events.LinkEnterEvent;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * This class extends the abstract analysis tool for the specific scenario of
 * Daganzos example.
 * 
 * @see scenarios.illustrative.analysis.TtAbstractAnalysisTool
 * 
 * @author tthunig
 *
 */
public class TtAnalyzeDgDaganzo extends TtAbstractAnalysisTool {

	@Override
	protected int determineRoute(LinkEnterEvent linkEnterEvent) {
		// in the daganzo scenario the route is unique if one gets a link enter
		// event of link 3 or 4
		int route = -1;
		switch (linkEnterEvent.getLinkId().toString()) {
		case "3":
			// the person uses the longer route
			route = 1;
			break;
		case "4": 
			// the person uses the shorter route
			route = 0;
			break;
		default:
			break;
		}
		return route;
	}

	@Override
	protected void defineNumberOfRoutes() {
		setNumberOfRoutes(2);
	}
	
}
