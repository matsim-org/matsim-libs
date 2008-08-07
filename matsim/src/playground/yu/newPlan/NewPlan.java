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

import org.matsim.network.NetworkLayer;
import org.matsim.population.Plans;
import org.matsim.population.PlansWriter;
import org.matsim.population.algorithms.PersonAlgorithm;

/**
 * @author yu
 * 
 */
public abstract class NewPlan extends PersonAlgorithm {
	protected PlansWriter pw;
	protected NetworkLayer net;

	public NewPlan(Plans plans) {
		pw = new PlansWriter(plans);
		pw.writeStartPlans();
	}

	/**
	 * 
	 */
	public NewPlan(NetworkLayer network, Plans plans) {
		this(plans);
		net = network;
	}

	public void writeEndPlans() {
		pw.writeEndPlans();
	}
}
