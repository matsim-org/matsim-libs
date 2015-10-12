/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureLoadTask.java
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

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class DepartureLoadTask extends TransitionLoadTask {

	/**
	 * @param key
	 */
	public DepartureLoadTask() {
		super("t_dep");
		// TODO Auto-generated constructor stub
	}

//	@Override
//	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
//		Set<String> purposes = new HashSet<String>();
//		for(Trajectory t : trajectories) {
//			for(int i = 0; i < t.getElements().size(); i += 2) {
//				purposes.add(((Activity)t.getElements().get(i)).getType());
//			}
//		}
//		
//		purposes.add(null);
//		
//		for(String purpose : purposes) {
//			analyze(trajectories, purpose);
//		}
//	}
//
//	private void analyze(Set<Trajectory> trajectories, String type) {
//		TDoubleArrayList samples = new TDoubleArrayList(trajectories.size());
//		
//		for(Trajectory t : trajectories) {
//			for(int i = 1; i < t.getTransitions().size() - 1; i += 2) {
//				Activity act = (Activity) t.getElements().get(i + 1);
//				if(type == null || act.getType().equals(type)) {
//					double time = t.getTransitions().get(i);
//					samples.add(time);
//				}
//			}
//		}
//		
//		try {
//			if(type == null)
//				type = "all";
//			
//			if(!samples.isEmpty()) {
//				TDoubleDoubleHashMap load = Histogram.createHistogram(samples.toNativeArray(), FixedSampleSizeDiscretizer.create(samples.toNativeArray(), 50, 50), true);
//				TXTWriter.writeHistogram(load, "time", "n", String.format("%1$s/depload.%2$s.txt", getOutputDirectory(), type));
//			}
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TransitionLoadTask#getTime(playground.johannes.coopsim.pysical.Trajectory, int)
	 */
	@Override
	protected double getTime(Trajectory t, int idx) {
		return t.getTransitions().get(idx);
	}
}
