package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.modal.ModalInjector;

/**
 * Estimates drt trips based only daily averages. No spatial or temporal differentiation is taken into account for the estimate.
 * This estimator is suited for small scenarios with few vehicles and trips and consequently few data points.
 */
public class BasicDrtEstimator implements DrtEstimator, IterationEndsListener {

	private DrtEventSequenceCollector collector;
	private DefaultMainLegRouter.RouteCreator creator;

	@Inject
	public BasicDrtEstimator(ModalInjector injector) {
		//zones = injector.getModal(DrtZonalSystem.class);
		collector = injector.getModal(DrtEventSequenceCollector.class);
		creator = injector.getModal(DefaultMainLegRouter.RouteCreator.class);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		System.out.println("##################################");
		System.out.println("##################################");
		System.out.println("##################################");

		System.out.println(event);

	}
}
