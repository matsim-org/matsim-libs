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
import java.util.List;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class HActivityLocation implements Hamiltonian {

	private final String activityType;
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	private DistanceCalculator dCalc = new CartesianDistanceCalculator();
	
	private ActivityFacilities facilities;
	
	public HActivityLocation(ActivityFacilities facilities) {
		this.facilities = facilities;
		this.activityType = null;
	}
	
	public HActivityLocation(ActivityFacilities facilities, String type) {
		this.facilities = facilities;
		this.activityType = type;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		Point source = (Point) original.getUserData(this);
		if (source == null) {
			Double homeX = new Double(original.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			Double homeY = new Double(original.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
			source = geoFactory.createPoint(new Coordinate(homeX, homeY));
			original.setUserData(this, source);
		}
		
		double dOrig = delta(original, source);
		double dMod = delta(modified, source);
		
		return dMod - dOrig;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(java.util.Collection)
	 */
	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		for(ProxyPerson person : persons) {
			Point source = (Point) person.getUserData(this);
			if (source == null) {
				Double homeX = new Double(person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
				Double homeY = new Double(person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
				source = geoFactory.createPoint(new Coordinate(homeX, homeY));
				person.setUserData(this, source);
			}
			sum += delta(person, source);
		}
		
		return sum;
	}

	private double delta(ProxyPerson person, Point source) {
		List<ProxyObject> activities = person.getPlan().getActivities();
		
		double delta = 0;
		for(int i = 1; i < activities.size(); i++) { //skip first (presumably home) activity
			ProxyObject activity = activities.get(i);
			String type = (String) activity.getAttribute(CommonKeys.ACTIVITY_TYPE);
			
			if(!type.equalsIgnoreCase("home")) {
			if(activityType == null || activityType.equalsIgnoreCase(type)) {
				ProxyObject origLeg = person.getPlan().getLegs().get(i-1);
				
				String str = origLeg.getAttribute(CommonKeys.LEG_DISTANCE);
				if(str != null) {
					double targetDistance = Double.parseDouble(str);
				
					ActivityFacility facility = (ActivityFacility) activity.getUserData(MutateActivityLocation.USER_DATA_KEY);
					if(facility == null) {
						facility = facilities.getFacilities().get(new IdImpl(activity.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
						activity.setUserData(MutateActivityLocation.USER_DATA_KEY, facility);
					}
					Point destination = MatsimCoordUtils.coordToPoint(facility.getCoord());
				
					double distance = dCalc.distance(source, destination);
					delta += Math.abs(distance - targetDistance);
				}
			}
			}
		}
		
		return - delta;
	}
}
