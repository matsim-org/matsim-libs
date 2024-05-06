package org.matsim.application.analysis.emissions;

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
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
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CommandLine.Command(
	name = "air-pollution", description = "General air pollution analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"emissions_total.csv", "emissions_grid_per_day.csv", "emissions_per_link.csv",
		"emissions_per_link_per_m.csv",
		"emissions_grid_per_hour.csv",
		"emissions_vehicle_info.csv",
	}
)
public class AirPollutionAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AirPollutionAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AirPollutionAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AirPollutionAnalysis.class);
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();
	@CommandLine.Mixin
	private SampleOptions sample;
	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
	private double gridSize;

	public static void main(String[] args) {
		new AirPollutionAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

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


		com.google.inject.Injector injector = Injector.createInjector(config, module);

		// Emissions module will be installed to the event handler
		injector.getInstance(EmissionModule.class);

		String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory()).toString();

		EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(3600, 86400);
		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		writeOutput(filteredNetwork, emissionsEventHandler);

		writeTotal(filteredNetwork, emissionsEventHandler);

		writeRaster(filteredNetwork, config, emissionsEventHandler);

		writeTimeDependentRaster(filteredNetwork, config, emissionsEventHandler);

		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());

		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}


	private void writeOutput(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) throws IOException {

		log.info("Emission analysis completed.");

		log.info("Writing output...");

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		CSVPrinter absolute = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link.csv")), CSVFormat.DEFAULT);
		CSVPrinter perMeter = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link_per_m.csv")), CSVFormat.DEFAULT);

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

	/**
	 * Total emissions table.
	 */
	private void writeTotal(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) {

		Object2DoubleMap<Pollutant> sum = new Object2DoubleLinkedOpenHashMap<>();

		DecimalFormat simple = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		simple.setMaximumFractionDigits(2);
		simple.setMaximumIntegerDigits(5);

		DecimalFormat scientific = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		for (Map.Entry<Id<Link>, Map<Pollutant, Double>> e : emissionsEventHandler.getLink2pollutants().entrySet()) {

			if (!network.getLinks().containsKey(e.getKey()))
				continue;
			for (Map.Entry<Pollutant, Double> p : e.getValue().entrySet()) {
				sum.mergeDouble(p.getKey(), p.getValue(), Double::sum);
			}
		}

		try (CSVPrinter total = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_total.csv")), CSVFormat.DEFAULT)) {

			total.printRecord("Pollutant", "kg");
			for (Pollutant p : Pollutant.values()) {
				double val = (sum.getDouble(p) / sample.getSample()) / 1000;
				total.printRecord(p, val < 100_000 && val > 100 ? simple.format(val) : scientific.format(val));
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates the data for the XY-Time plot. The time is fixed and the data is summarized over the run.
	 * Currently only the CO2_Total Values is printed because Simwrapper can handle only one value.
	 */
	private void writeRaster(Network network, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {

		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processHandlerEmissions(emissionsEventHandler.getLink2pollutants(), network, gridSize, 20);

		List<Integer> xLength = rasterMap.values().stream().map(Raster::getXLength).distinct().toList();
		List<Integer> yLength = rasterMap.values().stream().map(Raster::getYLength).distinct().toList();

		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_grid_per_day.csv")),
			CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			String crs = ProjectionUtils.getCRS(network);
			if (crs == null)
				crs = config.network().getInputCRS();
			if (crs == null)
				crs = config.global().getCoordinateSystem();

			// print coordinate system
//			printer.printComment(crs);

			// print header
			printer.print("time");
			printer.print("x");
			printer.print("y");

			printer.print("value");

			printer.println();

			for (int xi = 0; xi < xLength.get(0); xi++) {
				for (int yi = 0; yi < yLength.get(0); yi++) {

					Coord coord = raster.getCoordForIndex(xi, yi);
					double value = rasterMap.get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi);

//					if (value == 0)
//						continue;

					printer.print(0.0);
					printer.print(coord.getX());
					printer.print(coord.getY());

					printer.print(value);

					printer.println();
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
		}
	}

	private void writeTimeDependentRaster(Network network, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {

		TimeBinMap<Map<Pollutant, Raster>> timeBinMap = FastEmissionGridAnalyzer.processHandlerEmissionsPerTimeBin(emissionsEventHandler.getTimeBins(), network, gridSize, 20);

		Map<Pollutant, Raster> firstBin = timeBinMap.getTimeBin(timeBinMap.getStartTime()).getValue();

		List<Integer> xLength = firstBin.values().stream().map(Raster::getXLength).distinct().toList();
		List<Integer> yLength = firstBin.values().stream().map(Raster::getYLength).distinct().toList();

		Raster raster = firstBin.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("emissions_grid_per_hour.csv").toString()),
			CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			String crs = ProjectionUtils.getCRS(network);
			if (crs == null)
				crs = config.network().getInputCRS();
			if (crs == null)
				crs = config.global().getCoordinateSystem();

			// print coordinate system
			printer.printComment(crs);

			// print header
			printer.print("time");
			printer.print("x");
			printer.print("y");

			printer.print("value");

			printer.println();

			for (int xi = 0; xi < xLength.get(0); xi++) {
				for (int yi = 0; yi < yLength.get(0); yi++) {
					for (TimeBinMap.TimeBin<Map<Pollutant, Raster>> timeBin : timeBinMap.getTimeBins()) {

						Coord coord = raster.getCoordForIndex(xi, yi);
						double value = timeBin.getValue().get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi);

						if (value == 0)
							continue;

						printer.print(timeBin.getStartTime());
						printer.print(coord.getX());
						printer.print(coord.getY());

						printer.print(value);

						printer.println();
					}
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
		}

	}

}
