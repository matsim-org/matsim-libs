/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPopulationWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.api.basic.v01.population;

import org.matsim.core.population.PopulationWriter;

/**
 * Writes a population to a file.
 *
 * @author mrieser
 */
public class BasicPopulationWriter {

	private final BasicPopulation population;
	
	public BasicPopulationWriter(final BasicPopulation population) {
		this.population = population;
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
		new PopulationWriter(population, filename, "v4").write();
	}
	
}
