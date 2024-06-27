package org.matsim.application.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOptions;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Prepare command to generate Counts from the BASt traffic count data.
 * If you want to use manual matched counts, you have to follow a name convention. Unfortunately one count station counts vehicles in
 * both directions For example:
 * The station id is 001. The count direction 1 (Column 'HiRi1') is E (east) and the count direction 2 (Column 'HiRi2') is W (west)
 * If you want to match the count values of the east-lane to matsim link 'my_link_1' add a entry in the .csv like this:
 * row1 : 001_1; my_link_1
 * <p>
 * If you want to ignore stations just paste the station id into the .csv. Both count directions of the station will be ignored.
 *
 * @author hzoerner
 */
@CommandLine.Command(name = "counts-from-bast", description = "Creates MATSim from BASt Stundenwerte.txt")
public class CreateCountsFromBAStData implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateCountsFromBAStData.class);
	@CommandLine.Option(names = "--road-types", description = "Define on which roads counts are created")
	private final List<String> roadTypes = List.of("motorway", "primary", "trunk");
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();
	@CommandLine.Mixin
	private final CountsOptions counts = new CountsOptions();
	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions("EPSG:25832");
	@CommandLine.Option(names = "--network", description = "path to MATSim network", required = true)
	private String network;
	@CommandLine.Option(names = "--primary-data", description = "path to BASt Bundesstraßen-'Stundenwerte'-.txt file", required = true)
	private Path primaryData;
	@CommandLine.Option(names = "--motorway-data", description = "path to BASt Bundesautobahnen-'Stundenwerte'-.txt file", required = true)
	private Path motorwayData;
	@CommandLine.Option(names = "--station-data", description = "path to default BASt count station .csv", required = true)
	private Path stationData;
	@CommandLine.Option(names = "--search-range", description = "range for the buffer around count stations, in which links are queried", defaultValue = "50")
	private double searchRange;
	@CommandLine.Option(names = "--year", description = "Year of counts", required = true)
	private int year;
	@CommandLine.Option(names = "--output", description = "Output counts path", defaultValue = "counts-from-bast.xml.gz")
	private Path output;

	public static void main(String[] args) {
		new CreateCountsFromBAStData().execute(args);
	}

	@Override
	public Integer call() {
		Map<String, BAStCountStation> stations = readBAStCountStations(stationData, shp, counts);

		// Assigns link ids in the station objects
		matchBAStWithNetwork(network, stations, counts, crs);

//		clean(stations);

		readHourlyTrafficVolume(primaryData, stations);
		readHourlyTrafficVolume(motorwayData, stations);

		log.info("+++++++ Map aggregated traffic volumes to count stations +++++++");

		Counts<Link> mmCounts = new Counts<>();
		mmCounts.setYear(year);
		mmCounts.setName("BASt Counts");
		mmCounts.setSource("Bundesanstalt für Straßenwesen");
		mmCounts.setDescription("Aggregated daily traffic volumes for car and freight traffic.");

		stations.values().forEach(station -> mapTrafficVolumeToCount(station, mmCounts));

		log.info("+++++++ Write MATSim counts to {} +++++++", output);
		new CountsWriter(mmCounts).write(output.toString());


		return 0;
	}

	private void mapTrafficVolumeToCount(BAStCountStation station, Counts<Link> counts) {

		if (!station.hasMatchedLink())
			return;

		if (station.getMivTrafficVolume().isEmpty()) {
			log.warn("No traffic counts available for station {}", station.getName());
			return;
		}

		MeasurementLocation<Link> multiModeCount = counts.createAndAddMeasureLocation(station.getMatchedLink().getId(), station.getName() + "_" + station.getDirection());

		Measurable carVolume = multiModeCount.createVolume(TransportMode.car);
		Measurable freightVolume = multiModeCount.createVolume(TransportMode.truck);

		var mivTrafficVolumes = station.getMivTrafficVolume();
		var freightTrafficVolumes = station.getFreightTrafficVolume();

		for (String hour : mivTrafficVolumes.keySet()) {
			int h = Integer.parseInt(hour);
			Double mivAtHour = mivTrafficVolumes.get(hour);
			Double freightAtHour = freightTrafficVolumes.get(hour);

			carVolume.setAtHour(h, mivAtHour);
			freightVolume.setAtHour(h, freightAtHour);
		}
	}

	private void readHourlyTrafficVolume(Path pathToDisaggregatedData, Map<String, BAStCountStation> stations) {

		log.info("+++++++ Start reading traffic volume data +++++++");

		List<String> hours = new ArrayList<>();
		for (int i = 1; i < 25; i++) {
			String asString = i < 10 ? "0" : "";
			asString += Integer.toString(i);
			hours.add(asString);
		}

		List<CSVRecord> preFilteredRecords = null;

		Path input = pathToDisaggregatedData;
		FileSystem fs;
		BufferedReader reader;

		// Try to use file inside zip file
		if (input.getFileName().toString().endsWith(".zip")) {
			try {
				fs = FileSystems.newFileSystem(input, ClassLoader.getSystemClassLoader());
				try (Stream<Path> stream = Files.walk(fs.getPath("/"))) {
					Optional<Path> opt = stream.filter(p -> !p.toString().equals("/")).findFirst();
					if (opt.isPresent())
						input = opt.get();

					reader = Files.newBufferedReader(input, StandardCharsets.ISO_8859_1);
				}
			} catch (IOException e) {
				log.warn("Error processing zip file", e);
				return;
			}
		} else {

			try {
				reader = IOUtils.getBufferedReader(input.toUri().toURL(), StandardCharsets.ISO_8859_1);
			} catch (IOException e) {
				log.error("Error creating buffered reader by IOUtils", e);
				return;
			}
		}

		try {
			CSVParser records = CSVFormat
				.Builder.create()
				.setAllowMissingColumnNames(true)
				.setDelimiter(';')
				.setHeader()
				.build()
				.parse(reader);

			Set<String> keys = stations.keySet().stream()
				.map(key -> key.replaceAll("_1", "").replaceAll("_2", ""))
				.collect(Collectors.toSet());

			// Filter for weekday Tuesday-Wednesday
			preFilteredRecords = StreamSupport.stream(records.spliterator(), false)
				.filter(row -> keys.contains(row.get("Zst")))
				.filter(row -> {
					int day;
					try {
						day = Integer.parseInt(row.get("Wotag").replace(" ", ""));
					} catch (NumberFormatException e) {
						log.warn("Error parsing week day number: {}", row.get("Wotag"));
						return false;
					}
					return day > 1 && day < 5;
				})
				.collect(Collectors.toList());

			if (preFilteredRecords == null || preFilteredRecords.isEmpty()) {
				log.warn("Records read from {} don't contain the stations ... ", pathToDisaggregatedData);
				return;
			}

			log.info("+++++++ Start aggregation of traffic volume data +++++++");

			Set<String> stationIds = preFilteredRecords.stream()
				.map(row -> row.get("Zst"))
				.collect(Collectors.toSet());

			for (String number : stationIds) {
				BAStCountStation direction1 = stations.get(number + "_1");
				BAStCountStation direction2 = stations.get(number + "_2");

				String mivCol1 = "KFZ_" + direction1.getDirectionField();
				String mivCol2 = "KFZ_" + direction2.getDirectionField();
				String freightCol1 = "Lkw_" + direction1.getDirectionField();
				String freightCol2 = "Lkw_" + direction2.getDirectionField();

				//Use station name from 'direction1' because names of 'direction1' and 'direction2' are the same.
				log.info("Process data for count station {}", direction1.getName());

				List<CSVRecord> allEntriesOfStation = preFilteredRecords.stream()
					.filter(row -> row.get("Zst").equals(number)).toList();

				for (String hour : hours) {
					var hourlyTrafficVolumes = allEntriesOfStation.stream()
						.filter(row -> row.get("Stunde").replace("\"", "")
							.equals(hour)).toList();

					if (hourlyTrafficVolumes.isEmpty()) {
						log.warn("No volume for station {} at hour {}", direction1.getName(), hour);
						continue;
					}

					double divisor = hourlyTrafficVolumes.size();
					double sumMiv1 = hourlyTrafficVolumes.stream()
						.mapToDouble(row -> Double.parseDouble(row.get(mivCol1)))
						.sum();

					double sumMiv2 = hourlyTrafficVolumes.stream()
						.mapToDouble(row -> Double.parseDouble(row.get(mivCol2)))
						.sum();

					double meanMiv1 = sumMiv1 / divisor;
					double meanMiv2 = sumMiv2 / divisor;

					direction1.getMivTrafficVolume().put(hour, meanMiv1);
					direction2.getMivTrafficVolume().put(hour, meanMiv2);

					//Same procedure for freight
					double sumFreight1 = hourlyTrafficVolumes.stream()
						.mapToDouble(row -> Double.parseDouble(row.get(freightCol1)))
						.sum();

					double sumFreight2 = hourlyTrafficVolumes.stream()
						.mapToDouble(row -> Double.parseDouble(row.get(freightCol2)))
						.sum();

					double meanFreight1 = sumFreight1 / divisor;
					double meanFreight2 = sumFreight2 / divisor;

					direction1.getFreightTrafficVolume().put(hour, meanFreight1);
					direction2.getFreightTrafficVolume().put(hour, meanFreight2);
				}
			}
		} catch (IOException e) {
			log.error("Error reading hourly volumes", e);
		}
	}

	private void match(Network network, NetworkIndex<BAStCountStation> index, BAStCountStation station, CountsOptions counts) {

		Id<Link> manuallyMatched = counts.isManuallyMatched(station.getId());
		Link matched;
		if (manuallyMatched != null) {

			//Check if link is in the network
			if (!network.getLinks().containsKey(manuallyMatched))
				throw new RuntimeException("Manual matched station link " + manuallyMatched + " is not in the network!");

			matched = network.getLinks().get(manuallyMatched);
		} else {

			matched = index.query(station);

			if (matched == null) {
				station.setHasNoMatchedLink();
				log.warn("Could not match station {}", station.getName());
				return;
			}

		}
		station.setMatchedLink(matched);
		index.remove(matched);
	}

	private List<Predicate<Link>> createRoadTypeFilter(List<String> types) {

		List<Predicate<Link>> filter = new ArrayList<>();

		for (String type : types) {

			Predicate<Link> p = link -> {
				var attr = link.getAttributes().getAttribute("type");
				if (attr == null)
					return true;

				Pattern pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE);
				return pattern.matcher(attr.toString()).find();
			};

			filter.add(p);
		}
		return filter;
	}

	private void matchBAStWithNetwork(String pathToNetwork, Map<String, BAStCountStation> stations, CountsOptions countsOption, CrsOptions crs) {

		if (crs.getTargetCRS() != null)
			throw new RuntimeException("Please don't specify --target-crs. Only use --input-crs to determinate the network crs!");

		Network filteredNetwork;

		List<Predicate<Link>> roadTypeFilter = createRoadTypeFilter(roadTypes);

		{
			Network network = NetworkUtils.readNetwork(pathToNetwork);
			NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
			filter.addLinkFilter(link -> link.getAllowedModes().contains(TransportMode.car));
			filter.addLinkFilter(link -> roadTypeFilter.stream().anyMatch(predicate -> predicate.test(link)));

			filteredNetwork = filter.applyFilters();
		}

		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", crs.getInputCRS());
		NetworkIndex<BAStCountStation> index = new NetworkIndex<>(filteredNetwork, searchRange, station -> {
			Coord coord = station.getCoord();
			Coord transform = coordinateTransformation.transform(coord);
			return MGC.coord2Point(transform);
		});

		index.addLinkFilter((link, station) -> {
			String linkDir = BAStCountStation.getLinkDirection(link.link());
			String stationDir = station.getDirection();
			return linkDir.contains(stationDir);
		});

		log.info("+++++++ Match BASt stations with network +++++++");
		for (var station : stations.values())
			match(filteredNetwork, index, station, countsOption);
	}

	private Map<String, BAStCountStation> readBAStCountStations(Path pathToAggregatedData, ShpOptions shp, CountsOptions counts) {

		List<BAStCountStation> stations = new ArrayList<>();

		// The original bast file has windows encoding
		try (BufferedReader reader = Files.newBufferedReader(pathToAggregatedData, StandardCharsets.ISO_8859_1)) {

			CSVParser records = CSVFormat
				.Builder.create()
				.setAllowMissingColumnNames(true)
				.setDelimiter(';')
				.setHeader()
				.build()
				.parse(reader);

			for (CSVRecord row : records) {

				String id = row.get("DZ_Nr");
				String name = row.get("DZ_Name");

				if (counts.isIgnored(id) || counts.isIgnored(name))
					continue;

				String dir1 = row.get("Hi_Ri1");
				String dir2 = row.get("Hi_Ri2");

				String x = row.get("Koor_UTM32_E").replace(".", "");
				String y = row.get("Koor_UTM32_N").replace(".", "");

				Coord coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));

				BAStCountStation direction1 = new BAStCountStation(id + "_1", name, "R1", dir1, coord);
				BAStCountStation direction2 = new BAStCountStation(id + "_2", name, "R2", dir2, coord);
				stations.add(direction1);
				stations.add(direction2);
			}
		} catch (IOException e) {
			log.error("Error reading count stations", e);
			throw new RuntimeException("Station data could not be read in ...");
		}

		Set<String> ignored = counts.getIgnored();
		final Predicate<BAStCountStation> optFilter = station -> !ignored.contains(station.getId().replaceAll("_1", "")
			.replaceAll("_2", ""));

		final Predicate<BAStCountStation> shpFilter;
		if (shp.getShapeFile() != null) {
			// default input is set to lat lon
			ShpOptions.Index index = shp.createIndex(shp.getShapeCrs(), "_");
			CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", shp.getShapeCrs());
			shpFilter = station -> index.contains(transformation.transform(station.getCoord()));
		} else
			shpFilter = (station) -> true;

		// Return filtered map with id as key and station as value
		return stations.stream()
			.filter(optFilter.and(shpFilter))
			.collect(Collectors.toMap(
				BAStCountStation::getId, Function.identity()
			));
	}
}
