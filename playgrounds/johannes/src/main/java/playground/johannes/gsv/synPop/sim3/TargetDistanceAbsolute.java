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
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainElement;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.sim.Hamiltonian;

/**
 * @author johannes
 *
 */
public class TargetDistanceAbsolute implements Hamiltonian {
	
//	private final double detourFactor = TargetDistanceHamiltonian.DEFAULT_DETOUR_FACTOR;

	private static final Object TARGET_DISTANCE_KEY = new Object();

	public double evaluate(Person person1) {
		PlainPerson person = (PlainPerson) person1;
		double errSum = 0;
		
		for (int i = 1; i < person.getPlan().getActivities().size(); i++) {
			PlainElement leg = (PlainElement) person.getPlan().getLegs().get(i - 1);
			Double targetDistance = (Double) leg.getUserData(TARGET_DISTANCE_KEY);
			
			if (targetDistance == null) {
				String val = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
				if (val != null) {
					targetDistance = new Double(val);
				}
			}

			if (targetDistance != null) {
				PlainElement prev = (PlainElement) person.getPlan().getActivities().get(i - 1);
				PlainElement next = (PlainElement) person.getPlan().getActivities().get(i);
				
				double dist = distance(prev, next);
//				dist = dist * detourFactor;
//				dist = dist * TargetDistanceHamiltonian.calcDetourFactor(dist);
				double delta = Math.abs(dist - targetDistance);
//				if(targetDistance > 1000000)
//					System.err.println();
				errSum += delta;
			}
		}
		
		return errSum;
	}
	
	private double distance(PlainElement origin, PlainElement destination) {
		ActivityFacility orgFac = (ActivityFacility) origin.getUserData(ActivityLocationMutator.USER_DATA_KEY);
		ActivityFacility destFac = (ActivityFacility) destination.getUserData(ActivityLocationMutator.USER_DATA_KEY);

		Coord c1 = orgFac.getCoord();
		Coord c2 = destFac.getCoord();

		double dx = c1.getX() - c2.getX();
		double dy = c1.getY() - c2.getY();
		double d = Math.sqrt(dx*dx + dy*dy); 
		
		return d;
	}

}
