/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis.Trips.travelTime.V2;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.PlanElement;

import playground.droeder.Analysis.Trips.AbstractPlan2TripsFilter;
import playground.droeder.Analysis.Trips.travelTime.AbstractTTAnalysisTrip;

/**
 * @author droeder
 *
 */
public class TTPlan2TripsFilterV2 extends AbstractPlan2TripsFilter {

	@Override
	protected AbstractTTAnalysisTrip generateTrip(ArrayList<PlanElement> elements) {
		AbstractTTAnalysisTrip trip =  new TTAnalysisTripV2();
		trip.addElements(elements);
		return trip;
	}
	

}
