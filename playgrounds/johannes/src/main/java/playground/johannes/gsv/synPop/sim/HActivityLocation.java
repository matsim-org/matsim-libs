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

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyActivity;
import playground.johannes.gsv.synPop.ProxyLeg;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class HActivityLocation implements Hamiltonian {

	private final String activityType = "shop";
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	private DistanceCalculator dCalc = new OrthodromicDistanceCalculator();
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		Double homeX = (Double) original.getAttribute(CommonKeys.PERSON_HOME_COORD_X);
		Double homeY = (Double) original.getAttribute(CommonKeys.PERSON_HOME_COORD_Y);
		Point source = geoFactory.createPoint(new Coordinate(homeX, homeY));
		
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
			Double homeX = (Double) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X);
			Double homeY = (Double) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y);
			Point source = geoFactory.createPoint(new Coordinate(homeX, homeY));
			sum += delta(person, source);
		}
		
		return sum;
	}

	private double delta(ProxyPerson person, Point source) {
		List<ProxyActivity> activities = person.getPlan().getActivities();
		
		double delta = 0;
		for(int i = 0; i < activities.size(); i++) {
			ProxyActivity activity = activities.get(i);
			String type = (String) activity.getAttribute(CommonKeys.ACTIVITY_TYPE);
			
			if(activityType.equalsIgnoreCase(type)) {
				ProxyLeg origLeg = person.getPlan().getLegs().get(i-1);
				
				Double targetDistance = (Double) origLeg.getAttribute(CommonKeys.LEG_DISTANCE);
				if(targetDistance != null) {
				
				ActivityFacility facility = (ActivityFacility) activity.getAttribute(CommonKeys.ACTIVITY_FACILITY);
				Point destination = MatsimCoordUtils.coordToPoint(facility.getCoord());
				
				double distance = dCalc.distance(source, destination);
				delta += Math.abs(distance - targetDistance);
				}
			}
		}
		
		return - delta;
	}
}
