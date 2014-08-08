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

package playground.johannes.gsv.synPop.sim;

import java.util.Collection;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class HActivityLocation implements Hamiltonian {
//
//	private final String activityType;
//	
//	private final GeometryFactory geoFactory = new GeometryFactory();
	
	private DistanceCalculator dCalc = new CartesianDistanceCalculator();
	
	private ActivityFacilities facilities;
	
	public HActivityLocation(ActivityFacilities facilities) {
		this.facilities = facilities;
//		this.activityType = null;
	}
	
	public HActivityLocation(ActivityFacilities facilities, String type) {
		this.facilities = facilities;
//		this.activityType = type;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		double delta = 0;
		
		for(int i = 1; i < modified.getPlan().getActivities().size(); i++) {
			ProxyObject oldPrevAct = original.getPlan().getActivities().get(i - 1);
			ProxyObject newPrevAct = modified.getPlan().getActivities().get(i - 1);
			
			ProxyObject oldAct = original.getPlan().getActivities().get(i);
			ProxyObject newAct = modified.getPlan().getActivities().get(i);
			
			String targetDistStr = original.getPlan().getLegs().get(i - 1).getAttribute(CommonKeys.LEG_DISTANCE);
			if(targetDistStr != null) {
				double targetDistance = Double.parseDouble(targetDistStr);
				double oldDelta = Math.abs(distance(oldPrevAct, oldAct) - targetDistance);
				double newDelta = Math.abs(distance(newPrevAct, newAct) - targetDistance);
				
				delta += oldDelta - newDelta;
			}
		}
		
		return delta; // should we divide by the number of legs?
		
		
		
//		Point source = (Point) original.getUserData(this);
//		if (source == null) {
//			Double homeX = new Double(original.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
//			Double homeY = new Double(original.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
//			source = geoFactory.createPoint(new Coordinate(homeX, homeY));
//			original.setUserData(this, source);
//		}
//		
//		double dOrig = delta(original, source);
//		double dMod = delta(modified, source);
//		
//		return dMod - dOrig;
	}

	private double distance(ProxyObject origin, ProxyObject destination) {
		ActivityFacility orgFac = getFacility(origin);
		ActivityFacility destFac = getFacility(destination);
		
		Point p1 = MatsimCoordUtils.coordToPoint(orgFac.getCoord());
		Point p2 = MatsimCoordUtils.coordToPoint(destFac.getCoord());
		
		double d = dCalc.distance(p1, p2);
		
		return d;
	}
	
	private ActivityFacility getFacility(ProxyObject act) {
		ActivityFacility fac = (ActivityFacility) act.getUserData(MutateActivityLocation.USER_DATA_KEY);
		if(fac == null) {
			fac = facilities.getFacilities().get(new IdImpl(act.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
			act.setUserData(MutateActivityLocation.USER_DATA_KEY, fac);
		}
		
		return fac;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(java.util.Collection)
	 */
	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		
		for(ProxyPerson person : persons) {
			int cnt = 0;
			double personSum = 0;
			for(int i = 1; i < person.getPlan().getActivities().size(); i++) {

				ProxyObject prevAct = person.getPlan().getActivities().get(i - 1);
				ProxyObject act = person.getPlan().getActivities().get(i);
				
				String targetDistStr = person.getPlan().getLegs().get(i - 1).getAttribute(CommonKeys.LEG_DISTANCE);
				if(targetDistStr != null) {
					double targetDistance = Double.parseDouble(targetDistStr);
				
					personSum += Math.abs(distance(prevAct, act) - targetDistance);
					cnt++;
				}
			}
			if(cnt > 0)
				sum += personSum/(double)cnt;
		}
		
		return sum;
	}

//	private double delta(ProxyPerson person, Point source) {
//		List<ProxyObject> activities = person.getPlan().getActivities();
//		
//		double delta = 0;
//		for(int i = 1; i < activities.size(); i++) { //skip first (presumably home) activity
//			ProxyObject activity = activities.get(i);
//			String type = (String) activity.getAttribute(CommonKeys.ACTIVITY_TYPE);
//			
//			if(!type.equalsIgnoreCase("home")) {
//			if(activityType == null || activityType.equalsIgnoreCase(type)) {
//				ProxyObject origLeg = person.getPlan().getLegs().get(i-1);
//				
//				String str = origLeg.getAttribute(CommonKeys.LEG_DISTANCE);
//				if(str != null) {
//					double targetDistance = Double.parseDouble(str);
//				
//					ActivityFacility facility = (ActivityFacility) activity.getUserData(MutateActivityLocation.USER_DATA_KEY);
//					if(facility == null) {
//						facility = facilities.getFacilities().get(new IdImpl(activity.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
//						activity.setUserData(MutateActivityLocation.USER_DATA_KEY, facility);
//					}
//					Point destination = MatsimCoordUtils.coordToPoint(facility.getCoord());
//				
//					double distance = dCalc.distance(source, destination);
//					delta += Math.abs(distance - targetDistance);
//				}
//			}
//			}
//		}
//		
//		return - delta;
//	}
}
