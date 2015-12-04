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

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TripDistanceTotal extends AbstractTrajectoryProperty {

//	private String whitelist;
//
//	private boolean useMode = false;

	private String ignorePurpose = "pt interaction"; // FIXME

	private final DistanceCalculator calculator;

	private final ActivityFacilities facilities;
	
	private PlanElementCondition<Leg> condition = DefaultCondition.getInstance();

	public TripDistanceTotal(ActivityFacilities facilities) {
		this.facilities = facilities;
		this.calculator = OrthodromicDistanceCalculator.getInstance();
	}
	/**
	 * @deprecated
	 */
	public TripDistanceTotal(String whitelist, ActivityFacilities facilities) {
		this(whitelist, facilities, OrthodromicDistanceCalculator.getInstance());
		condition = new LegPurposeCondition(whitelist);
	}
	
	/**
	 * @deprecated
	 */
	public TripDistanceTotal(String whitelist, ActivityFacilities facilities, boolean useMode) {
		this(whitelist, facilities, OrthodromicDistanceCalculator.getInstance());
		if(useMode)
			condition = new LegModeCondition(whitelist);
		else
			condition = new LegPurposeCondition(whitelist);
	}

	/**
	 * @deprecated
	 */
	public TripDistanceTotal(String whitelist, ActivityFacilities facilities, DistanceCalculator calculator) {
		this(whitelist, facilities, calculator, null);
	}

	/**
	 * 
	 * @deprecated
	 */
	public TripDistanceTotal(String whitelist, ActivityFacilities facilities, DistanceCalculator calculator, String ignore) {
		this.facilities = facilities;
		this.calculator = calculator;
		// this.ignorePurpose = ignore;
		condition = new LegPurposeCondition(whitelist);
	}

	/**
	 * 
	 * @deprecated
	 */
	public TripDistanceTotal(String whitelist, ActivityFacilities facilities, DistanceCalculator calculator, boolean useMode) {
		this(whitelist, facilities, calculator);
		if(useMode)
			condition = new LegModeCondition(whitelist);
		else
			condition = new LegPurposeCondition(whitelist);
	}

	public void setCondition(PlanElementCondition<Leg> condition) {
		this.condition = condition;
	}
	
	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>(trajectories.size());

		for (Trajectory trajectory : trajectories) {
			double d_sum = 0;
//			int cnt = 0;
			for (int i = 2; i < trajectory.getElements().size(); i += 2) {
				if(condition.test(trajectory, (Leg) trajectory.getElements().get(i-1), i - 1)) {
					Activity destination = (Activity) trajectory.getElements().get(i);

//				boolean match = false;
//
//				if (useMode) {
//					Leg leg = (Leg) trajectory.getElements().get(i - 1);
//					if (whitelist == null || leg.getMode().equalsIgnoreCase(whitelist)) {
//						match = true;
//					}
//				} else {
//					if (!destination.getType().equals(ignorePurpose)) {
//						if (whitelist == null || destination.getType().equals(whitelist)) {
//							match = true;
//						}
//					}
//				}
//
//				if (match) {
					Id id = destination.getFacilityId();
					Coord dest = facilities.getFacilities().get(id).getCoord();

					Activity origin = null;
					boolean valid = false;
					int k = i;
					while (!valid) {
						origin = (Activity) trajectory.getElements().get(k - 2);
						if (!origin.getType().equals(ignorePurpose))
							valid = true;
						else
							k -= 2;
					}

					id = origin.getFacilityId();
					ActivityFacility fac = facilities.getFacilities().get(id);
					Coord source = fac.getCoord();

					if (!destination.getFacilityId().equals(origin.getFacilityId())) {
						try {
							double d = calculator.distance(MatsimCoordUtils.coordToPoint(source), MatsimCoordUtils.coordToPoint(dest));
							d_sum += d;
//							cnt++;

						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						}

					}
				}
			}
//			if (cnt > 0) {
//				double d_mean = d_sum / (double) cnt;
				values.adjustOrPutValue(trajectory, d_sum, d_sum);
//			}
		}

		return values;
	}

}
