/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistanceSum.java
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

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;

import playground.johannes.socialnetworks.sim.analysis.Trajectory;
import playground.johannes.socialnetworks.sim.gis.ActivityDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class TripDistanceSum extends AbstractTrajectoryProperty {

	private final static Logger logger = Logger.getLogger(TripDistanceSum.class);
	
	private final String purpose;
	
	private final ActivityDistanceCalculator calculator;
	
	public TripDistanceSum(String purpose, ActivityDistanceCalculator calculator) {
		this.purpose = purpose;
		this.calculator = calculator;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>(trajectories.size());
		
		int zeroDistances = 0;
		
		for(Trajectory trajectory : trajectories) {
			for(int i = 2; i < trajectory.getElements().size(); i += 2) {
				Activity destination = (Activity) trajectory.getElements().get(i);
				if(purpose == null || destination.getType().equals(purpose)) {
					Activity origin = (Activity) trajectory.getElements().get(i - 2);
					double d = calculator.distance(origin, destination);
					if(d > 0)
						values.adjustOrPutValue(trajectory, d, d);
					else
						zeroDistances++;
				}
				
			}
		}
		
		if(zeroDistances > 0)
			logger.debug(String.format("%1$s trips with zero distance.", zeroDistances));
			
		return values;
	}

}
