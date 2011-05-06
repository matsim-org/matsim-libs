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
package playground.droeder.Analysis.Trips.V2;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.AnalysisTrip;


/**
 * @author droeder
 *
 */
public class AnalysisTripV2 extends AnalysisTrip {
	
	private Integer nrOfElements = 0;
	
	public AnalysisTripV2(){
		
	}
	
	public void addElements(ArrayList<PlanElement> elements){
		this.nrOfElements = elements.size();
		super.analyzeElements(elements);
	}
	
	public void addEvents(ArrayList<PersonEvent> events){
		super.analyzeEvents(events);
	}
	
	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
}
