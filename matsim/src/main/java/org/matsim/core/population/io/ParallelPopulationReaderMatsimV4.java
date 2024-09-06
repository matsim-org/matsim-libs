/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPlansReaderMatsimV4.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Parallel implementation of the PopulationReaderMatsimV4. The main thread only reads
 * the file and creates empty person objects which are added to the population to ensure
 * that their order is not changed. Note that this approach is not compatible with
 * population streaming. When this feature is activated, the non-parallel reader is used.
 *
 * The parallel threads interpret the xml data for each person.
 *
 * @author cdobler
 */
/* deliberately package */  class ParallelPopulationReaderMatsimV4 extends PopulationReaderMatsimV4 {

	static final Logger log = LogManager.getLogger(ParallelPopulationReaderMatsimV4.class);

	private final boolean isPopulationStreaming;
	private final int numThreads;
	private final BlockingQueue<List<Tag>> queue;
	private final CollectorScenario collectorScenario;
	private final CollectorPopulation collectorPopulation;

	private Thread[] threads;
	private List<Tag> currentPersonXmlData;

	private final CoordinateTransformation coordinateTransformation;
	private Throwable exception = null;

	public ParallelPopulationReaderMatsimV4(
			final Scenario scenario ) {
		this( new IdentityTransformation() , scenario );
	}

	public ParallelPopulationReaderMatsimV4(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		super( coordinateTransformation , scenario );
		this.coordinateTransformation = coordinateTransformation;

		/*
		 * Check whether population streaming is activated
		 */
//		if (scenario.getPopulation() instanceof Population && ((Population)scenario.getPopulation()).isStreaming()) {
		if ( scenario.getPopulation() instanceof StreamingPopulationReader.StreamingPopulation ) {
			log.warn("Population streaming is activated - cannot use " + ParallelPopulationReaderMatsimV4.class.getName() + "!");

			this.isPopulationStreaming = true;
			this.numThreads = 1;
			this.queue = null;
			this.collectorPopulation = null;
			this.collectorScenario = null;
		} else {
			isPopulationStreaming = false;

			if (scenario.getConfig().global().getNumberOfThreads() > 0) {
				this.numThreads = scenario.getConfig().global().getNumberOfThreads();
			} else this.numThreads = 1;

			this.queue = new LinkedBlockingQueue<>();
			this.collectorPopulation = new CollectorPopulation(this.plans);
			this.collectorScenario = new CollectorScenario(scenario, collectorPopulation);
		}
	}

	private void initThreads() {
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {

			ParallelPopulationReaderMatsimV4Runner runner =
					new ParallelPopulationReaderMatsimV4Runner(
							this.coordinateTransformation,
							this.collectorScenario,
							this.queue);

			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(ParallelPopulationReaderMatsimV4Runner.class.toString() + i);
			thread.setUncaughtExceptionHandler(this::catchReaderException);
			threads[i] = thread;
			thread.start();
		}
	}

	private void stopThreads() {
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

		// if population streaming is activated, use non-parallel reader
		if (isPopulationStreaming) {
			super.startTag(name, atts, context);
			return;
		}

		if (PLANS.equals(name)) {
			log.info("Start parallel population reading...");
			initThreads();
			startPlans(atts);
		}
		else {
			// If it is an new person, create a new person and a list for its attributes.
			if (PERSON.equals(name)) {
				Person person = this.plans.getFactory().createPerson(Id.create(atts.getValue("id"), Person.class));
				currentPersonXmlData = new ArrayList<>();
				PersonTag personTag = new PersonTag();
				personTag.person = person;
				currentPersonXmlData.add(personTag);
				this.plans.addPerson(person);
			}

			// Create a new start tag and add it to the person data.
			StartTag tag = new StartTag();
			tag.name = name;
			tag.atts = new AttributesImpl(atts);	// We have to create copies of the attributes because the object is re-used by the parser!
			currentPersonXmlData.add(tag);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

		// if population streaming is activated, use non-parallel reader
		if (isPopulationStreaming) {
			super.endTag(name, content, context);
			return;
		}

		if (PLANS.equals(name)) {
			this.stopThreads();
			super.endTag(name, content, context);
			log.info("Finished parallel population reading...");
		} else {
			// Create a new end tag and add it to the person data.
			EndTag tag = new EndTag();
			tag.name = name;
			tag.content = content;
			tag.context = context;
			currentPersonXmlData.add(tag);

			// if its a person end tag, add the persons xml data to the queue.
			if (PERSON.equals(name)) queue.add(currentPersonXmlData);
		}
	}

	private static class CollectorScenario implements Scenario {
		// yyyy Why is this necessary at all?  Could you please explain your design decisions?  The same instance is passed to all threads, so
		// what is the difference to using the underlying population directly?

		private final Scenario delegate;
		private final CollectorPopulation population;

		public CollectorScenario(Scenario scenario, CollectorPopulation population) {
			this.delegate = scenario;
			this.population = population;
		}

		@Override
		public Network getNetwork() {
			return this.delegate.getNetwork();
		}

		@Override
		public Population getPopulation() {
			return this.population;	// return collector population
		}

		@Override
		public ActivityFacilities getActivityFacilities() {
			return this.delegate.getActivityFacilities();
		}

		@Override
		public TransitSchedule getTransitSchedule() {
			return this.delegate.getTransitSchedule();
		}

		@Override
		public Config getConfig() {
			return this.delegate.getConfig();
		}

		@Override
		public void addScenarioElement(String name, Object o) {
			this.delegate.addScenarioElement(name, o);
		}

		@Override
		public Object getScenarioElement(String name) {
			return this.delegate.getScenarioElement(name);
		}

		@Override
		public Vehicles getTransitVehicles() {
			return this.delegate.getTransitVehicles();
		}

		@Override
		public Households getHouseholds() {
			return this.delegate.getHouseholds();
		}

		@Override
		public Lanes getLanes() {
			return this.delegate.getLanes();
		}

		@Override
		public Vehicles getVehicles() {
			return this.delegate.getVehicles() ;
		}
	}

	private static class CollectorPopulation implements Population {

		private final Population population;

		public CollectorPopulation(Population population) {
			this.population = population;
		}

		@Override
		public PopulationFactory getFactory() {
			return population.getFactory();
		}

		@Override
		public String getName() {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public void setName(String name) {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public Map<Id<Person>, ? extends Person> getPersons() {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public void addPerson(Person p) {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public Person removePerson(Id<Person> personId) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public org.matsim.utils.objectattributes.attributable.Attributes getAttributes() {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}
	}

	public abstract static class Tag {
		String name;
		Stack<String> context = null;	// not used by the PopulationReader
	}

	public static final class StartTag extends Tag {
		Attributes atts;
	}

	public static final class PersonTag extends Tag {
		Person person;
	}

	public static final class EndTag extends Tag {
		String content;
	}

	/*
	 * Marker Tag to inform the threads that no further data has to be parsed.
	 */
	public static final class EndProcessingTag extends Tag {
	}
}
