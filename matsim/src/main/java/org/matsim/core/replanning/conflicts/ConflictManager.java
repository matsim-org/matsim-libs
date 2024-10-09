package org.matsim.core.replanning.conflicts;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.ReplanningUtils;

import com.google.common.base.Preconditions;

/**
 * This class handles conflicts during replanning. ConflictResolvers are used to
 * identify agents whose plans are conflicting with others and need to be
 * "rejected" in order to resolve these conflicts. "Rejecting" means to switch
 * those agents back to a plan in their memory that does not cause any
 * conflicts. Those are plans that are not "potentially conflicting", i.e.,
 * could interfere in any way with another agent. Those are usually plans that
 * don't contain a certain restricted/limited/capacitated mode or resource. The
 * logic of conflicts is defined using the ConflictResolver interface.
 */
public class ConflictManager {
	private final static Logger logger = LogManager.getLogger(ConflictManager.class);

	private final Set<ConflictResolver> resolvers;
	private final ConflictWriter writer;
	private final Random random;

	public ConflictManager(Set<ConflictResolver> resolvers, ConflictWriter writer, Random random) {
		this.resolvers = resolvers;
		this.random = random;
		this.writer = writer;
	}
	
	public void initializeReplanning(Population population) {
		if (resolvers.size() > 0) { // only require if active
			population.getPersons().values().forEach(ReplanningUtils::setInitialPlan);
		}
	}

	public void run(Population population, int iteration) {
		if (resolvers.size() == 0) {
			return;
		}

		logger.info("Resolving conflicts ...");

		Map<String, Integer> conflictCounts = new HashMap<>();
		IdSet<Person> conflictingIds = new IdSet<>(Person.class);

		for (ConflictResolver resolver : resolvers) {
			IdSet<Person> resolverConflictingIds = resolver.resolve(population, iteration);
			conflictCounts.put(resolver.getName(), resolverConflictingIds.size());
			conflictingIds.addAll(resolverConflictingIds);
		}

		logger.info("  Conflicts: " + conflictCounts.entrySet().stream()
				.map(entry -> String.format("%s=%d", entry.getKey(), entry.getValue()))
				.collect(Collectors.joining(", ")));

		int switchedToInitialCount = 0;
		int switchedToRandomCount = 0;

		for (Id<Person> personId : conflictingIds) {
			Person person = population.getPersons().get(personId);

			// If the initial plan is non-conflicting, switch back to it
			Plan initialPlan = ReplanningUtils.getInitialPlan(person);

			if (initialPlan != null && !isPotentiallyConflicting(initialPlan)) {
				person.setSelectedPlan(initialPlan);
				switchedToInitialCount++;
			} else {
				// Select a random non-conflicting plan
				List<Plan> candidates = person.getPlans().stream().filter(p -> !isPotentiallyConflicting(p))
						.collect(Collectors.toList());
				Preconditions.checkState(candidates.size() > 0,
						String.format("Agent %s has no non-conflicting plan", personId));

				// Shuffle, and select the first
				Collections.shuffle(candidates, random);
				person.setSelectedPlan(candidates.get(0));

				switchedToRandomCount++;
			}
		}

		logger.info(String.format("  %d (%.2f%%) switched to initial", switchedToInitialCount,
				(double) switchedToInitialCount / population.getPersons().size()));
		logger.info(String.format("  %d (%.2f%%) switched to random", switchedToRandomCount,
				(double) switchedToRandomCount / population.getPersons().size()));

		writer.write(iteration, switchedToInitialCount, switchedToRandomCount, conflictCounts);

		logger.info("  Done resolving conflicts!");
	}

	public boolean isPotentiallyConflicting(Plan plan) {
		for (ConflictResolver resolver : resolvers) {
			if (resolver.isPotentiallyConflicting(plan)) {
				return true;
			}
		}

		return false;
	}
}
