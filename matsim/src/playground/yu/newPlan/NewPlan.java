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
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 * 
 */
public abstract class NewPlan extends AbstractPersonAlgorithm {
	protected PopulationWriter pw;
	protected NetworkLayer net;

	public NewPlan(Population plans) {
		pw = new PopulationWriter(plans);
		pw.writeStartPlans();
	}

	/**
	 * 
	 */
	public NewPlan(NetworkLayer network, Population plans) {
		this(plans);
		net = network;
	}

	public void writeEndPlans() {
		pw.writeEndPlans();
	}
}
