package org.matsim.contrib.drt.estimator.impl;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.estimator.DrtOnlineEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Map;
import java.util.SplittableRandom;

/**
 * When this estimator is used, explicit simulations of DRT will be carried out intermittently. During the iterations where no
 * DRT is simulated, estimated DRT will be used. This estimator will replace the original DRT speed-up module
 * Estimates drt trips based only daily averages. No spatial or temporal differentiation is taken into account for the estimate.
 * This estimator is suited for small scenarios with few vehicles and trips and consequently few data points.
 */
public class OnlineSimulationBasedDrtEstimator implements DrtOnlineEstimator, IterationEndsListener {

	private static final Logger log = LogManager.getLogger(OnlineSimulationBasedDrtEstimator.class);

	private final DrtEventSequenceCollector collector;
	private final DrtEstimatorParams config;
	private final DrtConfigGroup drtConfig;
	private final DrtEstimator initial;

	private final SplittableRandom rnd = new SplittableRandom();
	/**
	 * Currently valid estimates.
	 */
	private GlobalEstimate currentEst;

	public OnlineSimulationBasedDrtEstimator(DrtEventSequenceCollector collector, DrtEstimator initial,
											 DrtEstimatorParams config, DrtConfigGroup drtConfig) {
		//zones = injector.getModal(DrtZonalSystem.class);
		this.collector = collector;
		this.initial = initial;
		this.config = config;
		this.drtConfig = drtConfig;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// Speed-up iteration need to be ignored for the estimates
		if (drtConfig.getDrtSpeedUpParams().isPresent() &&
			DrtSpeedUp.isTeleportDrtUsers(drtConfig.getDrtSpeedUpParams().get(),
				event.getServices().getConfig().controller(), event.getIteration())) {
			return;
		}

		GlobalEstimate est = new GlobalEstimate();

		int n = 0;

		int nRejections = collector.getRejectedRequestSequences().size();
		int nSubmitted = collector.getRequestSubmissions().size();

		for (DrtEventSequenceCollector.EventSequence seq : collector.getPerformedRequestSequences().values()) {

			Map<Id<Person>, DrtEventSequenceCollector.EventSequence.PersonEvents> personEvents = seq.getPersonEvents();
			for (Map.Entry<Id<Person>, DrtEventSequenceCollector.EventSequence.PersonEvents> entry : personEvents.entrySet()) {
				if (entry.getValue().getPickedUp().isPresent() && entry.getValue().getDroppedOff().isPresent()) {
					double waitTime = entry.getValue().getPickedUp().get().getTime() - seq.getSubmitted().getTime();
					est.waitTime.addValue(waitTime);

					double unsharedTime = seq.getSubmitted().getUnsharedRideTime();
					double travelTime = entry.getValue().getDroppedOff().get().getTime() - entry.getValue().getPickedUp().get().getTime();

					est.detour.addValue(travelTime / unsharedTime);

					double fare = seq.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();
					est.fare.addData(seq.getSubmitted().getUnsharedRideDistance(), fare);
					n++;
				}
			}
		}

		// At least some data points are required
		if (n <= 3)
			return;

		double rejectionRate = (double) nRejections / nSubmitted;

		if (currentEst == null) {
			est.meanWait = est.waitTime.getMean();
			est.stdWait = est.waitTime.getStandardDeviation();
			est.meanDetour = est.detour.getMean();
			est.stdDetour = est.detour.getStandardDeviation();
			est.rejectionRate = rejectionRate;
		} else {
			est.meanWait = config.decayFactor * est.waitTime.getMean() + (1 - config.decayFactor) * currentEst.waitTime.getMean();
			est.stdWait = config.decayFactor * est.waitTime.getStandardDeviation() + (1 - config.decayFactor) * currentEst.waitTime.getStandardDeviation();
			est.meanDetour = config.decayFactor * est.detour.getMean() + (1 - config.decayFactor) * currentEst.detour.getMean();
			est.stdDetour = config.decayFactor * est.detour.getStandardDeviation() + (1 - config.decayFactor) * currentEst.detour.getStandardDeviation();
			est.rejectionRate = config.decayFactor * rejectionRate + (1 - config.decayFactor) * currentEst.rejectionRate;
		}

		log.info("Calculated {}", est);
		currentEst = est;
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {

		if (currentEst == null) {
			// Same interface, just different binding
			return initial.estimate(route, departureTime);
		}

		double detour = Math.max(1, rnd.nextGaussian(currentEst.meanDetour, config.randomization * currentEst.stdDetour));
		double waitTime = Math.max(0, rnd.nextGaussian(currentEst.meanWait, config.randomization * currentEst.stdWait));

		return new Estimate(route.getDistance() * detour, route.getDirectRideTime() * detour, waitTime, currentEst.rejectionRate);
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
		private double rejectionRate;

		@Override
		public String toString() {
			return "GlobalEstimate{" +
				"meanWait=" + meanWait +
				", stdWait=" + stdWait +
				", meanDetour=" + meanDetour +
				", stdDetour=" + stdDetour +
				", rejectionRate=" + rejectionRate +
				'}';
		}
	}
}
