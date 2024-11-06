package org.matsim.application.analysis.accessibility;

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.avro.XYTData;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

@CommandLine.Command(
	name = "accessibility-offline", description = "Offline Accessibility analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"xxx.csv"
	}
)
public class AccessibilityAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AccessibilityAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AccessibilityAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AccessibilityAnalysis.class);

//	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
//	private double gridSize;

	public static void main(String[] args) {
		new AccessibilityAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max ).setBoundingBoxLeft(min ).setBoundingBoxRight(max );
		acg.setUseParallelization(false);


		Scenario scenario = ScenarioUtils.loadScenario(config);

		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		ActivityOption supermarket = scenario.getActivityFacilities().getFactory().createActivityOption("supermarket");
		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(200, 0));
		facility1.addActivityOption(supermarket);
		opportunities.addActivityFacility(facility1);

		ActivityFacility facility2 = opportunities.getFactory().createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(200, 200));
		facility2.addActivityOption(supermarket);

		opportunities.addActivityFacility(facility2);
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);


		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", input.getRunDirectory()).toString();

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, "supermarket");
		builder.build().run();


		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());

		config.vehicles().setVehiclesFile(null);
		config.network().setInputFile(ApplicationUtils.matchInput("output_network.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}


}
