package org.matsim.contrib.ev.withinday.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * Tracks detailed and aggregated information on the electric vehilce charging
 * processes and attempts.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayChargingAnalysisListener implements IterationEndsListener {
	static public final String PROCESSES_FILE = "wevc_charging_processes.csv";
	static public final String ATTEMPTS_FILE = "wevc_charging_attempts.csv";
	static public final String AGGREGATED_FILE = "wevc_analysis.csv";

	private final OutputDirectoryHierarchy outputHierarchy;
	private final WithinDayChargingAnalysisHandler handler;

	public WithinDayChargingAnalysisListener(WithinDayChargingAnalysisHandler handler,
			OutputDirectoryHierarchy outputHierarchy) {
		this.outputHierarchy = outputHierarchy;
		this.handler = handler;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			String chargingPath = outputHierarchy.getIterationFilename(event.getIteration(), PROCESSES_FILE);
			BufferedWriter chargingWriter = IOUtils.getBufferedWriter(chargingPath);

			chargingWriter.write(String.join(";", Arrays.asList( //
					"person_id", "vehicle_id", "process_index", "attempts", "successful", "start_time", "end_time"))
					+ "\n");

			for (var item : handler.getChargingProcessItems()) {
				chargingWriter.write(String.join(";", Arrays.asList( //
						item.personId().toString(), item.vehicleId().toString(), String.valueOf(item.processIndex()),
						String.valueOf(item.attempts()), String.valueOf(item.successful()),
						String.valueOf(item.startTime()), String.valueOf(item.endTime()))) + "\n");
			}

			chargingWriter.close();

			String attemptsPath = outputHierarchy.getIterationFilename(event.getIteration(), ATTEMPTS_FILE);
			BufferedWriter attemptsWriter = IOUtils.getBufferedWriter(attemptsPath);

			attemptsWriter.write(String.join(";", Arrays.asList( //
					"person_id", "vehicle_id", "process_index", "attempt_index", "successful", "start_time",
					"update_time", "end_time",
					"queue_start_time", "queue_end_time", "queued", "charging_start_time", "charging_end_time",
					"charged", "charger_id", "initial_charger_id", "enroute", "spontaneous", "energy_kWh")) + "\n");

			for (var item : handler.getChargingAttemptItems()) {
				attemptsWriter.write(String.join(";", Arrays.asList( //
						item.personId().toString(), item.vehicleId().toString(), String.valueOf(item.processIndex()),
						String.valueOf(item.attemptIndex()), String.valueOf(item.successful()),
						String.valueOf(item.startTime()), String.valueOf(item.updateTime()),
						String.valueOf(item.endTime()),
						String.valueOf(item.queueingStartTime()), String.valueOf(item.queueingEndTime()),
						String.valueOf(item.queued()), String.valueOf(item.chargingStartTime()),
						String.valueOf(item.chargingEndTime()), String.valueOf(item.charged()),
						item.chargerId().toString(), item.initialChargerId().toString(),
						String.valueOf(item.enroute()),
						String.valueOf(item.spontaneous()),
						String.valueOf(item.energy_kWh()))) + "\n");
			}

			attemptsWriter.close();

			writeAggregatedStatistics(event.getIteration());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeAggregatedStatistics(int iteration) throws IOException {
		int chargingPersonsCount = 0;
		int failedPersonsCount = 0;

		int chargingItemsCount = 0;
		int failedItemsCount = 0;

		int chargingAttempsCount = 0;
		int failedAttemptsCount = 0;

		IdSet<Person> personIds = new IdSet<>(Person.class);
		IdSet<Person> failedIds = new IdSet<>(Person.class);

		for (var item : handler.getChargingProcessItems()) {
			chargingItemsCount++;
			personIds.add(item.personId());

			if (!item.successful()) {
				failedItemsCount++;
				failedIds.add(item.personId());
			}
		}

		chargingPersonsCount = personIds.size();
		failedPersonsCount = failedIds.size();

		for (var item : handler.getChargingAttemptItems()) {
			chargingAttempsCount++;

			if (!item.successful()) {
				failedAttemptsCount++;
			}
		}

		File outputFile = new File(outputHierarchy.getOutputFilename(AGGREGATED_FILE));
		boolean writeHeader = !outputFile.exists();

		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outputFile.toString());

		if (writeHeader) {
			writer.write(String.join(";", Arrays.asList( //
					"iteration", "persons", "failed_persons", "processes", "failed_processes", "attempts",
					"failed_attempts"))
					+ "\n");
		}

		writer.write(String.join(";", Arrays.asList( //
				String.valueOf(iteration), String.valueOf(chargingPersonsCount), String.valueOf(failedPersonsCount),
				String.valueOf(chargingItemsCount), String.valueOf(failedItemsCount),
				String.valueOf(chargingAttempsCount), String.valueOf(failedAttemptsCount))) + "\n");

		writer.close();
	}
}
