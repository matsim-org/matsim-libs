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

package playground.sergioo.typesPopulation2013.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriterHandler;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;

public class PopulationWriter extends MatsimXmlWriter implements MatsimWriter, PersonAlgorithm {

	private final double write_person_fraction;
	private boolean fileOpened = false;

	private PopulationWriterHandler handler = null;
	private final Population population;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");

	private final static Logger log = Logger.getLogger(PopulationWriter.class);

	/**
	 * Creates a new PlansWriter to write out the specified plans to the file and with version
	 * as specified in the {@linkplain org.matsim.core.config.groups.PlansConfigGroup configuration}.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 *
	 * @param population the population to write to file
	 */
	public PopulationWriter(final Population population) {
		this(population, 1.0);
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
	public PopulationWriter(final Population population, final double fraction) {
		super();
		this.population = population;
		this.write_person_fraction = fraction;
		this.handler = new PopulationWriterHandlerImplPops();
	}

	public void startStreaming(final String filename) {
		if ((this.population instanceof PopulationImpl) && (((PopulationImpl) this.population).isStreaming())) {
			// write the file head if it is used with streaming.
			writeStartPlans(filename);
		} else {
			log.error("Cannot start streaming. Streaming must be activated in the Population.");
		}
	}

	public void closeStreaming() {
		if ((this.population instanceof PopulationImpl) && (((PopulationImpl) this.population).isStreaming())) {
			if (this.fileOpened) {
				writeEndPlans();
			} else {
				log.error("Cannot close streaming. File is not open.");
			}
		} else {
			log.error("Cannot close streaming. Streaming must be activated in the Population.");
		}
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeStartPlans(final String filename) {
		try {
			openFile(filename);
			this.fileOpened = true;
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(this.population, this.writer);
			this.handler.writeSeparator(this.writer);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final void writePerson(final Person person) {
		try {
			if ((this.write_person_fraction < 1.0) && (MatsimRandom.getRandom().nextDouble() >= this.write_person_fraction)) {
				return;
			}
			this.handler.writePerson(person, this.writer);
			counter.incCounter();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final void writePersons() {
		for (Person p : PopulationUtils.getSortedPersons(this.population).values()) {
			writePerson(p);
		}
	}

	public final void writeEndPlans() {
		if (this.fileOpened) {
			try {
				this.handler.endPlans(this.writer);
				this.writer.flush();
				this.writer.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}



	/**
	 * Writes all plans to the file.
	 */
	@Override
	public void write(final String filename) {
		this.writeStartPlans(filename);
		this.writePersons();
		this.writeEndPlans();
		counter.printCounter();
		counter.reset();
		log.info("Population written to: " + filename);
	}

	public BufferedWriter getWriter() {
		return this.writer;
	}

	// implementation of PersonAlgorithm
	// this is primarily to use the PlansWriter with filters and other algorithms.
	@Override
	public void run(final Person person) {
		writePerson(person);
	}
}
