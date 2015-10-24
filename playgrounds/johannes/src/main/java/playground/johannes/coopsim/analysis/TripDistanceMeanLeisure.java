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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TripDistanceMeanLeisure extends AbstractTrajectoryProperty {

	private final DistanceCalculator calculator;
	
	private final ActivityFacilities facilities;
	
	public TripDistanceMeanLeisure(ActivityFacilities facilities) {
		this(facilities, OrthodromicDistanceCalculator.getInstance());
	}
	
	public TripDistanceMeanLeisure(ActivityFacilities facilities, DistanceCalculator calculator) {
		this.facilities = facilities;
		this.calculator = calculator;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		return null;
	}

	@Override
	public DescriptiveStatistics statistics(Set<? extends Trajectory> trajectories) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(Trajectory trajectory : trajectories) {
			double d_sum = 0;
			int cnt = 0;
			for(int i = 2; i < trajectory.getElements().size(); i += 2) {
				Activity destination = (Activity) trajectory.getElements().get(i);
				
				if(destination.getType().equals("visit") || destination.getType().equals("gastro") || destination.getType().equals("culture")) {
					Id id = destination.getFacilityId();
					Coord dest = facilities.getFacilities().get(id).getCoord();
					
					Activity origin = (Activity) trajectory.getElements().get(i - 2);
					id = origin.getFacilityId();
					ActivityFacility fac = facilities.getFacilities().get(id);
					Coord source = fac.getCoord();
					
					if(!destination.getFacilityId().equals(origin.getFacilityId())) {
					try {
						double d = calculator.distance(MatsimCoordUtils.coordToPoint(source), MatsimCoordUtils.coordToPoint(dest));
						if(d > 0) {
							d_sum += d;
							cnt++;
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					}
				}
				
			}
			if(cnt > 0) {
				stats.addValue(d_sum/(double)cnt);
			}
		}
		
		return stats;
		
	}

}
