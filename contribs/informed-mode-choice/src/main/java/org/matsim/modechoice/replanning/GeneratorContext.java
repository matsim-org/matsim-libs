package org.matsim.modechoice.replanning;

import org.matsim.core.router.PlanRouter;
import org.matsim.modechoice.PlanModelService;
import org.matsim.modechoice.pruning.CandidatePruner;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Context for each separate thread.
 */
public final class GeneratorContext {

	public final TopKChoicesGenerator generator;
	public final SingleTripChoicesGenerator singleGenerator;
	public final PlanModelService service;
	public final PlanSelector selector;
	public final PlanRouter planRouter;

	@Nullable
	public final CandidatePruner pruner;

	@Inject
	public GeneratorContext(TopKChoicesGenerator generator, SingleTripChoicesGenerator singleGenerator,
							PlanModelService service, PlanSelector selector, PlanRouter planRouter, Provider<CandidatePruner> pruner) {
		this.generator = generator;
		this.singleGenerator = singleGenerator;
		this.service = service;
		this.selector = selector;
		this.planRouter = planRouter;
		this.pruner = pruner.get();
	}
}
