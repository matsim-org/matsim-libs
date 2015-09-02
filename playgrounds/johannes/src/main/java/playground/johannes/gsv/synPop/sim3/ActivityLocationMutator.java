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

import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainElement;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.Segment;

import java.util.List;
import java.util.Random;

/**
 * @author johannes
 * 
 */
public class ActivityLocationMutator implements SingleMutator {

	public static final Object USER_DATA_KEY = new Object();
	
	private static final Object IGNORE_KEY = new Object();

	private final String blacklist;

	private final Random random;

	private final FacilityData facilityData;

//	private final double mutationRange = 2000;

	private PlainElement currentAct;

	private ActivityFacility currentFacility;

	public ActivityLocationMutator(DataPool dataPool, Random random, String type) {
		this.random = random;
		this.blacklist = type;
		this.facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);

	}

	@Override
	public boolean mutate(Person person) {
		List<Segment> activities = ((PlainPerson)person).getPlan().getActivities();
	
		int idx = random.nextInt(activities.size());
		// if(idx == 0 || idx == activities.size() - 1)
		// return false;
	
		PlainElement act = (PlainElement)activities.get(idx);
	
		String type = null;
		Boolean ignore = (Boolean) act.getUserData(IGNORE_KEY);
		if (ignore == null) {
			type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
			if (blacklist == null || !blacklist.equalsIgnoreCase(type)) {
				ignore = false;
			} else {
				ignore = true;
			}
			act.setUserData(IGNORE_KEY, ignore);
		}
	
		if (!ignore) {
			if (type == null)
				type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
	
			currentAct = act;
			currentFacility = (ActivityFacility) act.getUserData(USER_DATA_KEY);
	
			ActivityFacility facility = null;
//			if (idx > 0 && idx < person.getPlan().getActivities().size() - 1) {
//				PlainElement prev = person.getPlan().getActivities().get(idx - 1);
//				PlainElement next = person.getPlan().getActivities().get(idx + 1);
//	
//				ActivityFacility prevFac = (ActivityFacility) prev.getUserData(USER_DATA_KEY);
//				if (prevFac == null) {
//					Id<ActivityFacility> id = Id.create(prev.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
//					prevFac = facilityData.getAll().getFacilities().get(id);
//					prev.setUserData(USER_DATA_KEY, prevFac);
//	
//				}
//	
//				ActivityFacility nextFac = (ActivityFacility) next.getUserData(USER_DATA_KEY);
//				if (nextFac == null) {
//					Id<ActivityFacility> id = Id.create(next.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
//					nextFac = facilityData.getAll().getFacilities().get(id);
//					next.setUserData(USER_DATA_KEY, nextFac);
//	
//				}
//	
//				if (prevFac.equals(nextFac)) {
//					PlainElement leg = person.getPlan().getLegs().get(idx - 1);
//					String value = leg.getAttribute(MiDKeys.LEG_ROUTE_DISTANCE);
//					if (value != null) {
//						double dist = Double.parseDouble(value);
//						// QuadTree<ActivityFacility> quadTree =
//						// quadTrees.get(act.getAttribute(CommonKeys.ACTIVITY_TYPE));
//						QuadTree<ActivityFacility> quadTree = facilityData.getQuadTree(act.getAttribute(CommonKeys.ACTIVITY_TYPE));
//						List<ActivityFacility> list = new ArrayList<ActivityFacility>(quadTree.get(prevFac.getCoord().getX(), prevFac.getCoord()
//								.getY(), dist - mutationRange, dist + mutationRange));
//	
//						facility = list.get(random.nextInt(list.size()));
//					} else {
//						facility = facilityData.randomFacility(type);
//					}
//				} else {
//					facility = facilityData.randomFacility(type);
//				}
//	
//			} else {
			List<ActivityFacility> list = facilityData.getFacilities(type);
			facility = list.get(random.nextInt(list.size()));
//				facility = facilityData.randomFacility(type);
//			}
//			act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
			act.setUserData(USER_DATA_KEY, facility);
			return true;
		} else {
			return false;
		}
	
	}

	@Override
	public void revert(Person person) {
//		currentAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, currentFacility.getId().toString());
		currentAct.setUserData(USER_DATA_KEY, currentFacility);

		currentAct = null;
		currentFacility = null;

	}

}
