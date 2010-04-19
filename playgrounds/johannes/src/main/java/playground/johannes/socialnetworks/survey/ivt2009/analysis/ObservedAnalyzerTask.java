/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedAnalyzerTask.java
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

import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.ComponentsTask;
import playground.johannes.socialnetworks.graph.social.analysis.AgeTask;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedDegree;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedTransitivity;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.social.analysis.ObservedAge;

/**
 * @author illenberger
 *
 */
public class ObservedAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObservedAnalyzerTask() {
		addTask(new GraphSizeTask());
		addTask(new WaveSizeTask());
		
		DegreeTask dTask = new DegreeTask();
		dTask.setModule(new ObservedDegree());
		addTask(dTask);
		
		DegreeIterationTask dItTask = new DegreeIterationTask();
		dItTask.setModule(new ObservedDegree());
		addTask(dItTask);
		
		TransitivityTask tTask = new TransitivityTask();
		tTask.setModule(new ObservedTransitivity());
		addTask(tTask);
		
		AgeTask aTask = new AgeTask();
		aTask.setModule(new ObservedAge());
		addTask(aTask);
		
		addTask(new ComponentsTask());
	}

}
