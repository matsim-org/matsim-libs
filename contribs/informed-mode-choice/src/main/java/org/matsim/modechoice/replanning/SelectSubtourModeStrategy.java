package org.matsim.modechoice.replanning;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
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

import jakarta.inject.Provider;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main strategy for informed mode choice.
 */
public class SelectSubtourModeStrategy extends AbstractMultithreadedModule {

	private static final Logger log = LogManager.getLogger(SelectSubtourModeStrategy.class);

	private final InformedModeChoiceConfigGroup config;
	private final SubtourModeChoiceConfigGroup smc;
	private final Provider<GeneratorContext> generator;

	private final Set<String> nonChainBasedModes;
	private final Set<String> switchModes;

	public SelectSubtourModeStrategy(Config config, Provider<GeneratorContext> generator) {
		super(config.global());

		this.config = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		this.smc = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);
		this.generator = generator;

		this.nonChainBasedModes = this.config.getModes().stream()
				.filter(m -> !ArrayUtils.contains(smc.getChainBasedModes(), m))
				.collect(Collectors.toSet());

		this.switchModes = new HashSet<>(this.config.getModes());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		GeneratorContext context = generator.get();
		return new Algorithm(context, SelectSingleTripModeStrategy.newAlgorithm(context, context.selector, context.pruner, nonChainBasedModes, config.isRequireDifferentModes()));
	}

	/**
	 * Checks if change single trip mode would have an option to choose from. That is the case, when not all trips are chain based.
	 */
	public static boolean hasSingleTripChoice(PlanModel model, Collection<String> nonChainBasedModes) {

		for (int i = 0; i < model.trips(); i++) {
			if (nonChainBasedModes.contains(model.getTripMode(i)))
				return true;
		}

		return false;
	}

	private final class Algorithm implements PlanAlgorithm {

		private final GeneratorContext ctx;
		private final SelectSingleTripModeStrategy.Algorithm singleTrip;
		private final Random rnd;

		public Algorithm(GeneratorContext ctx, SelectSingleTripModeStrategy.Algorithm singleTrip) {
			this.ctx = ctx;
			this.singleTrip = singleTrip;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {

			PlanModel model = ctx.service.getPlanModel(plan);
			model.setPlan(plan);

			if (model.trips() == 0)
				return;


			// Do change single trip on non-chain based modes with certain probability
			if (rnd.nextDouble() < smc.getProbaForRandomSingleTripMode() && hasSingleTripChoice(model, nonChainBasedModes)) {
				PlanCandidate c = singleTrip.chooseCandidate(model);
				if (c != null) {
					c.applyTo(plan);
					return;
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

				List<String[]> options = new ArrayList<>();

				// generate all single mode options
				for (String m : config.getModes()) {
					String[] option = model.getCurrentModes();

					// Fill with single mode
					for (int i = 0; i < mask.length; i++) {
						if (mask[i])
							option[i] = m;
					}

					options.add(option);
				}

				List<PlanCandidate> singleModeCandidates = ctx.generator.generatePredefined(model, options);
				singleModeCandidates.removeIf(p -> p.getUtility() == Double.NEGATIVE_INFINITY);

				// Single modes are also added
				Set<PlanCandidate> candidateSet = new LinkedHashSet<>(singleModeCandidates);

				// one could either allow all modes here or only non chain based
				// config switch might be useful to investigate which option is better

				// execute best k modes, setting k to 0 disables this, then this strategy is similar to random subtour
				if (config.getTopK() > 0) {
					candidateSet.addAll(ctx.generator.generate(model, nonChainBasedModes, mask));
				}

				// candidates are unique after this
				List<PlanCandidate> candidates = new ArrayList<>(candidateSet);

				if (config.isRequireDifferentModes())
					candidates.removeIf(c -> Arrays.equals(c.getModes(), model.getCurrentModesMutable()));

				// Pruning is applied based on current plan estimate
				// best k generator applied pruning already, but the single trip options need to be checked again
				if (ctx.pruner != null) {

					OptionalDouble max = candidates.stream().mapToDouble(PlanCandidate::getUtility).max();
					double t = ctx.pruner.planThreshold(model);

					if (max.isPresent() && t >= 0) {
						double threshold = max.getAsDouble() - t;
						candidates.removeIf(c -> c.getUtility() < threshold);
					}

					// Only applied at the end
					ctx.pruner.pruneCandidates(model, candidates, rnd);
				}

				if (!candidates.isEmpty()) {

					PlanCandidate select = ctx.selector.select(candidates);
					if (select != null) {
						if (ctx.pruner != null) {
							ctx.pruner.onSelectCandidate(model, select);
						}

						select.applyTo(plan);
						break;
					}
				}
			}
		}
	}
}
