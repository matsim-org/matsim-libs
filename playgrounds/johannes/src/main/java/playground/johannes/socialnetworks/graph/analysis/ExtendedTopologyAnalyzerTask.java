/* *********************************************************************** *
 * project: org.matsim.*
 * ExtendedTopologyAnalyzerTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.analysis;

import org.matsim.contrib.sna.graph.analysis.ComponentsTask;

/**
 * @author illenberger
 *
 */
public class ExtendedTopologyAnalyzerTask extends AnalyzerTaskComposite {

	public ExtendedTopologyAnalyzerTask() {
		addTask(new ComponentsTask());
		
		CentralityTask task = new CentralityTask();
		task.setCalcAPLDistribution(false);
		task.setCalcBetweenness(false);
		addTask(task);
	}
}
