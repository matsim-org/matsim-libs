package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.modal.ModalInjector;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.SplittableRandom;

/**
 * Estimates drt trips based only daily averages. No spatial or temporal differentiation is taken into account for the estimate.
 * This estimator is suited for small scenarios with few vehicles and trips and consequently few data points.
 */
public class BasicDrtEstimator implements DrtEstimator, IterationEndsListener {

	private static final Logger log = LogManager.getLogger(BasicDrtEstimator.class);

	private final DrtEventSequenceCollector collector;
	private final DrtEstimatorConfigGroup config;

	private final DrtSpeedUpParams speedUpParams;

	/**
	 * Currently valid estimates.
	 */
	private GlobalEstimate currentEst;

	private RegressionResults fare;

	private final SplittableRandom rnd = new SplittableRandom();

	@Inject
	public BasicDrtEstimator(ModalInjector injector) {
		//zones = injector.getModal(DrtZonalSystem.class);
		collector = injector.getModal(DrtEventSequenceCollector.class);
		config = injector.getModal(DrtEstimatorConfigGroup.class);

		DrtConfigGroup drtConfig = injector.get(DrtConfigGroup.class);
		speedUpParams = drtConfig.getDrtSpeedUpParams().orElse(null);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// Speed-up iteration need to be ignored for the estimates
		if (speedUpParams != null &&
				DrtSpeedUp.isTeleportDrtUsers(speedUpParams, event.getServices().getConfig().controler(), event.getIteration())) {
			return;
		}

		GlobalEstimate est = new GlobalEstimate();

		int n = 0;

		for (DrtEventSequenceCollector.EventSequence seq : collector.getPerformedRequestSequences().values()) {

			if (seq.getPickedUp().isPresent() && seq.getDroppedOff().isPresent()) {

				double waitTime = seq.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
				est.waitTime.addValue(waitTime);

				double unsharedTime = seq.getSubmitted().getUnsharedRideTime();
				double travelTime = seq.getDroppedOff().get().getTime() - seq.getPickedUp().get().getTime();

				est.detour.addValue(travelTime / unsharedTime);

				double fare = seq.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();
				est.fare.addData(seq.getSubmitted().getUnsharedRideDistance(), fare);
				n++;
			}
		}

		// At least some data points are required
		if (n <= 3)
			return;

		fare = est.fare.regress();

		if (currentEst == null) {
			est.meanWait = est.waitTime.getMean();
			est.stdWait = est.waitTime.getStandardDeviation();
			est.meanDetour = est.detour.getMean();
			est.stdDetour = est.detour.getStandardDeviation();
		} else {
			est.meanWait = config.decayFactor * est.waitTime.getMean() + (1 - config.decayFactor) * currentEst.waitTime.getMean();
			est.stdWait = config.decayFactor * est.waitTime.getStandardDeviation() + (1 - config.decayFactor) * currentEst.waitTime.getStandardDeviation();
			est.meanDetour = config.decayFactor * est.detour.getMean() + (1 - config.decayFactor) * currentEst.detour.getMean();
			est.stdDetour = config.decayFactor * est.detour.getStandardDeviation() + (1 - config.decayFactor) * currentEst.detour.getStandardDeviation();
		}

		log.info("Calculated {}", est);
		currentEst = est;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {

		if (currentEst == null)
			return new Estimate(route.getDistance() * config.defaultDetourFactor,
					route.getDirectRideTime() * config.defaultDetourFactor, config.defaultWaitTime, 0);

		double fare = 0;
		if (this.fare != null)
			fare = this.fare.getParameterEstimate(0) + this.fare.getParameterEstimate(1) * route.getDistance();


		double detour = Math.max(1, rnd.nextGaussian(currentEst.meanDetour, config.randomization * currentEst.stdDetour));
		double waitTime = Math.max(0, rnd.nextGaussian(currentEst.meanWait, config.randomization * currentEst.stdWait));

		return new Estimate(route.getDistance() * detour, route.getDirectRideTime() * detour, waitTime, fare);
	}

	/**
	 * Helper class to hold statistics.
	 */
	private static final class GlobalEstimate {

		private final SummaryStatistics waitTime = new SummaryStatistics();
		private final SummaryStatistics detour = new SummaryStatistics();
		private final SimpleRegression fare = new SimpleRegression(true);

		private double meanWait;
		private double stdWait;
		private double meanDetour;
		private double stdDetour;

		@Override
		public String toString() {
			return "GlobalEstimate{" +
					"meanWait=" + meanWait +
					", stdWait=" + stdWait +
					", meanDetour=" + meanDetour +
					", stdDetour=" + stdDetour +
					'}';
		}
	}
}
