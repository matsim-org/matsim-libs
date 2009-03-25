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
package playground.yu.newPlans;

import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 *
 */
public abstract class NewPopulation extends AbstractPersonAlgorithm {
	protected PopulationWriter pw;
	protected NetworkLayer net;

	public NewPopulation(final Population plans) {
		this.pw = new PopulationWriter(plans);
		this.pw.writeStartPlans();
	}

	public NewPopulation(final Population population, final String filename) {
		this.pw = new PopulationWriter(population, filename, "v4");
		this.pw.writeStartPlans();
	}


	/**
	 *
	 */
	public NewPopulation(final NetworkLayer network, final Population plans) {
		this(plans);
		this.net = network;
	}

	public void writeEndPlans() {
		this.pw.writeEndPlans();
	}
}
