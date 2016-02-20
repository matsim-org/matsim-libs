/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2011 by the members listed in the COPYING,  *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

/**
 * @author nagel
 */
public class PopulationWriter implements MatsimWriter {

	private final CoordinateTransformation transformation;
	private final Population population;
	private final Network network;

	public PopulationWriter(
			final CoordinateTransformation transformation,
			Population population,
			Network network) {
		this.transformation = transformation;
		this.population = population;
		this.network = network;
	}

	public PopulationWriter(Population population, Network network) {
		this( new IdentityTransformation() , population , network );
	}

	/**
	 * Writes the population in the most current format (currently population_v5.dtd).
	 */
	@Override
	public void write(final String filename) {
		writeV5(filename);
	}

	/**
	 * Writes the population in the format of plans_v4.dtd
	 *
	 * @param filename
	 */
	public void writeV4(final String filename) {
		new org.matsim.core.population.PopulationWriter(transformation , this.population, this.network).writeFileV4(filename);
	}

	/**
	 * Writes the population in the format of population_v5.dtd
	 *
	 * @param filename
	 */
	public void writeV5(final String filename) {
		new org.matsim.core.population.PopulationWriter( transformation , this.population, this.network).writeFileV5(filename);
	}

}
