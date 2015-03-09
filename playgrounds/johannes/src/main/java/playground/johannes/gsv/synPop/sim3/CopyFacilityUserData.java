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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.matsim.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

public class CopyFacilityUserData implements SamplerListener {

	private static final Logger logger = Logger.getLogger(CopyFacilityUserData.class);

	private AtomicLong iter = new AtomicLong();

	private final long interval;

	public CopyFacilityUserData(long interval) {
		this.interval = interval;
	}

	@Override
	public void afterStep(Collection<ProxyPerson> population, Collection<ProxyPerson> mutations, boolean accepted) {
		if (iter.get() % interval == 0) {
			logger.debug("Copying facility user data to attributes...");
			int cnt = 0;

			for (ProxyPerson person : population) {
				for (ProxyPlan plan : person.getPlans()) {
					for (ProxyObject act : plan.getActivities()) {
						ActivityFacility f = (ActivityFacility) act.getUserData(ActivityLocationMutator.USER_DATA_KEY);
						if (f != null) {
							act.setAttribute(CommonKeys.ACTIVITY_FACILITY, f.getId().toString());
						} else {
							cnt++;
						}
					}
				}
			}

			if (cnt > 0) {
				logger.warn(String.format("%s activities with unset facility.", cnt));
			}
		}

		iter.incrementAndGet();
	}
}
