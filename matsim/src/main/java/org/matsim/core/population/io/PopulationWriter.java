/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2012 by the members listed in the COPYING,  *
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

package org.matsim.core.population.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

public final class PopulationWriter extends AbstractMatsimWriter implements MatsimWriter {

	private final double write_person_fraction;

	private final CoordinateTransformation coordinateTransformation;
	private PopulationWriterHandler handler = null;
	private final Population population;
	private final Network network;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");

	private final static Logger log = Logger.getLogger(PopulationWriter.class);
	
	
	public PopulationWriter(final Population population) {
		this(population, null, 1.0);
	}

	public PopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population) {
		this(coordinateTransformation , population, null, 1.0);
	}

	/**
	 * Creates a new PlansWriter to write out the specified plans to the file and with version
	 * as specified in the {@linkplain org.matsim.core.config.groups.PlansConfigGroup configuration}.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write(java.lang.String)} is called.
	 *
	 * @param population the population to write to file
	 */
	public PopulationWriter(final Population population, final Network network) {
		this(population, network, 1.0);
	}

	public PopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population,
			final Network network) {
		this(coordinateTransformation , population, network, 1.0);
	}

	/**
	 * Creates a new PlansWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write(java.lang.String)} is called.
	 *
	 * @param coordinateTransformation transformation from the internal CRS to the CRS in which the file should be written
	 * @param population the population to write to file
	 * @param fraction of persons to write to the plans file
	 */
	public PopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population,
			final Network network,
			final double fraction) {
		this.coordinateTransformation = coordinateTransformation;
		this.population = population;
		this.network = network;
		this.write_person_fraction = fraction;
		this.handler = new PopulationWriterHandlerImplV5(coordinateTransformation);
	}

	/**
	 * Creates a new PlansWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write(java.lang.String)} is called.
	 *
	 * @param population the population to write to file
	 * @param fraction of persons to write to the plans file
	 */
	public PopulationWriter(
			final Population population,
			final Network network,
			final double fraction) {
		this( new IdentityTransformation() , population , network , fraction );
	}

	/**
	 * Writes all plans to the file.
	 */
	@Override
	public final void write(final String filename) {
		try {
			this.openFile(filename);
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(this.population, this.writer);
			this.handler.writeSeparator(this.writer);
			this.writePersons();
			this.handler.endPlans(this.writer);
			log.info("Population written to: " + filename);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			this.close();
			counter.printCounter();
			counter.reset();
		}
	}

	/**
	 * Writes all plans to the output stream and closes it.
	 * 
	 */
	public final void write(OutputStream outputStream) {
		try {
			this.openOutputStream(outputStream);
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(this.population, this.writer);
			this.handler.writeSeparator(this.writer);
			this.writePersons();
			this.handler.endPlans(this.writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			this.close();
			counter.printCounter();
			counter.reset();
		}
	}


	private  final void writePersons() {
		for (Person p : PopulationUtils.getSortedPersons(this.population).values()) {
			writePerson(p);
		}
	}

	private final void writePerson(final Person person) {
		try {
			if ((this.write_person_fraction < 1.0) && (MatsimRandom.getRandom().nextDouble() >= this.write_person_fraction)) {
				return;
			}
			this.handler.writePerson(person, this.writer);
			counter.incCounter();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public final void writeV0(final String filename) {
		this.handler = new PopulationWriterHandlerImplV0( coordinateTransformation , this.network);
		write(filename);
	}

	public final void writeV4(final String filename) {
		this.handler = new PopulationWriterHandlerImplV4( coordinateTransformation , this.network );
		write(filename);
	}

	public final void writeV5(final String filename) {
		this.handler = new PopulationWriterHandlerImplV5(coordinateTransformation);
		write(filename);
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}
	
}
