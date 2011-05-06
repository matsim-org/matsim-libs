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
package playground.droeder.Analysis.Trips.V1;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.AnalysisTrip;

/**
 * @author droeder
 *
 */
public class AnalysisTripV1 extends AnalysisTrip{
	

	public AnalysisTripV1(ArrayList<PersonEvent> events, ArrayList<PlanElement> elements){
		super.analyzeElements(elements);
		super.analyzeEvents(events);
	}
}
