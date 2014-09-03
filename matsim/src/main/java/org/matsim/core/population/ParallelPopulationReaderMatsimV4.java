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

package org.matsim.core.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
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
 * population streaming. When this feature is activated, the non-parallel reader has is used.
 * 
 * The parallel threads interpret the xml data for each person.
 * 
 * @author cdobler
 */
public class ParallelPopulationReaderMatsimV4 extends PopulationReaderMatsimV4 {
	
	static final Logger log = Logger.getLogger(ParallelPopulationReaderMatsimV4.class);
	
	private final boolean isPopulationStreaming;
	private final int numThreads;
	private final BlockingQueue<List<Tag>> queue;
	private final CollectorScenario collectorScenario;
	private final CollectorPopulation collectorPopulation;

	private Thread[] threads;
	private List<Tag> currentPersonXmlData;
		
	public ParallelPopulationReaderMatsimV4(final Scenario scenario) {
		super(scenario);
		
		/*
		 * Check whether population streaming is activated
		 */
		if (scenario.getPopulation() instanceof PopulationImpl && ((PopulationImpl)scenario.getPopulation()).isStreaming()) {
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
			
			this.queue = new LinkedBlockingQueue<List<Tag>>();
			this.collectorPopulation = new CollectorPopulation(this.plans);
			this.collectorScenario = new CollectorScenario(scenario, collectorPopulation);
		}
	}
		
	private void initThreads() {
		threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			
			ParallelPopulationReaderMatsimV4Runner runner = new ParallelPopulationReaderMatsimV4Runner(this.collectorScenario, this.queue);
			
			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(ParallelPopulationReaderMatsimV4Runner.class.toString() + i);
			threads[i] = thread;
			thread.start();
		}
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
				PersonImpl person = new PersonImpl(this.scenario.createId(atts.getValue("id")));			
				currentPersonXmlData = new ArrayList<Tag>();
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
			// signal the threads that they should end parsing
			for (int i = 0; i < this.numThreads; i++) {
				List<Tag> list = new ArrayList<Tag>();
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
	
	private static class CollectorScenario extends ScenarioImpl {
		
		private final Scenario scenario;
		private final CollectorPopulation population;
		
		public CollectorScenario(Scenario scenario, CollectorPopulation population) {
			super(scenario.getConfig());
			this.scenario = scenario;
			this.population = population;
		}
		
		@Override
		public Id createId(String id) {
			return scenario.createId(id);
		}

		@Override
		public Network getNetwork() {
			if (this.scenario != null) { // super-Constructor calls some init Methods which might call this method
				return scenario.getNetwork();
			}
			return null;
		}

		@Override
		public Population getPopulation() {
			return this.population;	// return collector population
		}
		
		public ActivityFacilities getActivityFacilities() {
			return scenario.getActivityFacilities();
		}

		@Override
		public Coord createCoord(double x, double y) {
			return scenario.createCoord(x, y);
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
		public ObjectAttributes getPersonAttributes() {
			return population.getPersonAttributes();
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
		public Map<Id, ? extends Person> getPersons() {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public void addPerson(Person p) {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}
	}
	
	public abstract static class Tag {
		String name;
		Stack<String> context = null;	// not used by the PopulationReader
	}
	
	public final class StartTag extends Tag {
		Attributes atts;
	}
	
	public final class PersonTag extends Tag {
		PersonImpl person;		
	}
	
	public final class EndTag extends Tag {
		String content;
	}
	
	/*
	 * Marker Tag to inform the threads that no further data has to be parsed.
	 */
	public final class EndProcessingTag extends Tag {
	}
}
