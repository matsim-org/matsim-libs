package org.matsim.application.analysis.emissions;

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
import org.matsim.application.options.*;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsByVehicleTypeEventHandler;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

@CommandLine.Command(
	name = "air-pollution", description = "General air pollution analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"emissions_total.csv", "emissions_per_link.csv",
		"emissions_per_link_per_m.csv",
		"emissions_grid_per_hour.%s",
		"emissions_vehicle_info.csv",
		"emissions_grid_per_day.%s",
		"emissions_per_vehicle_type.csv",
		"emissions_per_network_mode.csv"
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
	private final ConfigOptions co = new ConfigOptions();

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
		EmissionsByVehicleTypeEventHandler emissionsByVehicleType = new EmissionsByVehicleTypeEventHandler(scenario.getVehicles(), filteredNetwork);

		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.addHandler(emissionsByVehicleType);

		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		writeEmissionsByVehicleType(emissionsByVehicleType);
		writeEmissionsByNetworkMode(emissionsByVehicleType);

		writeOutput(filteredNetwork, emissionsEventHandler);

		writeTotal(filteredNetwork, emissionsEventHandler);

		writeRaster(filteredNetwork, config, emissionsEventHandler);
		writeAvroRaster(filteredNetwork, config, emissionsEventHandler);

		writeTimeDependentAvroRaster(filteredNetwork, config, emissionsEventHandler);
		writeTimeDependentRaster(filteredNetwork, config, emissionsEventHandler);

		return 0;
	}

	private Config prepareConfig() {
		Config config = co.loadConfig(input.getRunDirectory());

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

	private void writeEmissionsByNetworkMode(EmissionsByVehicleTypeEventHandler emissionsByVehicleType) throws IOException {

		log.info("Writing emissions by vehicle type...");
		Map<String, Object2DoubleMap<Pollutant>> pollutants = emissionsByVehicleType.getByNetworkMode();

		CSVPrinter emissionsCSV = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_network_mode.csv")), CSVFormat.DEFAULT);

		emissionsCSV.print("vehicleType");
		emissionsCSV.print("pollutant");
		emissionsCSV.print("value");
		emissionsCSV.println();

		for (Map.Entry<String, Object2DoubleMap<Pollutant>> entry : pollutants.entrySet()) {
			String vehicleTypeId = entry.getKey();
			Object2DoubleMap<Pollutant> emissionMap = entry.getValue();

			for (Pollutant pollutant : Pollutant.values()) {
				double emissionValue = emissionMap.getDouble(pollutant);
				emissionsCSV.print(vehicleTypeId);
				emissionsCSV.print(pollutant);
				emissionsCSV.print(emissionValue * sample.getUpscaleFactor());
				emissionsCSV.println();
			}
		}

		emissionsCSV.close();
	}

	private void writeEmissionsByVehicleType(EmissionsByVehicleTypeEventHandler emissionsByVehicleType) throws IOException {

		log.info("Writing emissions by vehicle type...");
		Map<Id<VehicleType>, Object2DoubleMap<Pollutant>> pollutants = emissionsByVehicleType.getByVehicleType();

		CSVPrinter emissionsCSV = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_vehicle_type.csv")), CSVFormat.DEFAULT);

		emissionsCSV.print("vehicleType");
		emissionsCSV.print("pollutant");
		emissionsCSV.print("value");
		emissionsCSV.println();

		for (Map.Entry<Id<VehicleType>, Object2DoubleMap<Pollutant>> entry : pollutants.entrySet()) {
			Id<VehicleType> vehicleTypeId = entry.getKey();
			Object2DoubleMap<Pollutant> emissionMap = entry.getValue();

			for (Pollutant pollutant : Pollutant.values()) {
				double emissionValue = emissionMap.getDouble(pollutant);
				emissionsCSV.print(vehicleTypeId);
				emissionsCSV.print(pollutant);
				emissionsCSV.print(emissionValue * sample.getUpscaleFactor());
				emissionsCSV.println();
			}
		}

		emissionsCSV.close();
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
				absolute.print(nf.format(emissionValue * sample.getUpscaleFactor()));

				Link link = network.getLinks().get(linkId);
				double emissionPerM = emissionValue / link.getLength();
				perMeter.print(nf.format(emissionPerM * sample.getUpscaleFactor()));
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
				double val = (sum.getDouble(p) * sample.getUpscaleFactor()) / 1000;
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
	private void writeAvroRaster(Network network, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {

		String crs = ProjectionUtils.getCRS(network);
		if (crs == null)
			crs = config.network().getInputCRS();
		if (crs == null)
			crs = config.global().getCoordinateSystem();

		XYTData avroData = new XYTData();
		avroData.setCrs(crs);

		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processHandlerEmissions(emissionsEventHandler.getLink2pollutants(), network, gridSize, 20);
		List<Integer> xLength = rasterMap.values().stream().map(Raster::getXLength).distinct().toList();
		List<Integer> yLength = rasterMap.values().stream().map(Raster::getYLength).distinct().toList();
		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		List<Float> xCoords = new ArrayList<>();
		List<Float> yCoords = new ArrayList<>();
		Map<CharSequence, List<Float>> values = new HashMap<>();
		List<Float> valuesList = new ArrayList<>();
		List<Integer> times = new ArrayList<>();

		times.add(0);

		for (int xi = 0; xi < xLength.get(0); xi++) {
			for (int yi = 0; yi < yLength.get(0); yi++) {
				Coord coord = raster.getCoordForIndex(xi, yi);
				double value = rasterMap.get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi) * sample.getUpscaleFactor();
				if (xi == 0) yCoords.add((float) coord.getY());
				if (yi == 0) xCoords.add((float) coord.getX());
				valuesList.add((float) value);
			}
		}


		values.put(String.valueOf(Pollutant.CO2_TOTAL), valuesList);

		avroData.setYCoords(yCoords);
		avroData.setXCoords(xCoords);
		avroData.setData(values);
		avroData.setTimestamps(times);

		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(avroData.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(output.getPath("emissions_grid_per_day.%s", "avro").toString()), false));
			dataFileWriter.append(avroData);
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

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_grid_per_day.%s", "csv")),
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
					double value = rasterMap.get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi) * sample.getUpscaleFactor();

					if (value == 0)
						continue;

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

		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("emissions_grid_per_hour.%s", "csv").toString()),
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
						double value = timeBin.getValue().get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi) * sample.getUpscaleFactor();

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

	private void writeTimeDependentAvroRaster(Network network, Config config, EmissionsOnLinkEventHandler emissionsEventHandler) {

		TimeBinMap<Map<Pollutant, Raster>> timeBinMap = FastEmissionGridAnalyzer.processHandlerEmissionsPerTimeBin(emissionsEventHandler.getTimeBins(), network, gridSize, 20);

		String crs = ProjectionUtils.getCRS(network);
		if (crs == null)
			crs = config.network().getInputCRS();
		if (crs == null)
			crs = config.global().getCoordinateSystem();

		XYTData avroData = new XYTData();
		avroData.setCrs(crs);

		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processHandlerEmissions(emissionsEventHandler.getLink2pollutants(), network, gridSize, 20);
		List<Integer> xLength = rasterMap.values().stream().map(Raster::getXLength).distinct().toList();
		List<Integer> yLength = rasterMap.values().stream().map(Raster::getYLength).distinct().toList();
		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		List<Float> xCoords = new ArrayList<>();
		List<Float> yCoords = new ArrayList<>();
		Map<CharSequence, List<Float>> values = new HashMap<>();
		List<Float> valuesList = new ArrayList<>();
		List<Integer> times = new ArrayList<>();

		for (TimeBinMap.TimeBin<Map<Pollutant, Raster>> timeBin : timeBinMap.getTimeBins()) {

			boolean isFirst = times.isEmpty();

			times.add((int) timeBin.getStartTime());

			for (int xi = 0; xi < xLength.get(0); xi++) {
				for (int yi = 0; yi < yLength.get(0); yi++) {
					Coord coord = raster.getCoordForIndex(xi, yi);

					if (xi == 0 && isFirst)
						yCoords.add((float) coord.getY());
					if (yi == 0 && isFirst)
						xCoords.add((float) coord.getX());

					double value = timeBin.getValue().get(Pollutant.CO2_TOTAL).getValueByIndex(xi, yi) * sample.getUpscaleFactor();
					valuesList.add((float) value);
				}
			}
		}

		values.put(String.valueOf(Pollutant.CO2_TOTAL), valuesList);

		avroData.setYCoords(yCoords);
		avroData.setXCoords(xCoords);
		avroData.setData(values);
		avroData.setTimestamps(times);

		DatumWriter<XYTData> datumWriter = new SpecificDatumWriter<>(XYTData.class);
		try (DataFileWriter<XYTData> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(avroData.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(output.getPath("emissions_grid_per_hour.%s", "avro").toString()), false));
			dataFileWriter.append(avroData);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
