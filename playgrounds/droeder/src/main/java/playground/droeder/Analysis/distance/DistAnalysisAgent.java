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
package playground.droeder.Analysis.distance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author droeder
 *
 */
public class DistAnalysisAgent {
	
	private Map<Id, DistAnalysisTrip> trips;
	
	public DistAnalysisAgent(List<PlanElement> elements){
		this.trips = this.generateTrips(elements);
	}

	/**
	 * @param elements
	 * @return
	 */
	private Map<Id, DistAnalysisTrip> generateTrips(List<PlanElement> elements) {
		Map<Id, DistAnalysisTrip> agentTrips = new HashMap<Id, DistAnalysisTrip>();
		
		//TODO implement
		
		
		return agentTrips;
	}

}
