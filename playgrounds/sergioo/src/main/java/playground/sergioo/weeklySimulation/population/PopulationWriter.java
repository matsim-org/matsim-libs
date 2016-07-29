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

package playground.sergioo.weeklySimulation.population;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationWriterHandler;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

public class PopulationWriter extends AbstractMatsimWriter implements MatsimWriter, PersonAlgorithm {

	private final double write_person_fraction;

	private PopulationWriterHandler handler = new PopulationWriterWeeklyHandlerImplV5();
	private final Population population;
	private final Network network;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");

	private final static Logger log = Logger.getLogger(PopulationWriter.class);
	
	
	public PopulationWriter(final Population population) {
		this(population, null, 1.0);
	}
	
	/**
	 * Creates a new PlansWriter to write out the specified plans to the file and with version
	 * as specified in the {@linkplain org.matsim.core.config.groups.PlansConfigGroup configuration}.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 *
	 * @param population the population to write to file
	 */
	public PopulationWriter(final Population population, final Network network) {
		this(population, network, 1.0);
	}

	/**
	 * Creates a new PlansWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 *
	 * @param population the population to write to file
	 * @param fraction of persons to write to the plans file
	 */
	public PopulationWriter(final Population population, final Network network, final double fraction) {
		super();
		this.population = population;
		this.network = network;
		this.write_person_fraction = fraction;
	}

	/**
	 * Writes all plans to the file.
	 */
	@Override
	public void write(final String filename) {
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
	public void write(OutputStream outputStream) {
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
	public void startStreaming(final String filename) {
		if ( true ) {
			throw new RuntimeException("the following is copy-and-paste from the original PopulationWriter; does not work "
					+ "any more after change of the streaming api, sorry.  kai, jul'16" ) ;
		}
//
//		if ((this.population instanceof Population) && (((Population) this.population).isStreaming())) {
//			// write the file head if it is used with streaming.
//			writeStartPlans(filename);
//		} else {
//			log.error("Cannot start streaming. Streaming must be activated in the Population.");
//		}
	}

	@Override
	public void run(final Person person) {
		writePerson(person);
	}

	public void closeStreaming() {
		if ( true ) {
			throw new RuntimeException("the following is copy-and-paste from the original PopulationWriter; does not work "
					+ "any more after change of the streaming api, sorry.  kai, jul'16" ) ;
		}
//
//		if ((this.population instanceof Population) && (((Population) this.population).isStreaming())) {
//			if (this.writer != null) {
//				writeEndPlans();
//			} else {
//				log.error("Cannot close streaming. File is not open.");
//			}
//		} else {
//			log.error("Cannot close streaming. Streaming must be activated in the Population.");
//		}
	}

	public final void writeStartPlans(final String filename) {
		try {
			this.openFile(filename);
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(this.population, this.writer);
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

	public BufferedWriter getWriter() {
		return this.writer;
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}
	
}
