/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim2;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.analysis.ActivityDistanceTask;
import playground.johannes.gsv.synPop.analysis.TrajectoryProxyBuilder;

/**
 * @author johannes
 *
 */
public class AnalyzerListener implements SamplerListener {

	private final TrajectoryAnalyzerTaskComposite task;
	
	private final String rootDir;
	
	private long iters;
	
	public AnalyzerListener(ActivityFacilities facilities, String rootDir) {
		this.rootDir = rootDir;
		task = new TrajectoryAnalyzerTaskComposite();
		task.addTask(new FacilityOccupancyTask(facilities));
//		task.addTask(new ActivityDistanceTask(facilities));
	
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson person, boolean accpeted) {
		iters++;
		
		Set<Trajectory> trajectories = TrajectoryProxyBuilder.buildTrajectories(population);
		String output = String.format("%s/%s", rootDir, String.valueOf(iters));
		File file = new File(output);
		file.mkdirs();
		try {
			TrajectoryAnalyzer.analyze(trajectories, task, file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim2.SamplerListener#afterModify(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public void afterModify(ProxyPerson person) {
		// TODO Auto-generated method stub
		
	}

}
