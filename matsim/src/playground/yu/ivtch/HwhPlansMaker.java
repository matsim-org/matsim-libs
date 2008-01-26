/* *********************************************************************** *
 * project: org.matsim.*
 * HwhPlansMaker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.ivtch;

import java.util.Set;

import org.matsim.config.Config;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PlanSimplifyForDebug;

/**
 * @author ychen
 * 
 */
public class HwhPlansMaker extends PlanSimplifyForDebug {

	protected PlansWriter pw;
	private Config config;

	/**
	 * @param network
	 */
	public HwhPlansMaker(NetworkLayer network, Config config, Plans plans) {
		super(network);
		this.config = config;
		for (int i = 0; i <= 24; i++) {
			loadActType(homeActs, i);
		}
		for (int i = 25; i <= 45; i++) {
			loadActType(workActs, i);
		}
		for (int i = 46; i <= 66; i++) {
			loadActType(eduActs, i);
		}
		pw = new PlansWriter(plans);
		pw.writeStartPlans();
	}

	protected void loadActType(Set<String> acts, int i) {
		acts.add(config.getParam("planCalcScore", "activityType_" + i));
	}

	public void writeEndPlans() {
		pw.writeEndPlans();
	}

	@Override
	public void run(Person person) {
		super.run(person);
		if (person.getPlans().size() > 0) {
			pw.writePerson(person);
		}
	}

}
