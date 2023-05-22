package org.matsim.application.analysis.emissions;

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
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.*;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(
	name = "air-pollution", description = "General air pollution analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {"emissions_footprint_csv", "emissions_grid_per_day.csv", "emissions_per_link.csv", "emissions_per_link_per_m.csv", "emissions_vehicle_info.csv"}
)
public class AirPollutionAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AirPollutionAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AirPollutionAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AirPollutionAnalysis.class);
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--use-default-road-types", description = "Add default hbefa_road_type link attributes to the network", defaultValue = "false")
	private boolean useDefaultRoadTypes;

	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
	private double gridSize;

	public static void main(String[] args) {
		new AirPollutionAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (useDefaultRoadTypes)
			addDefaultHbefaTypes(scenario);

		Network filteredNetwork;
		if (shp.isDefined()) {
			ShpOptions.Index index = shp.createIndex(ProjectionUtils.getCRS(scenario.getNetwork()), "_");

			NetworkFilterManager manager = new NetworkFilterManager(scenario.getNetwork(), config.network());
			manager.addLinkFilter(l -> index.contains(l.getCoord()));

			filteredNetwork = manager.applyFilters();
		} else
			filteredNetwork = scenario.getNetwork();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(EmissionModule.class);
			}
		};

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		com.google.inject.Injector injector = Injector.createInjector(config, module);

		// Emissions module will be installed to the event handler
		injector.getInstance(EmissionModule.class);

		String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory()).toString();

		EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(3600);
		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		writeOutput(filteredNetwork, emissionsEventHandler);

		writeRaster(filteredNetwork, emissionsEventHandler);

		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());
//		EmissionsConfigGroup emissionsConfigGroup

		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}

	/**
	 * changes/adds link attributes of the network in the given scenario.
	 *
	 * @param scenario for which to prepare the network
	 */
	private void addDefaultHbefaTypes(Scenario scenario) {
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
//		the type attribute in our network has the prefix "highway" for all links but pt links. we need to delete that because OsmHbefaMapping does not handle that.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			//pt links can be disregarded
			if (!link.getAllowedModes().contains("pt")) {
				NetworkUtils.setType(link, NetworkUtils.getType(link).replaceFirst("highway\\.", ""));
			}
		}
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());
	}

	private void writeOutput(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) throws IOException {

		log.info("Emission analysis completed.");

		log.info("Writing output...");

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		CSVPrinter absolute = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link.csv")), CSVFormat.DEFAULT);
		CSVPrinter perMeter = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link_per_m")), CSVFormat.DEFAULT);

		absolute.print("linkId");
		perMeter.print("linkId");

		for (Pollutant pollutant : Pollutant.values()) {
			absolute.print(pollutant);
			perMeter.print(pollutant + " [g/m]");
		}

		absolute.println();
		perMeter.println();

		Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

		for (Id<Link> linkId : link2pollutants.keySet()) {

			// Link might be filtered
			if (!network.getLinks().containsKey(linkId))
				continue;

			absolute.print(linkId);
			perMeter.print(linkId);

			for (Pollutant pollutant : Pollutant.values()) {
				double emissionValue = 0.;
				if (link2pollutants.get(linkId).get(pollutant) != null) {
					emissionValue = link2pollutants.get(linkId).get(pollutant);
				}
				absolute.print(nf.format(emissionValue));

				Link link = network.getLinks().get(linkId);
				double emissionPerM = emissionValue / link.getLength();
				perMeter.print(nf.format(emissionPerM));
			}

			absolute.println();
			perMeter.println();
		}

		absolute.close();
		perMeter.close();
	}

	private void writeRaster(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) {


		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processHandlerEmissions(emissionsEventHandler.getLink2pollutants(), network, gridSize, 20);

		Set<Pollutant> poll = rasterMap.keySet();

		List<Integer> xLength = rasterMap.values().stream().map(Raster::getXLength).distinct().toList();
		List<Integer> yLength = rasterMap.values().stream().map(Raster::getYLength).distinct().toList();

		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_grid_per_day.csv")), CSVFormat.DEFAULT)) {

			// print header
			printer.print("x");
			printer.print("y");

			for (Pollutant p : poll) {
				printer.print(p.name());
			}

			printer.println();

			for (int xi = 0; xi < xLength.get(0); xi++) {
				for (int yi = 0; yi < yLength.get(0); yi++) {

					Coord coord = raster.getCoordForIndex(xi, yi);

					printer.print(coord.getX());
					printer.print(coord.getY());

					for (Pollutant p : poll) {
						double value = rasterMap.get(p).getValueByIndex(xi, yi);
						printer.print(value);
					}

					printer.println();
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
		}

	}
}
