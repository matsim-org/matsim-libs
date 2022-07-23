package org.matsim.modechoice.replanning;

import org.matsim.modechoice.pruning.CandidatePruner;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Context for each separate thread.
 */
public final class GeneratorContext {

	final TopKChoicesGenerator generator;

	final SingleTripChoicesGenerator singleGenerator;

	final PlanSelector selector;

	@Nullable
	final CandidatePruner pruner;

	@Inject
	public GeneratorContext(TopKChoicesGenerator generator, SingleTripChoicesGenerator singleGenerator, PlanSelector selector, Provider<CandidatePruner> pruner) {
		this.generator = generator;
		this.singleGenerator = singleGenerator;
		this.selector = selector;
		this.pruner = pruner.get();
	}
}
