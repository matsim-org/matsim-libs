package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.pruning.CandidatePruner;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;

import javax.annotation.Nullable;
import jakarta.inject.Provider;
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
	private final Provider<CandidatePruner> pruner;

	private final boolean requireDifferentModes;

	public SelectSingleTripModeStrategy(GlobalConfigGroup globalConfigGroup,
	                                    List<String> modes,
	                                    Provider<SingleTripChoicesGenerator> generator,
	                                    Provider<PlanSelector> selector,
	                                    Provider<CandidatePruner> pruner, boolean requireDifferentModes) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
		this.modes = modes;
		this.pruner = pruner;
		this.requireDifferentModes = requireDifferentModes;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm(generator.get(), selector.get(), pruner.get(), modes, requireDifferentModes);
	}


	public static Algorithm newAlgorithm(SingleTripChoicesGenerator generator, PlanSelector selector, CandidatePruner pruner, Collection<String> modes, boolean requireDifferentModes) {
		return new Algorithm(generator, selector, pruner, modes, requireDifferentModes);
	}

	public static final class Algorithm implements PlanAlgorithm {

		private final SingleTripChoicesGenerator generator;
		private final PlanSelector selector;
		private final CandidatePruner pruner;
		private final Set<String> modes;
		private final Random rnd;
		private final boolean requireDifferentModes;

		public Algorithm(SingleTripChoicesGenerator generator, PlanSelector selector, CandidatePruner pruner, Collection<String> modes, boolean requireDifferentModes) {
			this.generator = generator;
			this.selector = selector;
			this.pruner = pruner;
			this.modes = new HashSet<>(modes);
			this.requireDifferentModes = requireDifferentModes;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {
			PlanModel model = PlanModel.newInstance(plan);

			PlanCandidate c = chooseCandidate(model, null);

			if (c != null) {
				if (pruner != null) {
					pruner.onSelectCandidate(model, c);
				}

				c.applyTo(plan);
			}
		}


		/**
		 * Choose one candidate with one single trip changed.
		 *
		 * @param avoidList combinations to avoid, can be null
		 * @return true if a candidate was selected
		 */
		@Nullable
		public PlanCandidate chooseCandidate(PlanModel model, @Nullable Collection<String[]> avoidList) {

			// empty plan
			if (model.trips() == 0)
				return null;

			boolean[] mask = new boolean[model.trips()];

			IntList options = new IntArrayList();

			// only select trips that are allowed to change
			for (int i = 0; i < model.trips(); i++) {
				if (modes.contains(model.getTripMode(i))) {
					options.add(i);
				}
			}

			if (avoidList != null) {
				String[] current = model.getCurrentModes();

				IntListIterator it = options.iterator();

				outer:
				while (it.hasNext()) {
					int idx = it.nextInt();

					for (String m : modes) {
						current[idx] = m;
						if (!avoidList.contains(current)) {
							continue outer;
						}
					}

					// if at least one option is found, idx is not removed
					it.remove();
				}
			}

			if (options.isEmpty())
				return null;

			int idx = options.getInt(rnd.nextInt(options.size()));

			// Set one trip to be modifiable
			mask[idx] = true;

			List<PlanCandidate> candidates = generator.generate(model, modes, mask);

			// Remove based on threshold
			if (pruner != null) {

				OptionalDouble max = candidates.stream().mapToDouble(PlanCandidate::getUtility).max();
				double t = pruner.tripThreshold(model, idx);

				if (max.isPresent() && t >= 0) {
					double threshold = max.getAsDouble() - t;
					candidates.removeIf(c -> c.getUtility() < threshold);
				}

				pruner.pruneCandidates(model, candidates, rnd);
			}

			// Remove options that are the same as the current mode
			if (requireDifferentModes)
				candidates.removeIf(c -> Objects.equals(c.getMode(idx), model.getTripMode(idx)));

			// Remove avoided combinations
			if (avoidList != null) {
				String[] current = model.getCurrentModes();

				candidates.removeIf(c -> {
					current[idx] = c.getMode(idx);
					return avoidList.contains(current);
				});
			}

			PlanCandidate selected = selector.select(candidates);

			log.debug("Candidates for person {} at trip {}: {} | selected {}", model.getPerson(), idx, candidates, selected);

			return selected;
		}
	}

}
