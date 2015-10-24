/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballAnalyzerTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis;


import org.matsim.contrib.socnetgen.sna.snowball.analysis.BridgeEdgeTask;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.ComponentsSeedTask;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.DegreeIterationTask;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.ResponseRateTask;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.WaveSizeTask;

/**
 * @author illenberger
 *
 */
public class SnowballAnalyzerTask extends AnalyzerTaskComposite {

	public SnowballAnalyzerTask() {
		addTask(new WaveSizeTask());
		addTask(new ResponseRateTask());
		
		DegreeIterationTask degreeTask = new DegreeIterationTask();
		degreeTask.setModule(ObservedDegree.getInstance());
		addTask(degreeTask);
		
		addTask(new SeedConnectionTask());
		addTask(new ComponentsSeedTask());
		addTask(new BridgeEdgeTask());
	}
}
