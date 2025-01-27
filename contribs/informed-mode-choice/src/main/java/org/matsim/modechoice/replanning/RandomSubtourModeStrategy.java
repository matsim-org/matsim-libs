package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.PlanModelService;

import jakarta.inject.Provider;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Random selection of modes for subtours without any informed decision-making.
 */
public class RandomSubtourModeStrategy extends AbstractMultithreadedModule {

	private static final Logger log = LogManager.getLogger(RandomSubtourModeStrategy.class);

	private final InformedModeChoiceConfigGroup config;
	private final SubtourModeChoiceConfigGroup smc;
	private final Provider<PlanModelService> provider;
	private final Set<String> nonChainBasedModes;
	private final Set<String> switchModes;

	public RandomSubtourModeStrategy(Config config, Provider<PlanModelService> provider) {
		super(config.global());

		this.config = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		this.smc = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);
		this.provider = provider;

		this.nonChainBasedModes = this.config.getModes().stream()
				.filter(m -> !ArrayUtils.contains(smc.getChainBasedModes(), m))
				.collect(Collectors.toSet());

		this.switchModes = new HashSet<>(this.config.getModes());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		return new Algorithm(provider.get());
	}

	private final class Algorithm implements PlanAlgorithm {
		private final Random rnd;
		private final PlanModelService planModelService;

		public Algorithm(PlanModelService planModelService) {
			this.planModelService = planModelService;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {

			PlanModel model = PlanModel.newInstance(plan);

			if (model.trips() == 0)
				return;

			List<String> modes = planModelService.allowedModes(model);

			// Do change single trip on non-chain based modes with certain probability
			if (rnd.nextDouble() < smc.getProbaForRandomSingleTripMode() && SelectSubtourModeStrategy.hasSingleTripChoice(model, nonChainBasedModes)) {

				IntList options = new IntArrayList();

				// only select trips that are allowed to change
				for (int i = 0; i < model.trips(); i++) {
					String m = model.getTripMode(i);
					if (nonChainBasedModes.contains(m) && switchModes.contains(m)) {
						options.add(i);
					}
				}

				while (!options.isEmpty()) {

					int idx = options.removeInt(rnd.nextInt(options.size()));

					String[] current = model.getCurrentModes();

					List<String> candidates = new ArrayList<>(nonChainBasedModes);

					if (config.isRequireDifferentModes())
						candidates.remove(current[idx]);

					while (!candidates.isEmpty()) {

						current[idx] = candidates.remove(rnd.nextInt(candidates.size()));

						// Check further constraints
						if (!planModelService.isValidOption(model, current))
							continue;

						new PlanCandidate(current, -1).applyTo(plan);
						return;
					}
				}
			}

			List<TripStructureUtils.Subtour> subtours = new ArrayList<>(TripStructureUtils.getSubtours(plan, smc.getCoordDistance()));

			// Add whole trip if not already present
			if (subtours.stream().noneMatch(s -> s.getTrips().size() == model.trips())) {
				subtours.add(0, TripStructureUtils.getUnclosedRootSubtour(plan));
			}

			while (!subtours.isEmpty()) {

				TripStructureUtils.Subtour st = subtours.remove(rnd.nextInt(subtours.size()));
				boolean[] mask = new boolean[model.trips()];
				for (int i = 0; i < model.trips(); i++) {
					if (st.getTrips().contains(model.getTrip(i)) && switchModes.contains(model.getTripMode(i)))
						mask[i] = true;
				}

				List<PlanCandidate> candidates = new ArrayList<>();

				// generate all single mode options
				for (String m : modes) {
					String[] option = model.getCurrentModes();

					// Fill with single mode
					for (int i = 0; i < mask.length; i++) {
						if (mask[i])
							option[i] = m;
					}

					// Current option is not added twice
					if (!config.isRequireDifferentModes() || !Arrays.equals(model.getCurrentModesMutable(), option))
						if (planModelService.isValidOption(model, option))
							candidates.add(new PlanCandidate(option, -1));

				}

				if (!candidates.isEmpty()) {
					PlanCandidate select = candidates.get(rnd.nextInt(candidates.size()));
					select.applyTo(plan);
					break;
				}
			}
		}
	}
}
