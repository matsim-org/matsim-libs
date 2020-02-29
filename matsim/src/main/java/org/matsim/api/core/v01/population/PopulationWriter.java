/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nagel
 */
public final class PopulationWriter implements MatsimWriter {
	
	private final CoordinateTransformation transformation;
	private final Population population;
	private final Network network;
	private final Map<Class<?>,AttributeConverter<?>> attributeConverters = new HashMap<>();

	public PopulationWriter(
			final CoordinateTransformation transformation,
			Population population,
			Network network) {
		this.transformation = transformation;
		this.population = population;
		this.network = network;
	}
	public PopulationWriter(
			final CoordinateTransformation transformation,
			Population population) {
		// w/o network works for V5
		this( transformation, population, null ) ;
	}
	public PopulationWriter(Population population, Network network) {
		this( new IdentityTransformation() , population , network );
	}
	public PopulationWriter(Population population) {
		// w/o network works for V5
		this( new IdentityTransformation() , population , null );
	}


	public <T> void putAttributeConverter(Class<T> clazz, AttributeConverter<T> converter) {
		this.attributeConverters.put( clazz , converter );
	}

	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		this.attributeConverters.putAll( converters );
	}

	/**
	 * Writes the population in the most current format (currently population_v6.dtd).
	 */
	@Override
	public void write(final String filename) {
		writeV6( filename );
	}

	public void write(final OutputStream stream) {
		writeV6(stream);
	}

	/**
	 * Writes the population in the format of plans_v4.dtd
	 *
	 * @param filename
	 */
	public void writeV4(final String filename) {
		new org.matsim.core.population.io.PopulationWriter(transformation , this.population, this.network).writeV4(filename);
	}

	/**
	 * Writes the population in the format of population_v5.dtd
	 *
	 * @param filename
	 */
	public void writeV5(final String filename) {
		new org.matsim.core.population.io.PopulationWriter( transformation , this.population, this.network).writeV5(filename);
	}

	/**
	 * Writes the population in the format of population_v6.dtd
	 *
	 * @param filename
	 */
	public void writeV6(final String filename) {
		final org.matsim.core.population.io.PopulationWriter writer =
				new org.matsim.core.population.io.PopulationWriter( transformation , this.population, this.network);
		writer.putAttributeConverters( attributeConverters );
		writer.writeV6(filename);
	}

	public void writeV6(final OutputStream stream) {
		final org.matsim.core.population.io.PopulationWriter writer =
				new org.matsim.core.population.io.PopulationWriter( transformation , this.population, this.network);
		writer.putAttributeConverters( attributeConverters );
		writer.writeV6(stream);
	}
}
