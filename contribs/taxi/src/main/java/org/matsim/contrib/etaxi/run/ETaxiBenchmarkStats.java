package org.matsim.contrib.etaxi.run;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkStats;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.contrib.etaxi.util.ETaxiStats;
import org.matsim.contrib.etaxi.util.ETaxiStatsCalculator;

import com.google.common.collect.ObjectArrays;

public class ETaxiBenchmarkStats extends TaxiBenchmarkStats {
	public static final String[] HEADER = ObjectArrays.concat(TaxiBenchmarkStats.HEADER, "QueuedTimeRatio_fleetAvg");

	private final SummaryStatistics queuedTimeRatio = new SummaryStatistics();

	public ETaxiBenchmarkStats(Fleet fleet, SubmittedTaxiRequestsCollector requestCollector,
			OutputDirectoryHierarchy controlerIO) {
		super(fleet, requestCollector, controlerIO);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		super.notifyAfterMobsim(event);
		ETaxiStats singleRunEStats = new ETaxiStatsCalculator(fleet.getVehicles().values()).getDailyEStats();
		queuedTimeRatio.addValue(singleRunEStats.getFleetQueuedTimeRatio());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeFile("ebenchmark_stats.txt", HEADER);
	}

	protected CSVLineBuilder createAndInitLineBuilder() {
		return super.createAndInitLineBuilder().addf("%.3f", queuedTimeRatio.getMean());
	}
}
