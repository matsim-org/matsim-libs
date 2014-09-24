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

package playground.johannes.gsv.synPop.invermo.sim;

import java.util.Collection;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.sim2.SamplerListener;

/**
 * @author johannes
 *
 */
public class CopyHomeLocations implements SamplerListener {

	private final long interval;
	
	private long iter;
	
	public CopyHomeLocations(long interval) {
		this.interval = interval;
	}
	
	@Override
	public void afterModify(ProxyPerson person) {
	}

	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson person, boolean accpeted) {
		iter++;
		if(iter % interval == 0) {
			for(ProxyPerson thePerson : population) {
				ActivityFacility home = (ActivityFacility) thePerson.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
				ProxyPlan plan = thePerson.getPlans().get(0);
				for(ProxyObject act : plan.getActivities()) {
					if(act.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase("home")) {
						act.setAttribute(CommonKeys.ACTIVITY_FACILITY, home.getId().toString());
					}
				}
			}
		}

	}

}
