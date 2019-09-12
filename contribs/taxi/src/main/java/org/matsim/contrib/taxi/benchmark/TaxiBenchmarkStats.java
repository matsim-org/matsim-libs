package org.matsim.contrib.taxi.benchmark;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListener;
import org.matsim.contrib.taxi.util.stats.TaxiStats;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculator;
import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;

public class TaxiBenchmarkStats
		implements ShutdownListener, MobsimBeforeCleanupListener, QSimScopeObjectListener<Fleet> {
	public static final String[] HEADER = { "n", "m", //
			"PassWaitTime_avg", //
			"PassWaitTime_95%ile", //
			"PassWaitTime_max", //
			"EmptyDriveRatio_fleetAvg", //
			"StayRatio_fleetAvg" };

	private final OutputDirectoryHierarchy controlerIO;

	private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
	private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
	private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

	private final SummaryStatistics emptyDriveRatio = new SummaryStatistics();
	private final SummaryStatistics stayRatio = new SummaryStatistics();

	private Fleet fleet;

	public TaxiBenchmarkStats(OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO;
	}

	@Override
	public void objectCreated(Fleet fleet) {
		this.fleet = fleet;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		TaxiStats singleRunStats = new TaxiStatsCalculator(fleet.getVehicles().values()).getDailyStats();

		passengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMean());
		pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getPercentile(95));
		maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMax());

		emptyDriveRatio.addValue(singleRunStats.getFleetEmptyDriveRatio());
		stayRatio.addValue(singleRunStats.getFleetStayRatio());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeFile("benchmark_stats.txt", HEADER);
	}

	protected void writeFile(String file, String[] header) {
		try (CompactCSVWriter writer = new CompactCSVWriter(
				IOUtils.getBufferedWriter(controlerIO.getOutputFilename(file)))) {
			writer.writeNext(header);
			writer.writeNext(createAndInitLineBuilder());
		}
	}

	protected CSVLineBuilder createAndInitLineBuilder() {
		return new CSVLineBuilder()//
				.addf("%.1f", passengerWaitTime.getMean())
				.addf("%.0f", pc95PassengerWaitTime.getMean())
				.addf("%.0f", maxPassengerWaitTime.getMean())
				.addf("%.3f", emptyDriveRatio.getMean())
				.addf("%.3f", stayRatio.getMean());
	}
}
