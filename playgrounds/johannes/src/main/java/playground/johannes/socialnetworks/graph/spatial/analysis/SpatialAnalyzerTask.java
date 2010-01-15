/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAnalyzerTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.StandardAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class SpatialAnalyzerTask extends GraphAnalyzerTaskComposite {

	public SpatialAnalyzerTask(String output) {
		super(output);
		addTasks(new StandardAnalyzerTask(getOutputDirectory()));
		addTasks(new DistanceTask(getOutputDirectory()));
	}

}
