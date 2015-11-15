/* *********************************************************************** *
 * project: org.matsim.*
 * ModeShareArrivalTask.java
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
package playground.johannes.coopsim.analysis;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 *
 */
public class ModeShareArrivalTask extends TrajectoryAnalyzerTask {
	
	private Discretizer discretizer = new LinearDiscretizer(3600.0);

	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		
		purposes.add(null);
		
		for(String purpose : purposes)
			analyze(trajectories, purpose);

	}

	private void analyze(Set<Trajectory> trajectories, String type) {
		Map<String, TDoubleDoubleHashMap> modes = new HashMap<String, TDoubleDoubleHashMap>();
		
		for(Trajectory t : trajectories) {
			for(int i = 1 ; i < t.getElements().size(); i += 2) {
				Leg leg = (Leg) t.getElements().get(i);
				Activity act = (Activity) t.getElements().get(i + 1);
				
				if(type == null || act.getType().equals(type)) {
					String mode = leg.getMode();
					
					TDoubleDoubleHashMap hist = modes.get(mode);
					if(hist == null) {
						hist = new TDoubleDoubleHashMap();
						modes.put(mode, hist);
					}
					
					hist.adjustOrPutValue(discretizer.discretize(t.getTransitions().get(i+1)), 1, 1);
				}
			}
		}
		
		try {
			if(type == null)
				type = "all";
			
			List<String> keys = new ArrayList<String>(modes.keySet());
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/mode_arr.%2$s.txt", getOutputDirectory(), type)));
			writer.write("time");
			for(String key : keys) {
				writer.write("\t");
				writer.write(key);
			}
			writer.newLine();
			
			for(int t = 0; t < 86000; t += discretizer.binWidth(t)) {
				double sum = 0;
				for(String key : keys) {
					TDoubleDoubleHashMap map = modes.get(key);
					sum += map.get(t);
				}
				
				writer.write(String.valueOf(t));
				for(String key : keys) {
					writer.write("\t");
					TDoubleDoubleHashMap map = modes.get(key);
					writer.write(String.valueOf(map.get(t)/sum));
				}
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
