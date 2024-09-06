package org.matsim.contrib.drt.analysis.afterSimAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

public class DrtVehicleStoppingTaskWriter {
	private static final Logger log = LogManager.getLogger(DrtVehicleStoppingTaskWriter.class);

	private final String eventsFile;
	private final String networkFile;
	private final String stoppingTasksOutputPath;
	private final StoppingTaskRecorder stoppingTaskRecorder;

	/**
	 * Constructor with default StoppingTaskRecorder. Stay task and stop task will be written down. *
	 */
	public DrtVehicleStoppingTaskWriter(Path directory) {
		Path eventsPath = glob(directory, "*output_events*").orElseThrow(() -> new IllegalStateException("No events file found."));
		Path networkPath = glob(directory, "*output_network*").orElseThrow(() -> new IllegalStateException("No network file found."));
		this.eventsFile = eventsPath.toString();
		this.networkFile = networkPath.toString();
		this.stoppingTasksOutputPath = directory + "/drt-stopping-tasks-XY-plot.csv";
		this.stoppingTaskRecorder = new StoppingTaskRecorder();
	}

	/**
	 * Use this function to add customized task type to be included in the analysis. The customized task type should *
	 * also be added to the arguments of the run function (as they are not standard task type)*
	 */
	public DrtVehicleStoppingTaskWriter addingCustomizedTaskToAnalyze(Task.TaskType customizedTaskType) {
		stoppingTaskRecorder.addExtraTaskTypeToAnalyze(customizedTaskType);
		return this;
	}

	public static void main(String[] args) throws IOException {
		// Input in argument: directory of the simulation output
		new DrtVehicleStoppingTaskWriter(Path.of(args[0])).run();
	}

	public void run(DrtTaskType... nonStandardTaskTypes) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();

		EventsManager eventManager = EventsUtils.createEventsManager();
		eventManager.addHandler(stoppingTaskRecorder);
		eventManager.initProcessing();

		MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager, nonStandardTaskTypes);
		matsimEventsReader.readFile(eventsFile);
		eventManager.finishProcessing();

		List<StoppingTaskRecorder.DrtTaskInformation> drtStoppingTaskEntries = stoppingTaskRecorder.getDrtTasksEntries();

		log.info("There are " + drtStoppingTaskEntries.size() + " drt stopping tasks in total");

		writeResultIntoCSVFile(drtStoppingTaskEntries, network, stoppingTasksOutputPath);
	}

	private void writeResultIntoCSVFile(List<StoppingTaskRecorder.DrtTaskInformation> drtStoppingTaskEntries, Network network, String outputFile)
			throws IOException {
		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(outputFile));

		csvWriter.append("Task_name");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append(",");
		csvWriter.append("Start_time");
		csvWriter.append(",");
		csvWriter.append("End_time");
		csvWriter.append(",");
		csvWriter.append("Driver_id");
		csvWriter.append(",");
		csvWriter.append("Occupancy_at_task_start");
		csvWriter.append("\n");

		for (StoppingTaskRecorder.DrtTaskInformation drtStoppingTaskDataEntry : drtStoppingTaskEntries) {
			double X = network.getLinks().get(drtStoppingTaskDataEntry.getLinkId()).getToNode().getCoord().getX();
			double Y = network.getLinks().get(drtStoppingTaskDataEntry.getLinkId()).getToNode().getCoord().getY();
			csvWriter.append(drtStoppingTaskDataEntry.getTaskName());
			csvWriter.append(",");
			csvWriter.append(Double.toString(X));
			csvWriter.append(",");
			csvWriter.append(Double.toString(Y));
			csvWriter.append(",");
			csvWriter.append(Double.toString(drtStoppingTaskDataEntry.getStartTime()));
			csvWriter.append(",");
			csvWriter.append(Double.toString(drtStoppingTaskDataEntry.getEndTime()));
			csvWriter.append(",");
			csvWriter.append(drtStoppingTaskDataEntry.getVehicleId().toString());
			csvWriter.append(",");
			csvWriter.append(Integer.toString(drtStoppingTaskDataEntry.getOccupancy()));
			csvWriter.append("\n");
		}
		csvWriter.flush();
		csvWriter.close();
	}

	/**
	 * Glob pattern from path, if not found tries to go into the parent directory.
	 */
	public static Optional<Path> glob(Path path, String pattern) {
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
