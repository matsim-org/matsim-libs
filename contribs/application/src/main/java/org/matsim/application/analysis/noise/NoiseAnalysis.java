package org.matsim.application.analysis.noise;

import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

@CommandLine.Command(
	name = "noise-analysis",
	description = "Noise analysis",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
@CommandSpec(
	requireRunDirectory = true,
	produces = {
		"emission_per_day.csv",
		"immission_per_day.%s",
		"immission_per_hour.%s"
	}
)
public class NoiseAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(NoiseAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(NoiseAnalysis.class);

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--consider-activities", split = ",", description = "Considered activities for noise calculation", defaultValue = "h,w,home,work")
	private Set<String> considerActivities;

	@CommandLine.Option(names = "--noise-barrier", description = "Path to the noise barrier File", defaultValue = "")
	private String noiseBarrierFile;

	public static void main(String[] args) {
		new NoiseAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = prepareConfig();

		config.controller().setOutputDirectory(input.getRunDirectory().toString());

		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(considerActivities.toArray(String[]::new));
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(considerActivities.toArray(String[]::new));
		if (shp.getShapeFile() != null) {
			CoordinateTransformation ct = shp.createInverseTransformation(config.global().getCoordinateSystem());

			Envelope bbox = shp.getGeometry().getEnvelopeInternal();

			Coord minCoord = ct.transform(new Coord(bbox.getMinX(), bbox.getMinY()));
			Coord maxCoord = ct.transform(new Coord(bbox.getMaxX(), bbox.getMaxY()));

			noiseParameters.setReceiverPointsGridMinX(minCoord.getX());
			noiseParameters.setReceiverPointsGridMinY(minCoord.getY());
			noiseParameters.setReceiverPointsGridMaxX(maxCoord.getX());
			noiseParameters.setReceiverPointsGridMaxY(maxCoord.getY());
		}

		noiseParameters.setNoiseComputationMethod(NoiseConfigGroup.NoiseComputationMethod.RLS19);

		if (!Objects.equals(noiseBarrierFile, "")) {
			noiseParameters.setNoiseBarriersSourceCRS(config.global().getCoordinateSystem());
			noiseParameters.setConsiderNoiseBarriers(true);
			noiseParameters.setNoiseBarriersFilePath(noiseBarrierFile);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		String outputFilePath = output.getPath().getParent() == null ? "." : output.getPath().getParent().toString();

		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputFilePath);
		outputFilePath += "/noise-analysis";
		noiseCalculation.run();

		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "/immissions/", outputFilePath + "/receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();

		final String[] paths = {outputFilePath + "/immissions/", outputFilePath + "/emissions/"};
		MergeNoiseOutput mergeNoiseOutput = new MergeNoiseOutput(paths, Path.of(outputFilePath), config.global().getCoordinateSystem());
		mergeNoiseOutput.run();


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
