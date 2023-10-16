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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

public final class StreamingPopulationWriter implements PersonAlgorithm {
	private final static Logger log = LogManager.getLogger(StreamingPopulationWriter.class);

	private final double write_person_fraction;

	private PopulationWriterHandler handler = null;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");
	private final Map<Class<?>, AttributeConverter<?>> attributeConverters = new HashMap<>();

	private static class DummyMatsimWriter extends AbstractMatsimWriter {
		BufferedWriter getWriter() {
			return writer;
		}
		void openHere(String filename ) {
			super.openFile(filename);
		}
		void closeHere() {
			super.close() ;
		}
	}
	private DummyMatsimWriter matsimWriter = new DummyMatsimWriter() ;


	public StreamingPopulationWriter() {
		this(1.0);
	}

	public StreamingPopulationWriter(
			final CoordinateTransformation coordinateTransformation) {
		this(coordinateTransformation , 1.0);
	}

	/**
	 *
	 * @param coordinateTransformation transformation from the internal CRS to the CRS in which the file should be written
	 * @param fraction of persons to write to the plans file
	 */
	public StreamingPopulationWriter(
			final CoordinateTransformation coordinateTransformation,
			final double fraction) {
		this.write_person_fraction = fraction;
		this.handler = new ParallelPopulationWriterHandlerV6(coordinateTransformation);
	}

	/**
	 * @param fraction of persons to write to the plans file
	 */
	public StreamingPopulationWriter(
			final double fraction) {
		this( new IdentityTransformation() , fraction );
	}

	public <T> void putAttributeConverter(Class<T> clazz, AttributeConverter<T> converter) {
		this.attributeConverters.put(clazz, converter);
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributeConverters.putAll(converters);
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
			if (matsimWriter.getWriter() != null) {
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
			@Override
			public Attributes getAttributes() {
				//A stream written Population cannot contain Population Attributes, only Person Attributes.
				return new AttributesImpl();
			}

		} ;
		try {
			matsimWriter.openHere(filename);
			this.handler.putAttributeConverters(this.attributeConverters);
			this.handler.writeHeaderAndStartElement(matsimWriter.getWriter());
			this.handler.startPlans(fakepop, matsimWriter.getWriter());
			this.handler.writeSeparator(matsimWriter.getWriter());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@SuppressWarnings("static-method")
	@Deprecated // see comments in method.  kai, dec'16
	public final void writePersons() {
		throw new RuntimeException("this execution path does not longer exist.  See comments in code") ;
		// you can program this yourself by using something like
//		for (Person p : PopulationUtils.getSortedPersons(population).values()) {
//			writePerson(p);
//		}
		// providing it at the level here would mean to have access to a population that is otherwise totally not needed for streaming.
		// kai, dec'16
	}

	public final void writePerson(final Person person) {
		try {
			if ((this.write_person_fraction < 1.0) && (MatsimRandom.getRandom().nextDouble() >= this.write_person_fraction)) {
				return;
			}
			this.handler.writePerson(person, matsimWriter.getWriter());
			counter.incCounter();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public final void writeEndPlans() {
		try {
			this.handler.endPlans(matsimWriter.getWriter());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		matsimWriter.closeHere();
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}

}
