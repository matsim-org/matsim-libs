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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributesConverter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
	private static final int THREADS_LIMIT = 4;
	private final boolean isPopulationStreaming;
	private final int numThreads;
	private final BlockingQueue<List<Tag>> tagQueue;
	private Thread[] threads;
	private List<Tag> currentPersonXmlData;

	private String inputCRS;
	private final String targetCRS;

	private boolean reachedPersons = false;

	private final BlockingQueue<CompletableFuture<Person>> personInsertionQueue = new LinkedBlockingQueue<>();
	private Thread personInsertionThread;
	private Throwable exception = null;

	public ParallelPopulationReaderMatsimV6(
			final String inputCRS,
			final String targetCRS,
			final Scenario scenario) {
		super(inputCRS, targetCRS, scenario);
		this.inputCRS = inputCRS;
		this.targetCRS = targetCRS;
		this.tagQueue = new LinkedBlockingQueue<>();

		/*
		 * Check whether population streaming is activated
		 */

		this.isPopulationStreaming = scenario.getPopulation() instanceof StreamingPopulationReader.StreamingPopulation;

		// Set threads
		if (scenario.getConfig().global().getNumberOfThreads() > 0) {
			this.numThreads = Math.min(THREADS_LIMIT,scenario.getConfig().global().getNumberOfThreads());
		} else this.numThreads = 1;
	}

	private static void initObjectAttributeConverters(ParallelPopulationReaderMatsimV6Runner runner, ObjectAttributesConverter converter)
	{
		Map<String, AttributeConverter<?>> targetConverter = runner.getObjectAttributesConverter().getConverters();
		targetConverter.putAll(converter.getConverters());
	}

	private void initThreads() {
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {

			ParallelPopulationReaderMatsimV6Runner runner =
					new ParallelPopulationReaderMatsimV6Runner(
							this.inputCRS,
							this.targetCRS,
							this.scenario,
							this.tagQueue,
							this.isPopulationStreaming);
			initObjectAttributeConverters(runner, this.getObjectAttributesConverter());

			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(ParallelPopulationReaderMatsimV6Runner.class.toString() + i);
			thread.setUncaughtExceptionHandler(this::catchReaderException);
			threads[i] = thread;
			thread.start();
		}

		if (this.scenario.getPopulation() instanceof StreamingPopulationReader.StreamingPopulation) {
			this.personInsertionThread = new Thread(new PersonInserter(this.scenario.getPopulation(), this.personInsertionQueue));
			this.personInsertionThread.start();
		}
	}

	private void stopThreads() {
		// signal the threads that they should end parsing
		for (int i = 0; i < this.numThreads; i++) {
			this.tagQueue.add(List.of(new EndProcessingTag()));
		}

		if (isPopulationStreaming) {
			CompletableFuture<Person> finishPerson = new CompletableFuture<>();
			finishPerson.complete(null);
			try {
				this.personInsertionQueue.put(finishPerson);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		// wait for the threads to finish
		try {
			for (Thread thread : threads) {
				thread.join();
			}
			if(this.isPopulationStreaming) {
				this.personInsertionThread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		if (this.exception != null) {
			throw new RuntimeException(this.exception);
		}
	}

	private void catchReaderException(Thread thread, Throwable throwable) {
		log.error("Error parsing XML", throwable);
		this.exception = throwable;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		//Reached first time a person
		if (PERSON.equals(name) && !this.reachedPersons) {
			this.reachedPersons = true;

			if (this.threads == null) {
				log.info("Start parallel population reading...");
				initThreads();
			}
		}

		// As long as we have not reached the persons in the xml, use super class
		if (!this.reachedPersons) {
			super.startTag(name, atts, context);
			return;
		}


		// If it is a new person, create a new person and a list for its attributes.
		if (PERSON.equals(name)) {
			if (this.exception != null) {
				this.stopThreads();
				throw new RuntimeException(this.exception);
			}

			// Just create a person, but do not add it here!
			Person person = this.plans.getFactory().createPerson(Id.create(atts.getValue(ATTR_PERSON_ID), Person.class));
			currentPersonXmlData = new ArrayList<>();
			PersonTag personTag = new PersonTag();
			personTag.person = person;
			currentPersonXmlData.add(personTag);

			// If in streaming mode, we need later complete persons
			if (isPopulationStreaming) {
				personTag.futurePerson = new CompletableFuture<>();
				try {
					this.personInsertionQueue.put(personTag.futurePerson);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			} else {
				// If not in streaming mode, we can work with a reference
				// of an unfinished person and add it right now...
				this.plans.addPerson(person);
			}
		} else {
			// Create a new start tag and add it to the person data.
			Stack<String> contextCopy = new Stack<>();
			contextCopy.addAll(context);
			StartTag tag = new StartTag();
			tag.name = name;
			tag.context = contextCopy;
			tag.atts = new AttributesImpl(atts);    // We have to create copies of the attributes because the object is re-used by the parser!
			currentPersonXmlData.add(tag);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(ATTRIBUTES.equals(name)&&context.peek().equals(POPULATION))
		{
			this.inputCRS = ProjectionUtils.getCRS(scenario.getPopulation());
		}

		// if population streaming is activated, use non-parallel reader
		// or if not reached the persons in the xml
		if (!this.reachedPersons) {
			super.endTag(name, content, context);
			return;
		}

		// End of population reached
		if (POPULATION.equals(name)) {
			this.stopThreads();

			super.endTag(name, content, context);
			log.info("Finished parallel population reading...");
			// Till parsing population
		} else {
			// Create a new end tag and add it to the person data.
			Stack<String> contextCopy = new Stack<>();
			contextCopy.addAll(context);
			EndTag tag = new EndTag();
			tag.name = name;
			tag.content = content;
			tag.context = contextCopy;
			currentPersonXmlData.add(tag);

			// if it's a person end tag, add the persons xml data to the queue.
			if (PERSON.equals(name)) {
				tagQueue.add(currentPersonXmlData);
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
		CompletableFuture<Person> futurePerson;
	}

	public final static class EndTag extends Tag {
		String content;
	}

	/*
	 * Marker Tag to inform the threads that no further data has to be parsed.
	 */
	public final static class EndProcessingTag extends Tag {
	}

	// This class is used to feed the population step by step
	// with new complete persons while being in streaming mode
	public final static class PersonInserter implements Runnable {
		Population population;
		BlockingQueue<CompletableFuture<Person>> personInsertionQueue;

		PersonInserter(Population population, BlockingQueue<CompletableFuture<Person>> personInsertionQueue)
		{
			this.population = population;
			this.personInsertionQueue = personInsertionQueue;
		}

		@Override
		public void run() {
			while (true) {
				try {
					CompletableFuture<Person> finishedPerson = this.personInsertionQueue.take();
					Person person = finishedPerson.get();
					if (person == null) {
						return;
					}
					this.population.addPerson(person);

				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}

