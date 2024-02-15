package org.matsim.application.analysis.noise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;



@CommandLine.Command(
		name = "noise-analysis",
		description = "Noise analysis",
		mixinStandardHelpOptions = true,
		showDefaultValues = true
)
@CommandSpec(
	requireRunDirectory = true,
	produces = {
		"noise-analysisimmission_consideredAgentUnits_damages_receiverPoint_merged_xyt.csv.gz",
	}
)
public class NoiseAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(NoiseAnalysis.class);

//	@CommandLine.Option(names = "--directory", description = "Path to run directory", required = true)
//	private String runDirectory;

	@CommandLine.Option(names = "--receiver-point-gap", description = "The gap between analysis points in meter",
			defaultValue = "250")
	private double receiverPointGap;

	@CommandLine.Option(names = "--noise-barrier", description = "Path to the noise barrier File", defaultValue = "")
	private String noiseBarrierFile;

	@CommandLine.Option(names = "--input-crs", description = "Coordinate Reference System", defaultValue = "EPSG:25832")
	private String crs;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(NoiseAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(NoiseAnalysis.class);

	public static void main(String[] args) {
		new NoiseAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = prepareConfig();

		config.controller().setOutputDirectory(input.getRunDirectory().toString());

		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(new String[]{"h", "w", "home", "work"});
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(new String[]{"h", "w", "home", "work"});
		if (shp.getShapeFile() != null) {
			CoordinateTransformation ct = shp.createInverseTransformation(config.global().getCoordinateSystem());
			double maxX = Double.MIN_VALUE; // Initialization with the opposite min/max
			double maxY = Double.MIN_VALUE;
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			List<Coord> coords = Arrays.stream(shp.getGeometry().getCoordinates()).
					map(c -> new Coord(c.x, c.y)).
					collect(Collectors.toList());
			for (Coord coord : coords) {
				ct.transform(coord);
				double x = coord.getX();
				double y = coord.getY();
				if (x > maxX) {
					maxX = x;
				}

				if (x < minX) {
					minX = x;
				}

				if (y > maxY) {
					maxY = y;
				}

				if (y < minY) {
					minY = y;
				}
			}
			noiseParameters.setReceiverPointsGridMinX(minX);
			noiseParameters.setReceiverPointsGridMinY(minY);
			noiseParameters.setReceiverPointsGridMaxX(maxX);
			noiseParameters.setReceiverPointsGridMaxY(maxY);
		}

		noiseParameters.setNoiseComputationMethod(NoiseConfigGroup.NoiseComputationMethod.RLS19);

		if (!noiseBarrierFile.equals("")) {
			noiseParameters.setNoiseBarriersSourceCRS(config.global().getCoordinateSystem());
			noiseParameters.setConsiderNoiseBarriers(true);
			noiseParameters.setNoiseBarriersFilePath(noiseBarrierFile);
		}

		// ...
		Scenario scenario = ScenarioUtils.loadScenario(config);



		String outputFilePath = output.getPath().getParent().toString();
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputFilePath);
		outputFilePath += "/noise-analysis";
		noiseCalculation.run();

		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "/immissions/", outputFilePath + "/receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();

		final String[] labels = {"immission", "consideredAgentUnits", "damages_receiverPoint"};
		final String[] workingDirectories = {outputFilePath + "/immissions/", outputFilePath + "/consideredAgentUnits/", outputFilePath + "/damages_receiverPoint/"};

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile();
		merger.setReceiverPointsFile(outputFilePath + "/receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();

//		Files.move(Path.of(outputFilePath + "/noise-analysis"), Path.of(outputFilePath));

		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString(), new NoiseConfigGroup());

		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.plans().setInputFile(ApplicationUtils.matchInput("plans", input.getRunDirectory()).toAbsolutePath().toString());
		config.facilities().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}

}
