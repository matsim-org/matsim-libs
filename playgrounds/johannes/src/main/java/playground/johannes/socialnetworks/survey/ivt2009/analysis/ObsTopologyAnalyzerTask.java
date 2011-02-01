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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import org.matsim.contrib.sna.graph.analysis.ComponentsTask;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.snowball.analysis.ObservedTransitivity;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.PropertyDegreeTask;

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
