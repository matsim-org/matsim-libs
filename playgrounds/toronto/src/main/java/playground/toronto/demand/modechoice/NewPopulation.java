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

package playground.toronto.demand.modechoice;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationWriter;

/**
 * @author yu
 */
public abstract class NewPopulation extends AbstractPersonAlgorithm {
	protected StreamingPopulationWriter pw;
	protected Network network;

	public NewPopulation(final Network network, final Population population,
			final String outputPopulationFilename) {
		this.network = network;
		pw = new StreamingPopulationWriter();
		pw.writeStartPlans(outputPopulationFilename);
	}

	@Override
	public void run(Person person) {
		beforeWritePersonHook(person);
		pw.writePerson(person);
	}

	/**
	 * this method should be overridden by subclasses
	 * 
	 * @param person
	 */
	protected void beforeWritePersonHook(Person person) {
		// dummy empty
	}

	public void writeEndPlans() {
		pw.writeEndPlans();
	}
}
