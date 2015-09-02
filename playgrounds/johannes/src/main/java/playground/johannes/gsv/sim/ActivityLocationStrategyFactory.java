/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.johannes.synpop.data.CommonKeys;

import javax.inject.Provider;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class ActivityLocationStrategyFactory implements Provider<PlanStrategy> {

	private static final Logger logger = Logger.getLogger(ActivityLocationStrategyFactory.class);

	private Strategy strategy;

	private Random random;

	private String blacklist;

	private final int numThreads;

	private final Controler controler;

	private final double mutationError;

	private Set<Person> candidates;

	private final double threshold;

	public ActivityLocationStrategyFactory(Random random, int numThreads, String blacklist, Controler controler, double mutationError,
			double threshold) {
		this.random = random;
		this.numThreads = numThreads;
		this.blacklist = blacklist;
		this.controler = controler;
		this.mutationError = mutationError;
		this.threshold = threshold;
	}

	@Override
	public PlanStrategy get() {
		if (strategy == null) {
			ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
			Population pop = controler.getScenario().getPopulation();
			ActivityLocationStrategy delegate = new ActivityLocationStrategy(facilities, pop, random, numThreads, blacklist, mutationError);
			strategy = new Strategy(delegate);
			controler.addControlerListener(strategy);

			candidates = new HashSet<>();
			for (Person person : pop.getPersons().values()) {
				ObjectAttributes oatts = pop.getPersonAttributes();

				List<Double> atts = (List<Double>) oatts.getAttribute(person.getId().toString(), CommonKeys.LEG_GEO_DISTANCE);
				if (atts != null) {
					for (Double d : atts) {
						if (d != null && d > threshold) {
							/*
							 * do not add the foreign dummy persons
							 */
							if (!person.getId().toString().startsWith("foreign")) {
								candidates.add(person);
								break;
							}
						}
					}
				}
			}

			logger.info(String.format("%s candidates for replanning out of %s persons.", candidates.size(), pop.getPersons().size()));

		}
		return strategy;
	}

	private class Strategy implements PlanStrategy, IterationStartsListener {

		private ActivityLocationStrategy delegate;

		private int iteration;

		// private int count;

		private List<HasPlansAndId> replannedPersons = new LinkedList<>();

		// private Controler controler;

		public Strategy(ActivityLocationStrategy delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run(HasPlansAndId<Plan, Person> person) {
			if (iteration >= 5) { // because of cadyts
				if (candidates.contains(person)) {
					delegate.run(person);
					replannedPersons.add(person);
					// count++;
				}
			}

		}

		@Override
		public void init(ReplanningContext replanningContext) {
			delegate.init(replanningContext);
		}

		@Override
		public void finish() {
			delegate.finish();
			int replanned = delegate.getAndResetReplanCount();
			logger.info(String.format("%s persons requested for replanning, %s persons replaned.", replannedPersons.size(), replanned));

			String file = controler.getControlerIO().getIterationFilename(iteration, "replannedPersons.txt");
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				for (HasPlansAndId person : replannedPersons) {
					writer.write(person.getId().toString());
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			replannedPersons = new LinkedList<>();
			// count = 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.matsim.core.controler.listener.IterationStartsListener#
		 * notifyIterationStarts
		 * (org.matsim.core.controler.events.IterationStartsEvent)
		 */
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			iteration = event.getIteration();

		}

	}
}
