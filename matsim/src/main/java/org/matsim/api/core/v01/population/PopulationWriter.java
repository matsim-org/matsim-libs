/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;

/**
 * @author nagel
 *
 */
public class PopulationWriter implements MatsimWriter {

	private final Population population;
	private final Network network;

	public PopulationWriter(Population population, Network network) {
		this.population = population;
		this.network = network;
	}

	/**
	 * Writes the population in the most current format (currently plans_v4.dtd).
	 */
	public void write(final String filename) {
		writeV4(filename);
	}

	/**
	 * Writes the population in the format of plans_v4.dtd
	 *
	 * @param filename
	 */
	public void writeV4(final String filename) {
		new org.matsim.core.population.PopulationWriter(this.population, this.network).writeFileV4(filename);
	}

}
