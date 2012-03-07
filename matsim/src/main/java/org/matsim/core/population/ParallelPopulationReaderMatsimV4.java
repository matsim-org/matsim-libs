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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

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
	private final CollectorKnowledges collectorKnowledges;

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
			this.collectorKnowledges = null;
			this.collectorScenario = null;
		} else {
			isPopulationStreaming = false;

			if (scenario.getConfig().global().getNumberOfThreads() > 0) {
				this.numThreads = scenario.getConfig().global().getNumberOfThreads();			
			} else this.numThreads = 1;
			
			this.queue = new LinkedBlockingQueue<List<Tag>>();
			this.collectorPopulation = new CollectorPopulation(this.plans);
			this.collectorKnowledges = new CollectorKnowledges();
			this.collectorScenario = new CollectorScenario(scenario, collectorPopulation, collectorKnowledges);
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
			
			// add collector knowledges to knowledges
			if (knowledges != null) {
				knowledges.getKnowledgesByPersonId().putAll(this.collectorKnowledges.getKnowledgesByPersonId());
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
	
	private class CollectorScenario extends ScenarioImpl {
		
		private final Scenario scenario;
		private final CollectorPopulation population;
		private final CollectorKnowledges knowledges;
		
		public CollectorScenario(Scenario scenario, CollectorPopulation population, CollectorKnowledges knowledges) {
			super(scenario.getConfig());
			this.scenario = scenario;
			this.population = population;
			this.knowledges = knowledges;
		}
		
		@Override
		public Id createId(String id) {
			return scenario.createId(id);
		}

		@Override
		public Network getNetwork() {
			return scenario.getNetwork();
		}

		@Override
		public Population getPopulation() {
			return this.population;	// return collector population
		}
		
		@Override
		public Knowledges getKnowledges() {
			return this.knowledges; // return collector knowledges
		}
		
		public ActivityFacilitiesImpl getActivityFacilities() {
			return ((ScenarioImpl) scenario).getActivityFacilities();
		}

		@Override
		public Coord createCoord(double x, double y) {
			return scenario.createCoord(x, y);
		}
	}
	
	private class CollectorPopulation implements Population {

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
		public Map<Id, ? extends Person> getPersons() {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}

		@Override
		public void addPerson(Person p) {
			throw new RuntimeException("Calls to this method are not expected to happen...");
		}
	}
	
	/*
	 * We have to extend KnowledgesImpl since there a factory is created
	 * which directly accesses the Object which creates it. Otherwise
	 * the factory would call the original knowledges object.
	 * Alternatively we could ensure that KnowledgesImpl is thread-safe
	 * and add data directly to it instead of using this collector.
	 */
	private class CollectorKnowledges extends KnowledgesImpl {

		private final Map<Id, KnowledgeImpl> knowledgeByPersonId;
		
		public CollectorKnowledges() {
			this.knowledgeByPersonId = new ConcurrentHashMap<Id, KnowledgeImpl>();
		}

		@Override
		public Map<Id, KnowledgeImpl> getKnowledgesByPersonId() {
			return knowledgeByPersonId;
		}
	}
	
	public abstract class Tag {
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
