/* *********************************************************************** *
 * project: org.matsim.*
 * NewPlan.java
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
package playground.yu.newPlan;

import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * @author yu
 * 
 */
public abstract class NewPlan extends PersonAlgorithm {
	protected PlansWriter pw;

	/**
	 * 
	 */
	public NewPlan(Plans plans) {
		pw = new PlansWriter(plans);
		pw.writeStartPlans();
	}

	public NewPlan() {
	}// dummy constructor

	public void writeEndPlans() {
		pw.writeEndPlans();
	}
}
