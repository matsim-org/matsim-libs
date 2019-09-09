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
package playground.vsp.analysis.modules.ptTripAnalysis.traveltime;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.PlanElement;

import playground.vsp.analysis.modules.ptTripAnalysis.AbstractPlan2TripsFilter;

/**
 * @author droeder
 *
 */
public class Plan2TripsFilter extends AbstractPlan2TripsFilter{
	
	private Collection<String> networkmodes;
	private Collection<String> ptModes;
	
	public Plan2TripsFilter(Collection<String> ptModes, Collection<String> networkModes) {
		this.ptModes = ptModes;
		this.networkmodes = networkModes;
	}

	@Override
	protected TTAnalysisTrip generateTrip(ArrayList<PlanElement> elements) {
		TTAnalysisTrip trip = new TTAnalysisTrip(this.ptModes, this.networkmodes);
		trip.addElements(elements);
		return trip;
	}
	

}
