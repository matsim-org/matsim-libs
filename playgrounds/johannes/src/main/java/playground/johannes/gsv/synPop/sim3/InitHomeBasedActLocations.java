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

package playground.johannes.gsv.synPop.sim3;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainElement;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.processing.EpisodeTask;

import java.util.List;
import java.util.Random;

/**
 * @author johannes
 * 
 */
public class InitHomeBasedActLocations implements EpisodeTask {

	private final double error = 0.1;

	private final Random random;

	private final FacilityData data;

	public InitHomeBasedActLocations(DataPool pool, Random random) {
		this.data = (FacilityData) pool.get(FacilityDataLoader.KEY);
		this.random = random;
	}

	@Override
	public void apply(Episode plan) {
		/*
		 * Assign facilities for act with already set facility id.
		 */
		for (Attributable act : plan.getActivities()) {
			String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			if (id != null) {
				Id<ActivityFacility> idObj = Id.create(id, ActivityFacility.class);
				ActivityFacility f = data.getAll().getFacilities().get(idObj);
				((PlainElement)act).setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
			}
		}

		for (int i = 0; i < plan.getActivities().size(); i++) {
			PlainElement act = (PlainElement)plan.getActivities().get(i);
			ActivityFacility f = (ActivityFacility) act.getUserData(ActivityLocationMutator.USER_DATA_KEY);
			if (f == null) {
				String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				if (i > 0) {
					PlainElement prev = (PlainElement)plan.getActivities().get(i - 1);
					PlainElement leg = (PlainElement)plan.getLegs().get(i - 1);
					ActivityFacility prevFac = (ActivityFacility) prev.getUserData
							(ActivityLocationMutator
							.USER_DATA_KEY);
					String targetDist = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					if (prevFac != null && targetDist != null) {
						double d = Double.parseDouble(targetDist);
//						d = d/TargetDistanceHamiltonian.DEFAULT_DETOUR_FACTOR;
//						d = d/TargetDistanceHamiltonian.calcDetourFactor(d);
						f = getFacility(prevFac.getCoord(), d, type);
						act.setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
					} else {
						f = data.randomFacility(type);
						act.setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
					}
				} else {
					f = data.randomFacility(type);
					act.setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
				}
			}
		}
	}

	private ActivityFacility getFacility(Coord center, double radius, String type) {
		QuadTree<ActivityFacility> quadTree = data.getQuadTree(type);
		double factor = error;

		ActivityFacility f = null;
		int i = 0;
		while (f == null) {
			double min = Math.max(0, radius * (1 - factor));
			double max = radius * (1 + factor);

//			List<ActivityFacility> list = new ArrayList<>(quadTree.get(center.getX(), center.getY(), min, max));
			List<ActivityFacility> list = (List<ActivityFacility>) quadTree.get(center.getX(), center.getY(), min, max);
			if (list.isEmpty()) {
				if(i > 9) {
					f = data.randomFacility(type);
				} else {
					factor = factor * 2;
				}
			} else {
				f = list.get(random.nextInt(list.size()));
			}
			i++;
		}

		return f;

	}

}
