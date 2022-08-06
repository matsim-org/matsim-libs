package org.matsim.contrib.drt.analysis.afterSimAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.StoppingTaskRecorder.DrtStoppingTaskDataEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DrtVehicleStoppingTaskWriter {
	private static final Logger log = LogManager.getLogger(DrtVehicleStoppingTaskWriter.class);
	private final double endTime = 30 * 3600;

	private final String eventsFile;
	private final String networkFile;
	private final String stayTasksOutputPath;
	private final String stopTasksOutputPath;

	public DrtVehicleStoppingTaskWriter(Path directory) {
		Path eventsPath = glob(directory, "*output_events*", true).orElseThrow(() -> new IllegalStateException("No events file found."));
		Path networkPath = glob(directory, "*output_network*", true).orElseThrow(() -> new IllegalStateException("No network file found."));
		this.eventsFile = eventsPath.toString();
		this.networkFile = networkPath.toString();
		this.stayTasksOutputPath = directory + "/stay-tasks-XY-plot.csv";
		this.stopTasksOutputPath = directory + "/stop-tasks-XY-plot.csv";
	}

	public static void main(String[] args) throws IOException {
		// Input in argument: directory of the simulation output
		DrtVehicleStoppingTaskWriter drtVehicleStoppingTaskWriter = new DrtVehicleStoppingTaskWriter(Path.of(args[0]));
		drtVehicleStoppingTaskWriter.run();
	}

	public void run(DrtTaskType... nonStandardTaskTypes) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();
		StoppingTaskRecorder stoppingTaskRecorder = new StoppingTaskRecorder();

		EventsManager eventManager = EventsUtils.createEventsManager();
		eventManager.addHandler(stoppingTaskRecorder);
		eventManager.initProcessing();

		MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager, nonStandardTaskTypes);
		matsimEventsReader.readFile(eventsFile);
		eventManager.finishProcessing();

		List<DrtStoppingTaskDataEntry> stayTaskDataEntries = stoppingTaskRecorder.getStayTaskDataEntries();
		// Adding in the final stay tasks (which ends after the dvrpTaskEnded event)
		Collection<DrtStoppingTaskDataEntry> finalStayTasksEntries = stoppingTaskRecorder.getStartedStayTasksMap().values();
		for (DrtStoppingTaskDataEntry drtStoppingTaskDataEntry : finalStayTasksEntries) {
			drtStoppingTaskDataEntry.setEndTime(endTime);
			stayTaskDataEntries.add(drtStoppingTaskDataEntry);
		}
		System.out.println("There are " + stayTaskDataEntries.size() + " stay tasks in total");
		System.out.println("There are " + stoppingTaskRecorder.getStopTaskDataEntries().size() + " stop tasks in total");
		writeResultIntoCSVFile(stayTaskDataEntries, network, stayTasksOutputPath);
		writeResultIntoCSVFile(stoppingTaskRecorder.getStopTaskDataEntries(), network, stopTasksOutputPath);
	}

	private void writeResultIntoCSVFile(List<DrtStoppingTaskDataEntry> stayTaskDataEntries, Network network, String outputFile)
			throws IOException {
		System.out.println("Writing CSV File now");
		FileWriter csvWriter = new FileWriter(outputFile);

		csvWriter.append("Task ID");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append(",");
		csvWriter.append("Start Time");
		csvWriter.append(",");
		csvWriter.append("End Time");
		csvWriter.append(",");
		csvWriter.append("Driver Id");
		csvWriter.append("\n");

		for (DrtStoppingTaskDataEntry drtStoppingTaskDataEntry : stayTaskDataEntries) {
			double X = network.getLinks().get(drtStoppingTaskDataEntry.getLinkId()).getToNode().getCoord().getX();
			double Y = network.getLinks().get(drtStoppingTaskDataEntry.getLinkId()).getToNode().getCoord().getY();
			csvWriter.append(drtStoppingTaskDataEntry.getTaskId());
			csvWriter.append(",");
			csvWriter.append(Double.toString(X));
			csvWriter.append(",");
			csvWriter.append(Double.toString(Y));
			csvWriter.append(",");
			csvWriter.append(Double.toString(drtStoppingTaskDataEntry.getStartTime()));
			csvWriter.append(",");
			csvWriter.append(Double.toString(drtStoppingTaskDataEntry.getEndTime()));
			csvWriter.append(",");
			csvWriter.append(drtStoppingTaskDataEntry.getPersonId().toString());
			csvWriter.append("\n");
		}
		csvWriter.flush();
		csvWriter.close();
	}

	/**
	 * Glob pattern from path, if not found tries to go into the parent directory.
	 */
	public static Optional<Path> glob(Path path, String pattern, boolean parent) {
		PathMatcher m = path.getFileSystem().getPathMatcher("glob:" + pattern);
		try {
			Optional<Path> match = Files.list(path).filter(p -> m.matches(p.getFileName())).findFirst();
			// Look one directory higher for required file
			if (match.isEmpty())
				return Files.list(path.getParent()).filter(p -> m.matches(p.getFileName())).findFirst();

			return match;
		} catch (IOException e) {
			log.warn(e);
		}

		return Optional.empty();
	}
}
