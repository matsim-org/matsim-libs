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
import org.matsim.utils.objectattributes.attributable.Attributes;

public final class StreamingPopulationWriter extends AbstractMatsimWriter implements MatsimWriter, PersonAlgorithm {

	private final double write_person_fraction;

	private final CoordinateTransformation coordinateTransformation;
	private PopulationWriterHandler handler = null;
	private final Population population;
	private final Network network;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");

	private final static Logger log = Logger.getLogger(StreamingPopulationWriter.class);
	
	
	public StreamingPopulationWriter(final Population population) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
		this(population, null, 1.0);
	}

	public StreamingPopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
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
	public StreamingPopulationWriter(final Population population, final Network network) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
		this(population, network, 1.0);
	}

	public StreamingPopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population,
			final Network network) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
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
	public StreamingPopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final Population population,
			final Network network,
			final double fraction) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
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
	public StreamingPopulationWriter(
			final Population population,
			final Network network,
			final double fraction) {
		// yyyyyy the PersonAlgorithm and the standard version of this class should be separated ...
		// the PersonAlgorithm should be called without the Population argument.  kai, jul'16
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


	// implementation of PersonAlgorithm
	// this is primarily to use the PlansWriter with filters and other algorithms.
	public final void startStreaming(final String filename) {
//		if ((this.population instanceof Population) && (((Population) this.population).isStreaming())) {
//		if ( this.population instanceof StreamingPopulationReader.StreamingPopulation ) {
	// write the file head if it is used with streaming.
			writeStartPlans(filename);
//		} else {
//			log.error("Cannot start streaming. Streaming must be activated in the Population.");
//		}
	}

	@Override
	public final void run(final Person person) {
		writePerson(person);
	}

	public final void closeStreaming() {
//		if ((this.population instanceof Population) && (((Population) this.population).isStreaming())) {
//		if ( this.population instanceof StreamingPopulationReader.StreamingPopulation ) {
			if (this.writer != null) {
				writeEndPlans();
			} else {
				log.error("Cannot close streaming. File is not open.");
			}
//		} else {
//			log.error("Cannot close streaming. Streaming must be activated in the Population.");
//		}
	}

	public final void writeStartPlans(final String filename) {
		Population fakepop = new Population(){

			@Override public PopulationFactory getFactory() {
				throw new RuntimeException("not implemented") ;
			}
			@Override public String getName() {
				return "population written from streaming" ;
			}
			@Override public void setName(String name) {
				throw new RuntimeException("not implemented") ;
			}
			@Override public Map<Id<Person>, ? extends Person> getPersons() {
				throw new RuntimeException("not implemented") ;
			}
			@Override public void addPerson(Person p) {
				throw new RuntimeException("not implemented") ;
			}
			@Override public Person removePerson(Id<Person> personId) {
				throw new RuntimeException("not implemented") ;
			}
			@Override public ObjectAttributes getPersonAttributes() {
				throw new RuntimeException("not implemented") ;
			}
			@Override
			public Attributes getAttributes() {
				throw new RuntimeException( "not implemented" );
			}

		} ;
		try {
			this.openFile(filename);
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(fakepop, this.writer);
			this.handler.writeSeparator(this.writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public final void writePersons() {
		for (Person p : PopulationUtils.getSortedPersons(this.population).values()) {
			writePerson(p);
		}
	}

	public final void writePerson(final Person person) {
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

	public final void writeEndPlans() {
		try {
			this.handler.endPlans(this.writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.close();
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

	public final BufferedWriter getWriter() {
		return this.writer;
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}
	
}
