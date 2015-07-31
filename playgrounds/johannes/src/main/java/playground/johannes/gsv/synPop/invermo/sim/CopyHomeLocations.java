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
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.sim3.SamplerListener;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;

/**
 * @author johannes
 *
 */
public class CopyHomeLocations implements SamplerListener {

	private final long interval;
	
	private final AtomicLong iter = new AtomicLong();
	
	public CopyHomeLocations(long interval) {
		this.interval = interval;
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, Collection<ProxyPerson> person, boolean accpeted) {
		if(iter.get() % interval == 0) {
			for(ProxyPerson thePerson : population) {
				ActivityFacility home = (ActivityFacility) thePerson.getUserData(SwitchHomeLocation.USER_FACILITY_KEY);
				ProxyPlan plan = thePerson.getPlans().get(0);
				for(Element act : plan.getActivities()) {
					if(ActivityType.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
						act.setAttribute(CommonKeys.ACTIVITY_FACILITY, home.getId().toString());
					}
				}
			}
		}
		
		iter.incrementAndGet();
	}

}
