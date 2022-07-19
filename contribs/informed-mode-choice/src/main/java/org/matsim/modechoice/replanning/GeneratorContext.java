package org.matsim.modechoice.replanning;

import org.matsim.modechoice.search.SingleTripChoicesGenerator;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Inject;

/**
 * Context for each separate thread.
 */
public final class GeneratorContext {

	final TopKChoicesGenerator generator;

	final SingleTripChoicesGenerator singleGenerator;

	final PlanSelector selector;

	@Inject
	public GeneratorContext(TopKChoicesGenerator generator, SingleTripChoicesGenerator singleGenerator, PlanSelector selector) {
		this.generator = generator;
		this.singleGenerator = singleGenerator;
		this.selector = selector;
	}
}
