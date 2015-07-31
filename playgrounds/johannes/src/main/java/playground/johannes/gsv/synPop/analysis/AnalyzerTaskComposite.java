/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryAnalyzerTaskComposite.java
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
package playground.johannes.gsv.synPop.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class AnalyzerTaskComposite extends AnalyzerTask {

	private static final Logger logger = Logger.getLogger(AnalyzerTaskComposite.class);
	
	private List<AnalyzerTask> tasks;
	
	public AnalyzerTaskComposite() {
		tasks = new LinkedList<AnalyzerTask>();
	}
	
	public void addTask(AnalyzerTask task) {
		tasks.add(task);
	}
	
	public void setOutputDirectory(String output) {
		for(AnalyzerTask task : tasks) {
			task.setOutputDirectory(output);
		}
	}
	
	@Override
	public void analyze(Collection<PlainPerson> person, Map<String, DescriptiveStatistics> results) {
		for(AnalyzerTask task : tasks) {
			logger.debug(String.format("Running task %1$s...", task.getClass().getSimpleName()));
			task.analyze(person, results);
		}

	}

}
