package org.matsim.modechoice.replanning;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;

import javax.inject.Provider;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;

/**
 * Selects the mode for a single leg, based on estimation.
 *
 * @see org.matsim.core.population.algorithms.ChooseRandomSingleLegMode
 */
public class SelectSingleTripModeStrategy extends AbstractMultithreadedModule {

	private static final Logger log = LogManager.getLogger(SelectSingleTripModeStrategy.class);

	private final Provider<SingleTripChoicesGenerator> generator;
	private final Provider<Selector<PlanCandidate>> selector;

	public SelectSingleTripModeStrategy(GlobalConfigGroup globalConfigGroup, Provider<SingleTripChoicesGenerator> generator,
	                                   Provider<Selector<PlanCandidate>> selector) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm(generator.get(), selector.get());
	}

	private static final class Algorithm implements PlanAlgorithm {

		private final SingleTripChoicesGenerator generator;
		private final Selector<PlanCandidate> selector;
		private final Random rnd;

		public Algorithm(SingleTripChoicesGenerator generator, Selector<PlanCandidate> selector) {
			this.generator = generator;
			this.selector = selector;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {

			PlanModel model = new PlanModel(plan);

			// empty plan
			if (model.trips() == 0)
				return;

			boolean[] mask = new boolean[model.trips()];

			// Set one trip to be modifiable
			int idx = rnd.nextInt(mask.length);

			mask[idx] = true;

			// TODO: only select trips that are allowed to change

			Collection<PlanCandidate> candidates = generator.generate(plan, mask);

			// Remove options that are the same as the current mode
			candidates.removeIf(c -> Objects.equals(c.getMode(idx), model.getTripMode(idx)));

			PlanCandidate selected = selector.select(candidates);

			log.debug("Candidates for person {} at trip {}: {} | selected {}", plan.getPerson(), idx, candidates, selected);

			if (selected != null) {
				selected.applyTo(plan);
			}
		}
	}

}
