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
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.johannes.synpop.data.CommonKeys;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class WrappedActivityLocationStrategy implements PlanStrategy {

	private static Logger logger = Logger.getLogger(WrappedActivityLocationStrategy.class);

	private ActivityLocationStrategy delegate;
	private final Set<Person> candidates;
	private int iteration;
	private List<HasPlansAndId> replannedPersons = new LinkedList<>();
	private OutputDirectoryHierarchy controlerIO;

	@Inject
	WrappedActivityLocationStrategy(GlobalConfigGroup globalConfigGroup, GsvConfigGroup gsvConfigGroup, Provider<TripRouter> tripRouterProvider, ActivityFacilities activityFacilities, Population population, OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO;
		Random random = new XORShiftRandom(globalConfigGroup.getRandomSeed());
		int numThreads = globalConfigGroup.getNumberOfThreads();
		String blacklist = "home";
		double mutationError = gsvConfigGroup.getMutationError();
		double threshold = gsvConfigGroup.getDistThreshold();
		this.delegate = new ActivityLocationStrategy(activityFacilities, population, random, numThreads, blacklist, mutationError, tripRouterProvider);
		candidates = new HashSet<>();
		for (Person person : population.getPersons().values()) {
			ObjectAttributes oatts = population.getPersonAttributes();

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
		logger.info(String.format("%s candidates for replanning out of %s persons.", candidates.size(), population.getPersons().size()));
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
		iteration = replanningContext.getIteration();
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
		int replanned = delegate.getAndResetReplanCount();
		logger.info(String.format("%s persons requested for replanning, %s persons replaned.", replannedPersons.size(), replanned));

		String file = controlerIO.getIterationFilename(iteration, "replannedPersons.txt");
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

}
