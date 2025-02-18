package org.matsim.modechoice.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import jakarta.inject.Provider;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import java.util.List;
import java.util.Random;

/**
 * Strategy that simply select from configured generator..
 */
public class SimplePlanSelectionStrategy extends AbstractMultithreadedModule {

	private final Provider<GeneratorContext> generator;

	public SimplePlanSelectionStrategy(GlobalConfigGroup globalConfigGroup,
									   Provider<GeneratorContext> generator) {
		super(globalConfigGroup);
		this.generator = generator;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm(generator.get());
	}

	private static final class Algorithm implements PlanAlgorithm {

		private final GeneratorContext ctx;
		private final Random rnd;

		public Algorithm(GeneratorContext ctx) {
			this.ctx = ctx;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run(Plan plan) {

			PlanModel planModel = ctx.service.getPlanModel(plan);
			List<PlanCandidate> candidates = ctx.generator.generate(planModel);

			if (ctx.pruner != null) {
				ctx.pruner.pruneCandidates(planModel, candidates, rnd);
			}

			PlanCandidate candidate = ctx.selector.select(candidates);

			if (candidate != null) {
				if (ctx.pruner != null) {
					ctx.pruner.onSelectCandidate(planModel, candidate);
				}

				candidate.applyTo(plan);
			}
		}
	}

}
