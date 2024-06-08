package org.matsim.contrib.drt.estimator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.OptionalTime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Analyzes and outputs drt estimates errors metrics based on daily requests.
 */
public final class DrtEstimateAnalyzer implements StartupListener, ShutdownListener, AfterMobsimListener {

	private static final Logger log = LogManager.getLogger(DrtEstimateAnalyzer.class);

	// Might be useful but not needed currently
	//private final DefaultMainLegRouter.RouteCreator creator;
	private final DrtEstimator estimator;
	private final DrtEventSequenceCollector collector;
	private final String mode;

	private CSVPrinter csv;

	public DrtEstimateAnalyzer(DrtEstimator estimator, DrtEventSequenceCollector collector, String mode) {
		this.estimator = estimator;
		this.collector = collector;
		this.mode = mode;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		String filename = event.getServices().getControlerIO().getOutputFilename("drt_estimates_" + mode + ".csv");

		try {
			csv = new CSVPrinter(Files.newBufferedWriter(Path.of(filename), StandardCharsets.UTF_8), CSVFormat.DEFAULT);
			csv.printRecord("iteration",
					"wait_time_mae", "wait_time_err_q5", "wait_time_err_q50", "wait_time_err_q95",
					"travel_time_mae", "travel_time_err_q5", "travel_time_err_q50", "travel_time_err_q95",
					"fare_mae", "fare_err_q5", "fare_err_q50", "fare_err_q95"
			);

		} catch (IOException e) {
			throw new UncheckedIOException("Could not open output file for estimates.", e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			csv.close();
		} catch (IOException e) {
			log.warn("Could not close drt estimate file", e);
		}
	}

	/**
	 * Needs to run before any estimators updates.
	 */
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		try {
			csv.printRecord(calcMetrics(event.getIteration()));
			csv.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return row of metrics for the csv file.
	 */
	private Iterable<Number> calcMetrics(int iteration) {

		DescriptiveStatistics waitTime = new DescriptiveStatistics();
		DescriptiveStatistics travelTime = new DescriptiveStatistics();
		DescriptiveStatistics fare = new DescriptiveStatistics();

		for (DrtEventSequenceCollector.EventSequence seq : collector.getPerformedRequestSequences().values()) {
			Map<Id<Person>, DrtEventSequenceCollector.EventSequence.PersonEvents> personEvents = seq.getPersonEvents();
			for (Map.Entry<Id<Person>, DrtEventSequenceCollector.EventSequence.PersonEvents> entry : personEvents.entrySet()) {
				if (entry.getValue().getPickedUp().isPresent() && entry.getValue().getDroppedOff().isPresent()) {

					// many attributes are not filled, when using the constructor
					DrtRoute route = new DrtRoute(seq.getSubmitted().getFromLinkId(), seq.getSubmitted().getToLinkId());
					route.setDirectRideTime(seq.getSubmitted().getUnsharedRideTime());
					route.setDistance(seq.getSubmitted().getUnsharedRideDistance());

					double valWaitTime = entry.getValue().getPickedUp().get().getTime() - seq.getSubmitted().getTime();
					double valTravelTime = entry.getValue().getDroppedOff().get().getTime() - entry.getValue().getPickedUp().get().getTime();
					double valFare = seq.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();

					DrtEstimator.Estimate estimate = estimator.estimate(route, OptionalTime.defined(seq.getSubmitted().getTime()));

					waitTime.addValue(Math.abs(estimate.waitingTime() - valWaitTime));
					travelTime.addValue(Math.abs(estimate.rideTime() - valTravelTime));
					// TODO add fare estimator?
//					fare.addValue(Math.abs(estimate.fare() - valFare));
				}
			}
		}

		return List.of(
				iteration,
				waitTime.getMean(), waitTime.getPercentile(5), waitTime.getPercentile(50), waitTime.getPercentile(95),
				travelTime.getMean(), travelTime.getPercentile(5), travelTime.getPercentile(50), travelTime.getPercentile(95),
				fare.getMean(), fare.getPercentile(5), fare.getPercentile(50), fare.getPercentile(95)
		);
	}

}
