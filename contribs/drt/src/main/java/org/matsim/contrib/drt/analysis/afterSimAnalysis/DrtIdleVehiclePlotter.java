package org.matsim.contrib.drt.analysis.afterSimAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.StayTaskManager.StayTaskDataEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DrtIdleVehiclePlotter {
	private static final Logger log = LogManager.getLogger(DrtIdleVehiclePlotter.class);
	private final int eventsQueueSize = 1048576 * 32;
	private final double endTime = 30 * 3600;

	private final String eventsFile;
	private final String networkFile;
	private final String outputPath;

	public DrtIdleVehiclePlotter(Path directory) {
		Path eventsPath = glob(directory, "*output_events*", true).orElseThrow(() -> new IllegalStateException("No events file found."));
		Path networkPath = glob(directory, "*output_network*", true).orElseThrow(() -> new IllegalStateException("No network file found."));
		this.eventsFile = eventsPath.toString();
		this.networkFile = networkPath.toString();
		this.outputPath = directory.toString() + "/idle-vehicle-XY-plot.csv";
	}

	public static void main(String[] args) throws IOException {
		// Input in argument: directory of the simulation output
		DrtIdleVehiclePlotter drtIdleVehiclePlotter = new DrtIdleVehiclePlotter(Path.of(args[0]));
		drtIdleVehiclePlotter.run();
	}

	public void run() throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();
		StayTaskManager stayTaskManager = new StayTaskManager();

		ParallelEventsManager eventManager = new ParallelEventsManager(false, eventsQueueSize);

		eventManager.addHandler(stayTaskManager);
		eventManager.initProcessing();

		MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager);
		matsimEventsReader.readFile(eventsFile);

		List<StayTaskManager.StayTaskDataEntry> stayTaskDataEntries = stayTaskManager.getStayTaskDataEntriesList();
		// Adding in the final stay tasks (which ends after the dvrpTaskEnded event)
		Collection<StayTaskManager.StayTaskDataEntry> finalStayTasksEntries = stayTaskManager.getStartedSatyTasksMap().values();
		for (StayTaskManager.StayTaskDataEntry stayTaskDataEntry : finalStayTasksEntries) {
			stayTaskDataEntry.setEndTime(endTime);
			stayTaskDataEntries.add(stayTaskDataEntry);
		}
		System.out.println("there are " + stayTaskDataEntries.size() + " stay tasks in total");
		writeResultIntoCSVFile(stayTaskDataEntries, network, outputPath);
	}

	private void writeResultIntoCSVFile(List<StayTaskDataEntry> stayTaskDataEntries, Network network, String outputFile)
			throws IOException {
		System.out.println("Writing CSV File now");
		FileWriter csvWriter = new FileWriter(outputFile);

		csvWriter.append("Stay Task ID");
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

		for (StayTaskDataEntry stayTaskDataEntry : stayTaskDataEntries) {
			double X = network.getLinks().get(stayTaskDataEntry.getLinkId()).getToNode().getCoord().getX();
			double Y = network.getLinks().get(stayTaskDataEntry.getLinkId()).getToNode().getCoord().getY();
			List<String> elements = new ArrayList<>();

			elements.add(stayTaskDataEntry.getStayTaskId());
			elements.add(Double.toString(X));
			elements.add(Double.toString(Y));
			elements.add(Double.toString(stayTaskDataEntry.getStartTime()));
			elements.add(Double.toString(stayTaskDataEntry.getEndTime()));
			elements.add(stayTaskDataEntry.getPersonId().toString());

			csvWriter.append(String.join(",", elements));
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
