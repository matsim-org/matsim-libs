/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityOpenTimes.java
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;

/**
 * @author illenberger
 * 
 */
public class FacilityOpenTimes {

	private Discretizer discretizer = new LinearDiscretizer(900);
	
	public Map<String, Map<String, TDoubleDoubleHashMap>> statistics(ActivityFacilities facilities) {
		Map<String, Map<String, TDoubleDoubleHashMap>> types = new HashMap<String, Map<String, TDoubleDoubleHashMap>>();
		
		TObjectIntHashMap<String> typeCounts = new TObjectIntHashMap<String>();
		
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Map<String, ActivityOption> opts = facility.getActivityOptions();
			for (ActivityOption opt : opts.values()) {
				String type = opt.getType();

				Map<String, TDoubleDoubleHashMap> days = types.get(type);
				if (days == null) {
					days = new HashMap<String, TDoubleDoubleHashMap>();
					types.put(type, days);
				}

				for (DayType day : DayType.values()) {
					Set<OpeningTime> openTimes = opt.getOpeningTimes(day);

					if (openTimes != null) {
						
						
						TDoubleDoubleHashMap hist = days.get(day.toString());
						
						if(hist == null) {
							hist = new TDoubleDoubleHashMap();
							days.put(day.toString(), hist);
						}
						
						for(OpeningTime openTime : openTimes) {
							typeCounts.adjustOrPutValue(type, 1, 1);
							
							int start = (int) discretizer.discretize(openTime.getStartTime());
							int end = (int) discretizer.discretize(openTime.getEndTime());
							for (int t = start ; t <= end; t += discretizer.binWidth(t)) {
								hist.adjustOrPutValue(t, 1, 1);
							}
						}
					}
				}

			}
		}
		
		TObjectIntIterator<String> it = typeCounts.iterator();
		for(int i = 0; i< typeCounts.size(); i++) {
			it.advance();
			System.out.println(String.format("%1$s open times of type %2$s.", it.value(), it.key()));
		}
		
		return types;
	}
}
