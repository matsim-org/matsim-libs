/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.utils.pop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationWriter;

/**
 * Helper class, for handling plansfiles *
 *
 * @author aneumann, Yu
 *
 */
public abstract class NewPopulation extends AbstractPersonAlgorithm {
	private static final Logger log = Logger.getLogger(NewPopulation.class);
	protected StreamingPopulationWriter popWriter;
	protected Network net;

	public NewPopulation(final Network network, final Population population, final String filename) {
		this.popWriter = new StreamingPopulationWriter(population, network);
		this.popWriter.writeStartPlans(filename);
	}

	public void writeEndPlans() {
		log.info("Dumping plans to file");
		this.popWriter.writeEndPlans();
	}
}
