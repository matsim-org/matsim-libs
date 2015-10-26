/* *********************************************************************** *
 * project: org.matsim.*
 * TopoAnalyzerTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.studies.sbsurvey.analysis;


import org.matsim.contrib.socnetgen.sna.graph.analysis.*;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.ObservedTransitivity;

/**
 * @author illenberger
 *
 */
public class ObsTopologyAnalyzerTask extends AnalyzerTaskComposite {

	public ObsTopologyAnalyzerTask() {
		addTask(new GraphSizeTask());
		
		DegreeTask degreeTask = new DegreeTask();
		degreeTask.setModule(ObservedDegree.getInstance());
		addTask(degreeTask);
		
		TransitivityTask transitivityTask = new TransitivityTask();
		transitivityTask.setModule(ObservedTransitivity.getInstance());
		addTask(transitivityTask);
		
		PropertyDegreeTask xDegreeTask = new PropertyDegreeTask();
		xDegreeTask.setModule(ObservedDegree.getInstance());
		addTask(xDegreeTask);
		
		addTask(new ComponentsTask());
	}

}
