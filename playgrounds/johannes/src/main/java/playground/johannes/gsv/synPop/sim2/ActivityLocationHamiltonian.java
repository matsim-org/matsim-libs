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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.MutateActivityLocation;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class ActivityLocationHamiltonian implements Hamiltonian {
	
	private final double detourFactor = 1.3;

	private static final Object TARGET_DISTANCE_KEY = new Object();

	private final DistanceCalculator calculator = CartesianDistanceCalculator
			.getInstance();

	private final ActivityFacilities facilities;

//	private double planDistanceDelta;
	
	public ActivityLocationHamiltonian(ActivityFacilities facilities) {
		this.facilities = facilities;
	}

//	@Override
//	public void beforeStep(ProxyPerson person) {
//		planDistanceDelta = evaluatePlan(person);
//	}
//
//	@Override
//	public double afterStep(ProxyPerson person) {
//		double newDelta = evaluatePlan(person);
//		
//		double diff = planDistanceDelta - newDelta;
//		return diff;
//	}

//	@Override
//	public double evaluate(Collection<ProxyPerson> persons) {
//		double sum = 0;
//		
//		for(ProxyPerson person : persons) {
//			int cnt = 0;
//			double personSum = 0;
//			for(int i = 1; i < person.getPlan().getActivities().size(); i++) {
//
//				ProxyObject prevAct = person.getPlan().getActivities().get(i - 1);
//				ProxyObject act = person.getPlan().getActivities().get(i);
//				
//				String targetDistStr = person.getPlan().getLegs().get(i - 1).getAttribute(CommonKeys.LEG_DISTANCE);
//				if(targetDistStr != null) {
//					double targetDistance = Double.parseDouble(targetDistStr);
//				
//					personSum += Math.abs(distance(prevAct, act) - targetDistance);
//					cnt++;
//				}
//			}
//			if(cnt > 0)
//				sum += personSum/(double)cnt;
//		}
//		
//		return sum;
//	}
	
	public double evaluate(ProxyPerson person) {
		double totaldelta = 0;
		
		for (int i = 1; i < person.getPlan().getActivities().size(); i++) {
			ProxyObject leg = person.getPlan().getLegs().get(i - 1);
			Double targetDistance = (Double) leg.getUserData(TARGET_DISTANCE_KEY);
			if (targetDistance == null) {
				String val = leg.getAttribute(CommonKeys.LEG_DISTANCE);
				if (val != null) {
					targetDistance = new Double(val);
				}
			}

			if (targetDistance != null) {
				ProxyObject prev = person.getPlan().getActivities().get(i - 1);
				ProxyObject next = person.getPlan().getActivities().get(i);
				
				double dist = distance(prev, next);
				targetDistance = Math.max(targetDistance, 100);
				dist = dist * detourFactor;
				double delta = Math.abs(dist - targetDistance)/targetDistance;
				totaldelta += delta;
			}
		}
		
		return totaldelta;
	}
	
	private double distance(ProxyObject origin, ProxyObject destination) {
		ActivityFacility orgFac = getFacility(origin);
		ActivityFacility destFac = getFacility(destination);

//		Point p1 = MatsimCoordUtils.coordToPoint(orgFac.getCoord());
//		Point p2 = MatsimCoordUtils.coordToPoint(destFac.getCoord());
//
//		double d = calculator.distance(p1, p2);

		Coord c1 = orgFac.getCoord();
		Coord c2 = destFac.getCoord();
		double d = Math.sqrt(c1.getX() * c2.getX() + c1.getY() * c2.getY()); 
		return d;
	}

	private ActivityFacility getFacility(ProxyObject act) {
		ActivityFacility fac = (ActivityFacility) act
				.getUserData(MutateActivityLocation.USER_DATA_KEY);
		if (fac == null) {
			fac = facilities.getFacilities().get(
					new IdImpl(act.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
			act.setUserData(MutateActivityLocation.USER_DATA_KEY, fac);
		}

		return fac;
	}
}
