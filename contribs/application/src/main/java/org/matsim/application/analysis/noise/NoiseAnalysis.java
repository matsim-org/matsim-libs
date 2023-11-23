package org.matsim.application.analysis.noise;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
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
public class NoiseAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(NoiseAnalysis.class);

	@CommandLine.Option(names = "--directory", description = "Path to run directory", required = true)
	private String runDirectory;

	@CommandLine.Option(names = "--runId", description = "Pattern to match runId.", defaultValue = "")
	private String runId;

	@CommandLine.Option(names = "--receiver-point-gap", description = "The gap between analysis points in meter",
			defaultValue = "250")
	private double receiverPointGap;

	@CommandLine.Option(names = "--noise-barrier", description = "Path to the noise barrier File", defaultValue = "")
	private String noiseBarrierFile;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();


	public static void main(String[] args) {
		new NoiseAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());

		if (crs.getInputCRS() == null || crs.getInputCRS().isBlank()) {
			log.error("Input CRS must be set [--input-crs]");
			return 2;
		}

		if (shp.getShapeFile() == null) {
			log.error("Shp file is always required [--shp]");
			return 2;
		}

		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		config.global().setCoordinateSystem(crs.getInputCRS());
		config.controller().setRunId(runId);
		if (!runId.equals("")) {
			config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
			config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		} else {
			config.network().setInputFile(runDirectory + "output_network.xml.gz");
			config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		}
		config.controller().setOutputDirectory(runDirectory);

		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(new String[]{"h", "w", "home", "work"});
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(new String[]{"h", "w", "home", "work"});
		if (shp.getShapeFile() != null) {
			CoordinateTransformation ct = shp.createInverseTransformation(crs.getInputCRS());
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
			noiseParameters.setNoiseBarriersSourceCRS(crs.getInputCRS());
			noiseParameters.setConsiderNoiseBarriers(true);
			noiseParameters.setNoiseBarriersFilePath(noiseBarrierFile);
		}

		// ...
		Scenario scenario = ScenarioUtils.loadScenario(config);

		String outputDirectory = runDirectory + "analysis/";
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();


		String outputFilePath = outputDirectory + "noise-analysis/";
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();

		final String[] labels = {"immission", "consideredAgentUnits", "damages_receiverPoint"};
		final String[] workingDirectories = {outputFilePath + "/immissions/", outputFilePath + "/consideredAgentUnits/", outputFilePath + "/damages_receiverPoint/"};

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile();
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();

		return 0;
	}

}
