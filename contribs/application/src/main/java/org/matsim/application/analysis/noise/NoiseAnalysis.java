package org.matsim.application.analysis.noise;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.*;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

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
		"immission_per_hour.%s",
		"damages_receiverPoint_per_hour.%s",
		"damages_receiverPoint_per_day.%s",
		"noise_stats.csv"
	}
)
public class NoiseAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(NoiseAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(NoiseAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(NoiseAnalysis.class);

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private final ConfigOptions co = new ConfigOptions();

	@CommandLine.Mixin
	private final SampleOptions sampleOptions = new SampleOptions();

	@CommandLine.Option(names = "--consider-activities", split = ",", description = "Considered activities for noise calculation." +
		" Use asterisk ('*') for acttype prefixes, if all such acts shall be considered.", defaultValue = "home*,work*,educ*,leisure*")
	private Set<String> consideredActivities;

	@CommandLine.Option(names = "--noise-barrier", description = "Path to the noise barrier File", defaultValue = "")
	private String noiseBarrierFile;

	public static void main(String[] args) {
		new NoiseAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = prepareConfig();

		config.controller().setOutputDirectory(input.getRunDirectory().toString());

		//trying to set noise parameters more explicitly, here...
		//if NoiseConfigGroup was added before. do not override (most) parameters
		boolean overrideParameters = ! ConfigUtils.hasModule(config, NoiseConfigGroup.class);
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);

		if (overrideParameters){
			log.warn("no NoiseConfigGroup was configured before. Will set some standards. You should check the next lines in the log file and the output_config.xml!");
			noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivities.toArray(String[]::new));
			noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities.toArray(String[]::new));

			{
				//the default settings are now actually the same as what we 'override' here, but let's leave it here for clarity.
				Set<String> ignoredNetworkModes = CollectionUtils.stringArrayToSet( new String[]{TransportMode.bike, TransportMode.walk, TransportMode.transit_walk, TransportMode.non_network_walk} );
				noiseParameters.setNetworkModesToIgnoreSet( ignoredNetworkModes );

				String[] hgvIdPrefixes = {"lkw", "truck", "freight"};
				noiseParameters.setHgvIdPrefixesArray( hgvIdPrefixes );
			}

			//use actual speed and not freespeed
			noiseParameters.setUseActualSpeedLevel(true);
			//use the valid speed range (recommended by IK)
			noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);

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
		} else {
			log.warn("will override a few settings in NoiseConfigGroup, as we are now doing postprocessing and do not want any internalization etc." +
				" You should check the next lines in the log file!");
		}

		// we only mean to do postprocessing here, thus no internalization etc
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		//we don't need events (for Dashboard) - spare disk space.
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);
		noiseParameters.setComputeNoiseDamages(true);

		if(! sampleOptions.isSet() && noiseParameters.getScaleFactor() == 1d){
			log.warn("You didn't provide the simulation sample size via command line option --sample-size! This means, noise damages are not scaled!!!");
		} else if (noiseParameters.getScaleFactor() == 1d){
			if (sampleOptions.getSample() == 1d){
				log.warn("Be aware that the noise output is not scaled. This might be unintended. If so, assure to provide the sample size via command line option --sample-size, in the SimWrapperConfigGroup," +
					"or provide the scaleFactor (the inverse of the sample size) in the NoiseConfigGroup!!!");
			}
			noiseParameters.setScaleFactor(sampleOptions.getUpscaleFactor());
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		String outputFilePath = output.getPath().getParent() == null ? "." : output.getPath().getParent().toString();

		log.info("starting " + NoiseOfflineCalculation.class + " with the following parameters:\n"
			+ noiseParameters);

		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputFilePath);
		outputFilePath += "/noise-analysis";
		noiseCalculation.run();

		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "/immissions/", outputFilePath + "/receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();

		MergeNoiseOutput mergeNoiseOutput = new MergeNoiseOutput(Path.of(outputFilePath), config.global().getCoordinateSystem());
		mergeNoiseOutput.run();

		// Total stats
		DecimalFormat df = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.US));
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("noise_stats.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Annual cost rate per pop. unit [€]:", df.format(noiseParameters.getAnnualCostRate()));
			for (Map.Entry<String, Float> labelValueEntry : mergeNoiseOutput.getTotalReceiverPointValues().entrySet()) {
				printer.printRecord("Total " + labelValueEntry.getKey() + " at receiver points", df.format(labelValueEntry.getValue()));
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		return 0;
	}

	private Config prepareConfig() {
		Config config = co.loadConfig(input.getRunDirectory());

		//it is important to match "output_vehicles.xml.gz" specifically, because otherwise dvrpVehicle files might be matched and the code crashes later
		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("output_vehicles.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.plans().setInputFile(ApplicationUtils.matchInput("plans", input.getRunDirectory()).toAbsolutePath().toString());
		config.facilities().setInputFile(ApplicationUtils.matchInput("facilities", input.getRunDirectory()).toAbsolutePath().toString());
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		//ts, aug '24: not sure if and why we need to set 1 thread
		config.global().setNumberOfThreads(1);

		return config;
	}
}
