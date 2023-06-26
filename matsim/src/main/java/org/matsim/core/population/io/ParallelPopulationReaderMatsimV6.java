package org.matsim.core.population.io;

/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPopulationReaderMatsimV6.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Parallel implementation of the PopulationReaderMatsimV6. The main thread only reads
 * the file and creates empty person objects which are added to the population to ensure
 * that their order is not changed. Note that this approach is not compatible with
 * population streaming. When this feature is activated, the non-parallel reader is used.
 * The parallel threads interpret the xml data for each person.
 *
 * @author cdobler, steffenaxer
 */
/* deliberately package */  class ParallelPopulationReaderMatsimV6 extends PopulationReaderMatsimV6 {

	static final Logger log = LogManager.getLogger(ParallelPopulationReaderMatsimV6.class);

	private final boolean isPopulationStreaming;
	private final int numThreads;
	private final BlockingQueue<List<Tag>> queue;
	private Thread[] threads;
	private List<Tag> currentPersonXmlData;

	private final String inputCRS;
	private final String targetCRS;

	private boolean reachedPersons = false;


	public ParallelPopulationReaderMatsimV6(
			final String inputCRS,
			final String targetCRS,
			final Scenario scenario) {
		super(inputCRS, targetCRS, scenario);
		this.inputCRS = inputCRS;
		this.targetCRS = targetCRS;

		/*
		 * Check whether population streaming is activated
		 */

		if (scenario.getPopulation() instanceof StreamingPopulationReader.StreamingPopulation) {
			log.warn("Population streaming is activated - cannot use " + ParallelPopulationReaderMatsimV6.class.getName() + "!");

			this.isPopulationStreaming = true;
			this.numThreads = 1;
			this.queue = null;

		} else {
			isPopulationStreaming = false;

			if (scenario.getConfig().global().getNumberOfThreads() > 0) {
				this.numThreads = scenario.getConfig().global().getNumberOfThreads();
			} else this.numThreads = 1;

			this.queue = new LinkedBlockingQueue<>();
		}
	}

	private void initThreads() {
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {

			ParallelPopulationReaderMatsimV6Runner runner =
					new ParallelPopulationReaderMatsimV6Runner(
							this.inputCRS,
							this.targetCRS,
							this.scenario,
							this.queue);

			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(ParallelPopulationReaderMatsimV6Runner.class.toString() + i);
			threads[i] = thread;
			thread.start();
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		//Reached first time a person
		if (PERSON.equals(name) && !this.reachedPersons) {
			this.reachedPersons = true;

			if (!isPopulationStreaming && this.threads == null) {
				log.info("Start parallel population reading...");
				initThreads();
			}
		}

		// if population streaming is activated, use non-parallel reader
		if (isPopulationStreaming || !this.reachedPersons) {
			super.startTag(name, atts, context);
			return;
		}


		// If it is a new person, create a new person and a list for its attributes.
		if (PERSON.equals(name)) {
			Person person = this.plans.getFactory().createPerson(Id.create(atts.getValue(ATTR_PERSON_ID), Person.class));
			currentPersonXmlData = new ArrayList<>();
			PersonTag personTag = new PersonTag();
			personTag.person = person;
			currentPersonXmlData.add(personTag);
			this.plans.addPerson(person);
		} else {

			// Create a new start tag and add it to the person data.
			StartTag tag = new StartTag();
			tag.name = name;
			tag.context = (Stack<String>) context.clone();
			tag.atts = new AttributesImpl(atts);    // We have to create copies of the attributes because the object is re-used by the parser!
			currentPersonXmlData.add(tag);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

		// if population streaming is activated, use non-parallel reader
		// or if not reached the persons in the xml
		if (isPopulationStreaming || !this.reachedPersons) {
			super.endTag(name, content, context);
			return;
		}

		// End of population reached
		if (POPULATION.equals(name)) {
			// signal the threads that they should end parsing
			for (int i = 0; i < this.numThreads; i++) {
				List<Tag> list = new ArrayList<>();
				list.add(new EndProcessingTag());
				this.queue.add(list);
			}

			// wait for the threads to finish
			try {
				for (Thread thread : threads) {
					thread.join();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			super.endTag(name, content, context);
			log.info("Finished parallel population reading...");
			// Till parsing population
		} else {
			// Create a new end tag and add it to the person data.
			EndTag tag = new EndTag();
			tag.name = name;
			tag.content = content;
			tag.context = (Stack<String>) context.clone();
			currentPersonXmlData.add(tag);

			// if it's a person end tag, add the persons xml data to the queue.
			if (PERSON.equals(name)) {
				queue.add(currentPersonXmlData);
			}
		}
	}

	public abstract static class Tag {
		String name;
		Stack<String> context;
	}

	public final static class StartTag extends Tag {
		Attributes atts;
	}

	public final static class PersonTag extends Tag {
		Person person;
	}

	public final static class EndTag extends Tag {
		String content;
	}

	/*
	 * Marker Tag to inform the threads that no further data has to be parsed.
	 */
	public final static class EndProcessingTag extends Tag {
	}
}

