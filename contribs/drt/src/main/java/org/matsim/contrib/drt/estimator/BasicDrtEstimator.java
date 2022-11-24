package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.modal.ModalInjector;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Estimates drt trips based only daily averages. No spatial or temporal differentiation is taken into account for the estimate.
 * This estimator is suited for small scenarios with few vehicles and trips and consequently few data points.
 */
public class BasicDrtEstimator implements DrtEstimator, IterationEndsListener {

	private final DrtEventSequenceCollector collector;
	private final DefaultMainLegRouter.RouteCreator creator;

	private final DrtEstimatorConfigGroup config;

	@Inject
	public BasicDrtEstimator(ModalInjector injector) {
		//zones = injector.getModal(DrtZonalSystem.class);
		collector = injector.getModal(DrtEventSequenceCollector.class);
		creator = injector.getModal(DefaultMainLegRouter.RouteCreator.class);
		config = injector.getModal(DrtEstimatorConfigGroup.class);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		System.out.println("##################################");
		System.out.println("##################################");
		System.out.println("##################################");

		System.out.println(event);

		GlobalEstimate est = new GlobalEstimate();


		for (DrtEventSequenceCollector.EventSequence seq : collector.getPerformedRequestSequences().values()) {

			double waitTime = seq.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
			double fare = seq.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();

			// TODO: detour factor

			System.out.println(waitTime + " " + fare);

		}

		// TODO: collect over iterations

	}

	public static final class GlobalEstimate {

		private final SummaryStatistics waitTime = new SummaryStatistics();
		private final SummaryStatistics detour = new SummaryStatistics();

		private final SimpleRegression fare = new SimpleRegression(true);

	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		return new Estimate(0, 0, 0, 0);
	}
}
