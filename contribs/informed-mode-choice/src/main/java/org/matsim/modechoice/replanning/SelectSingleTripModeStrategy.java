package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import java.util.*;

/**
 * Selects the mode for a single leg, based on estimation.
 *
 * @see org.matsim.core.population.algorithms.ChooseRandomSingleLegMode
 */
public class SelectSingleTripModeStrategy extends AbstractMultithreadedModule {

	private static final Logger log = LogManager.getLogger(SelectSingleTripModeStrategy.class);

	private final Provider<SingleTripChoicesGenerator> generator;
	private final Provider<PlanSelector> selector;

	private final List<String> modes;

	public SelectSingleTripModeStrategy(GlobalConfigGroup globalConfigGroup,
	                                    List<String> modes,
	                                    Provider<SingleTripChoicesGenerator> generator,
	                                    Provider<PlanSelector> selector) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
		this.modes = modes;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm(generator.get(), selector.get(), modes);
	}


	public static Algorithm newAlgorithm(SingleTripChoicesGenerator generator, PlanSelector selector, Collection<String> modes) {
		return new Algorithm(generator, selector, modes);
	}

	public static final class Algorithm implements PlanAlgorithm {

		private final SingleTripChoicesGenerator generator;
		private final PlanSelector selector;
		private final Set<String> modes;
		private final Random rnd;

		public Algorithm(SingleTripChoicesGenerator generator, PlanSelector selector, Collection<String> modes) {
			this.generator = generator;
			this.selector = selector;
			this.modes = new HashSet<>(modes);
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {
			PlanModel model = PlanModel.newInstance(plan);
			run(plan, model);
		}

		public void run(Plan plan, PlanModel model) {

			// empty plan
			if (model.trips() == 0)
				return;

			boolean[] mask = new boolean[model.trips()];

			IntList options = new IntArrayList();

			// only select trips that are allowed to change
			for (int i = 0; i < model.trips(); i++) {
				if (modes.contains(model.getTripMode(i)))
					options.add(i);
			}

			if (options.isEmpty())
				return;

			int idx = options.getInt(rnd.nextInt(options.size()));

			// Set one trip to be modifiable
			mask[idx] = true;

			Collection<PlanCandidate> candidates = generator.generate(model, modes, mask);

			// Remove options that are the same as the current mode
			candidates.removeIf(c -> Objects.equals(c.getMode(idx), model.getTripMode(idx)));

			PlanCandidate selected = selector.select(candidates);

			log.debug("Candidates for person {} at trip {}: {} | selected {}", model.getPerson(), idx, candidates, selected);

			if (selected != null) {
				selected.applyTo(plan);
			}
		}
	}

}
