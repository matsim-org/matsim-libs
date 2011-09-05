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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;
import playground.johannes.socialnetworks.sim.gis.MatsimCoordUtils;

/**
 * @author illenberger
 *
 */
public class TripDistanceSum extends AbstractTrajectoryProperty {

	private final String purpose;
	
	private final DistanceCalculator calculator;
	
	private final ActivityFacilities facilities;
	
	public TripDistanceSum(String purpose, ActivityFacilities facilities) {
		this(purpose, facilities, OrthodromicDistanceCalculator.getInstance());
	}
	
	public TripDistanceSum(String purpose, ActivityFacilities facilities, DistanceCalculator calculator) {
		this.purpose = purpose;
		this.facilities = facilities;
		this.calculator = calculator;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>(trajectories.size());
		
		for(Trajectory trajectory : trajectories) {
			for(int i = 2; i < trajectory.getElements().size(); i += 2) {
				Activity destination = (Activity) trajectory.getElements().get(i);
				
				if(purpose == null || destination.getType().equals(purpose)) {
					Id id = destination.getFacilityId();
					Coord dest = facilities.getFacilities().get(id).getCoord();
					
					Activity origin = (Activity) trajectory.getElements().get(i - 2);
					id = origin.getFacilityId();
					ActivityFacility fac = facilities.getFacilities().get(id);
					Coord source = fac.getCoord();
					
					double d = calculator.distance(MatsimCoordUtils.coordToPoint(source), MatsimCoordUtils.coordToPoint(dest));
					values.adjustOrPutValue(trajectory, d, d);
				}
				
			}
		}
		
		return values;
	}

}
