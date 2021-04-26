package org.matsim.application.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(
		name = "summary",
		description = "Run suite of analysis functionality."
)
public class AnalysisSummary implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AnalysisSummary.class);

	@CommandLine.Parameters(arity = "1..2", paramLabel = "INPUT", description = "Input run directory. Specify two directories in order to compare runs.")
	private List<Path> runDirectory;

	@CommandLine.Option(names = "--run-id", defaultValue = "*", description = "Pattern used to match runId", required = true)
	private String runId;

	@CommandLine.Option(names = "--sample-size", description = "Sample size in [0,1] used to upscale the results", required = true)
	private double sampleSize;

	@CommandLine.Option(names = "--run-id-compare", defaultValue = "", description = "Run id to compare with", required = false)
	private String runIdToCompareWith;

	@CommandLine.Option(names = "--home-act-prefix", defaultValue = "home", description = "Prefix to identify home activities")
	private String homeActivityPrefix;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		System.exit(new CommandLine(new AnalysisSummary()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		final String[] helpLegModes = {TransportMode.walk}; // to be able to analyze old runs
		final String modesString = TransportMode.car + "," + TransportMode.pt + "," + TransportMode.bike + "," + TransportMode.walk + "," + TransportMode.ride;

		Scenario scenario1 = loadScenario(runId, runDirectory.get(0), crs);
		Scenario scenario0 = null;

		if (runDirectory.size() > 1)
			scenario0 = loadScenario(runIdToCompareWith, runDirectory.get(1), crs);

		List<AgentFilter> agentFilters = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter("");
		filter1a.preProcess(scenario1);
		agentFilters.add(filter1a);

		List<TripFilter> tripFilters = new ArrayList<>();

		if (shp.getShapeFile() != null) {
			AgentAnalysisFilter filter1b = new AgentAnalysisFilter("residents-in-area");
			filter1b.setZoneFile(shp.getShapeFile().toString());
			filter1b.setRelevantActivityType(homeActivityPrefix);
			filter1b.preProcess(scenario1);
			agentFilters.add(filter1b);

			TripAnalysisFilter tripFilter1a = new TripAnalysisFilter("");
			tripFilter1a.preProcess(scenario1);
			tripFilters.add(tripFilter1a);

			TripAnalysisFilter tripFilter1b = new TripAnalysisFilter("o-and-d-in-area");
			tripFilter1b.setZoneInformation(shp.getShapeFile().toString(), crs.getInputCRS());
			tripFilter1b.preProcess(scenario1);
			tripFilter1b.setBuffer(0.);
			tripFilter1b.setTripConsiderType(TripAnalysisFilter.TripConsiderType.OriginAndDestination);
			tripFilters.add(tripFilter1b);
		}

		final List<VehicleFilter> vehicleFilters = new ArrayList<>();

		vehicleFilters.add(null);

		VehicleAnalysisFilter vehicleAnalysisFilter1 = new VehicleAnalysisFilter("drt-vehicles", "drt", VehicleAnalysisFilter.StringComparison.Contains);
		vehicleFilters.add(vehicleAnalysisFilter1);

		VehicleAnalysisFilter vehicleAnalysisFilter2 = new VehicleAnalysisFilter("pt-vehicles", "tr", VehicleAnalysisFilter.StringComparison.Contains);
		vehicleFilters.add(vehicleAnalysisFilter2);

		List<String> modes = Arrays.asList(modesString.split(","));

		MatsimAnalysis analysis = new MatsimAnalysis();
		analysis.setScenario1(scenario1);
		analysis.setScenario0(scenario0);

		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		analysis.setVehicleFilters(vehicleFilters);

		analysis.setScenarioCRS(crs.getInputCRS());
		analysis.setScalingFactor((int) Math.round(1 / sampleSize));
		analysis.setModes(modes);
		analysis.setHelpLegModes(helpLegModes);

		if (shp.getShapeFile() != null)
			analysis.setZoneInformation(shp.getShapeFile().toString(), crs.getInputCRS(), null);

		analysis.setVisualizationScriptInputDirectory(null);

		analysis.run();

		return 0;
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

	/**
	 * Helper function to glob for a required file.
	 *
	 * @throws IllegalStateException if no file was matched
	 */
	public static String globFile(Path path, String runId, String name) {

		String file = glob(path, runId + ".*" + name + ".*", true).orElseThrow(() -> new IllegalStateException("No " + name + "file found.")).toString();

		log.info("Using {} file: {}", name, file);

		return file;
	}

	/**
	 * Load scenario from a directory using globed patterns.
	 *
	 * @param runId        run id pattern
	 * @param runDirectory path to run directory
	 * @param crs          crs of the scenario
	 */
	public static Scenario loadScenario(String runId, Path runDirectory, CrsOptions crs) {
		log.info("Loading scenario...");

		Path populationFile = glob(runDirectory, runId + ".*plans.*", true).orElseThrow(() -> new IllegalStateException("No plans file found."));
		int index = populationFile.getFileName().toString().indexOf(".");
		if (index == -1)
			index = 0;

		String resolvedRunId = populationFile.getFileName().toString().substring(0, index);
		log.info("Using population {} with run id {}", populationFile, resolvedRunId);

		Path networkFile = glob(runDirectory, runId + ".*network.*", true).orElseThrow(() -> new IllegalStateException("No network file found."));
		log.info("Using network {}", networkFile);

		String facilitiesFile = glob(runDirectory, runId + ".*facilities.*", true).map(Path::toString).orElse(null);
		log.info("Using facilities {}", facilitiesFile);

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS());
		config.controler().setOutputDirectory(runDirectory.toString());
		config.controler().setRunId(resolvedRunId);

		config.plans().setInputFile(populationFile.toString());
		config.network().setInputFile(networkFile.toString());
		config.facilities().setInputFile(facilitiesFile);

		return ScenarioUtils.loadScenario(config);
	}
}
