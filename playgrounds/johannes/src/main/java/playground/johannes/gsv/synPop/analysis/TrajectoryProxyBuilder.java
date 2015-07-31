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

package playground.johannes.gsv.synPop.analysis;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.synpop.data.Episode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class TrajectoryProxyBuilder {

	public static Trajectory buildTrajectory(ProxyPerson person) {
		Trajectory t = new Trajectory(null);
		Episode plan = person.getPlan();
		for(int i = 0; i < plan.getActivities().size(); i++) {
			ProxyActAdaptor act = new ProxyActAdaptor(plan.getActivities().get(i));
			t.addElement(act, act.getEndTime());
			
			if(i < plan.getActivities().size() - 1) {
				ProxyLegAdaptor leg = new ProxyLegAdaptor(plan.getLegs().get(i));
				t.addElement(leg, leg.getDepartureTime() + leg.getTravelTime());
			}
		}
		
		return t;
	}
	
	public static Set<Trajectory> buildTrajectories(Collection<ProxyPerson> persons) {
		Set<Trajectory> trajectories = new HashSet<Trajectory>(persons.size());
		
		for(ProxyPerson person : persons) {
			trajectories.add(buildTrajectory(person));
		}
		
		return trajectories;
	}
}
