package org.matsim.modechoice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.modechoice.replanning.GeneratorContext;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Checks if any plan is violating the configured constraints.
 */
public class ModeConstraintChecker implements IterationEndsListener, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(ModeConstraintChecker.class);

	@Inject
	private Provider<GeneratorContext> provider;

	@Inject
	private PlanModelService service;

	@Inject
	private InformedModeChoiceConfigGroup config;

	private ThreadLocal<GeneratorContext> generator;

	private final Set<Person> violations = ConcurrentHashMap.newKeySet();

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		Population population = event.getServices().getScenario().getPopulation();

		if (event.getIteration() == 0 && service.hasConstraints() && config.getConstraintCheck() != InformedModeChoiceConfigGroup.ConstraintCheck.none) {

			generator = ThreadLocal.withInitial(() -> provider.get());
			violations.clear();

			ParallelPersonAlgorithmUtils.run(population, event.getServices().getConfig().global().getNumberOfThreads(), this);

			// reset field so it can be garbage collected
			generator = null;
		}

		if (violations.size() > 0) {

			log.error("There are {} constraints violations", violations.size());
			log.error("Violating persons are: {}", violations.stream().map(Person::getId).collect(Collectors.toList()));

			if (config.getConstraintCheck() == InformedModeChoiceConfigGroup.ConstraintCheck.abort)
				throw new IllegalStateException(String.format("Aborting due to %d constraint violations in input plans. Set constraintCheck to 'warn' or 'repair' to avoid this error.", violations.size()));
		}
	}

	@Override
	public void run(Person person) {

		// Copy so we can remove plans
		List<? extends Plan> plans = new ArrayList<>(person.getPlans());

		boolean hasValidPlan = false;
		boolean hasViolation = false;

		for (Plan plan : plans) {

			PlanModel model = PlanModel.newInstance(plan);
			boolean valid = service.isValidOption(model, model.getCurrentModesMutable());

			if (valid)
				hasValidPlan = true;
			else {
				hasViolation = true;
				violations.add(person);

				// remove all invalid plans, but keep at least one
				if (config.getConstraintCheck() == InformedModeChoiceConfigGroup.ConstraintCheck.repair && person.getPlans().size() > 1) {
					person.removePlan(plan);
				}
			}
		}

		Plan first = person.getPlans().iterator().next();

		// Choose the best candidate in order to repair the plan
		if (!hasValidPlan && config.getConstraintCheck() == InformedModeChoiceConfigGroup.ConstraintCheck.repair) {

			GeneratorContext ctx = generator.get();

			Collection<PlanCandidate> candidates = ctx.generator.generate(PlanModel.newInstance(first), null, null, 1, Double.NaN, Double.NaN);

			PlanCandidate candidate = candidates.iterator().next();
			candidate.applyTo(first);

			person.setSelectedPlan(first);
		} else if (hasViolation && hasValidPlan && config.getConstraintCheck() == InformedModeChoiceConfigGroup.ConstraintCheck.repair) {

			// set selected plan to one of the remaining
			person.setSelectedPlan(first);
		}
	}
}
