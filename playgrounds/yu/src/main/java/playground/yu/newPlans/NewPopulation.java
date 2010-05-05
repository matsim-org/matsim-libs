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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 */
public abstract class NewPopulation extends AbstractPersonAlgorithm {
	protected PopulationWriter pw;
	protected Network net;

	public NewPopulation(final Network network, final Population population, final String filename) {
		this.net = network;
		this.pw = new PopulationWriter(population, network);
		this.pw.writeStartPlans(filename);
	}

	public void writeEndPlans() {
		this.pw.writeEndPlans();
	}
}
