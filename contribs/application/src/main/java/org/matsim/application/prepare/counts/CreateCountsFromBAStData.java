package org.matsim.application.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	/**
	 * Aggregation used if {@code --aggregate} is not given. It is the weekday filter this command has always applied.
	 */
	private static final List<String> DEFAULT_AGGREGATIONS = List.of("midweek=TUESDAY,WEDNESDAY,THURSDAY");

	/**
	 * The 'Datum' column of the 'Stundenwerte' files, e.g. 230101 for the first of January 2023.
	 */
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

	private static final String OUTPUT_FILE = "counts-from-bast.xml.gz";

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
	@CommandLine.Option(names = "--network-geometries", description = "path to link geometries, as written by the SumoNetworkConverter. Improves the matching of stations to links. Expected to be in the same crs as the network")
	private String networkGeometries;
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
	@CommandLine.Option(names = "--aggregate", description = "one aggregation of the hourly counts, repeatable. " +
		"Syntax: <name>=<DAY>[,<DAY>...][@<from>:<to>[+<from>:<to>...]], e.g. " +
		"saturday=SATURDAY@2023-06-01:2023-08-31. Dates are inclusive and given as yyyy-MM-dd, a day is used if it " +
		"falls into any of the ranges. Without ranges the whole input period is used. Each aggregation is written to " +
		"its own output subdirectory. Default: midweek=TUESDAY,WEDNESDAY,THURSDAY")
	private List<String> aggregationSpecs;
	@CommandLine.Option(names = "--statistic", description = "how the hourly values of the aggregated days are " +
		"combined. MEDIAN is robust against single days with an accident, a closure or a broken detector. " +
		"Candidates: ${COMPLETION-CANDIDATES}", defaultValue = "MEAN")
	private Statistic statistic;
	@CommandLine.Option(names = "--min-days", description = "minimum number of days every hour of a station has to " +
		"rest on for the station to be written. Stations not covering all 24 hours are always skipped", defaultValue = "1")
	private int minDays;
	@CommandLine.Option(names = "--skip-daily-counts", description = "do not write the counts of the single calendar " +
		"days. By default one counts file per day is written to <output>/days/MM/dd.xml.gz, holding the volumes " +
		"actually measured on that day")
	private boolean skipDailyCounts;
	@CommandLine.Option(names = "--output", description = "Output directory, one subdirectory per aggregation", defaultValue = "counts-from-bast")
	private Path output;

	public static void main(String[] args) {
		new CreateCountsFromBAStData().execute(args);
	}

	@Override
	public Integer call() throws IOException {

		//Parse before the expensive reading and map matching, so that a malformed option fails right away
		List<Aggregation> aggregations = Aggregation.parse(
			aggregationSpecs == null || aggregationSpecs.isEmpty() ? DEFAULT_AGGREGATIONS : aggregationSpecs);

		Files.createDirectories(output);

		Map<String, BAStCountStation> stations = readBAStCountStations(stationData, shp, counts);

		// Assigns link ids in the station objects
		matchBAStWithNetwork(network, stations, counts, crs);

		//Only matched stations can carry counts, so the rest is not worth aggregating
		Map<String, BAStCountStation> matched = stations.entrySet().stream()
			.filter(entry -> entry.getValue().hasMatchedLink())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		log.info("+++++++ Matched {} of {} station directions to the network +++++++", matched.size(), stations.size());

		//date -> station id -> hourly volumes of that day, filled while reading, see DailyVolumes
		Map<LocalDate, Map<String, DailyVolumes>> daily = new TreeMap<>();

		readHourlyTrafficVolume(primaryData, matched, aggregations, daily);
		readHourlyTrafficVolume(motorwayData, matched, aggregations, daily);

		for (Aggregation aggregation : aggregations)
			writeAggregation(aggregation, matched);

		if (!skipDailyCounts)
			writeDailyCounts(daily, matched);

		return 0;
	}

	/**
	 * Writes one counts file per aggregation to {@code <output>/<name>/counts-from-bast.xml.gz}.
	 */
	private void writeAggregation(Aggregation aggregation, Map<String, BAStCountStation> stations) throws IOException {

		log.info("+++++++ Aggregate traffic volumes for '{}' over {} +++++++", aggregation.name(), aggregation.describe());

		Counts<Link> counts = new Counts<>();
		counts.setYear(year);
		counts.setName("BASt Counts " + aggregation.name());
		counts.setSource("Bundesanstalt für Straßenwesen");
		counts.setDescription(statistic.name().toLowerCase(Locale.ROOT) + " hourly traffic volumes for car and freight traffic ("
			+ aggregation.describe() + ").");

		int noData = 0;
		int thin = 0;
		for (BAStCountStation station : stations.values()) {

			BAStCountStation.HourlyValues values = station.peekVolumes(aggregation.name());
			if (values == null) {
				noData++;
				continue;
			}

			//Covering all 24 hours says nothing about how many days each of them rests on. A station whose detector
			//was down for most of the period passes that check with a handful of days per hour, and those days are
			//rarely a cross section of the period.
			int fewest = values.fewestDays();
			if (fewest < minDays) {
				log.warn("Station {} rests on only {} days in its thinnest hour of aggregation {}, fewer than the {} required!",
					station.getName(), fewest, aggregation.name(), minDays);
				thin++;
				continue;
			}

			MeasurementLocation<Link> location = counts.createAndAddMeasureLocation(station.getMatchedLink().getId(),
				station.getName() + "_" + station.getDirection() + ", " + station.getId() + "_" + station.getDirectionField());

			Measurable carVolume = location.createVolume(TransportMode.car);
			Measurable freightVolume = location.createVolume(TransportMode.truck);

			for (int hour = 0; hour < BAStCountStation.HOURS_PER_DAY; hour++) {
				carVolume.setAtHour(hour, statistic.apply(values.car()[hour]));
				freightVolume.setAtHour(hour, statistic.apply(values.freight()[hour]));
			}
		}

		//An aggregation whose days are not in the input would produce an empty counts file
		if (counts.getMeasureLocations().isEmpty()) {
			log.warn("Aggregation {} matched no usable data, no counts file is written.", aggregation.name());
			return;
		}

		Path directory = output.resolve(aggregation.name());
		Files.createDirectories(directory);
		Path file = directory.resolve(OUTPUT_FILE);

		log.info("+++++++ Write MATSim counts to {} +++++++", file);
		new CountsWriter(counts).write(file.toString());

		log.info("Aggregation {}: wrote {} stations, skipped {} without data and {} that do not cover all hours on at least {} days.",
			aggregation.name(), counts.getMeasureLocations().size(), noData, thin, minDays);
	}

	/**
	 * Writes one counts file per calendar day to {@code <output>/days/MM/dd.xml.gz}. Other than the aggregated counts
	 * these hold the volumes actually measured on that day, for every day the data covers and not only for the days
	 * the {@link Aggregation}s select.
	 */
	private void writeDailyCounts(Map<LocalDate, Map<String, DailyVolumes>> daily, Map<String, BAStCountStation> stations) throws IOException {

		log.info("+++++++ Start writing daily counts +++++++");

		Path dailyPath = output.resolve("days");
		int incomplete = 0;
		int written = 0;

		for (Map.Entry<LocalDate, Map<String, DailyVolumes>> day : daily.entrySet()) {
			LocalDate date = day.getKey();

			Counts<Link> counts = new Counts<>();
			counts.setYear(date.getYear());
			counts.setName("BASt Counts " + date);
			counts.setSource("Bundesanstalt für Straßenwesen");
			counts.setDescription("Car and freight counts of " + date + ".");

			for (Map.Entry<String, DailyVolumes> entry : day.getValue().entrySet()) {
				DailyVolumes volumes = entry.getValue();

				//A station is only written if it covers the full day, the same rule the aggregation applies
				if (!volumes.isComplete()) {
					incomplete++;
					continue;
				}

				BAStCountStation station = stations.get(entry.getKey());
				MeasurementLocation<Link> location = counts.createAndAddMeasureLocation(station.getMatchedLink().getId(),
					station.getName() + "_" + station.getDirection() + ", " + station.getId() + "_" + station.getDirectionField());

				Measurable car = location.createVolume(TransportMode.car);
				Measurable freight = location.createVolume(TransportMode.truck);

				for (int hour = 0; hour < BAStCountStation.HOURS_PER_DAY; hour++) {
					car.setAtHour(hour, volumes.car()[hour]);
					freight.setAtHour(hour, volumes.freight()[hour]);
				}
			}

			//Days on which no station covers all hours would produce an empty counts file
			if (counts.getMeasureLocations().isEmpty()) {
				log.warn("No station has data for the whole day on {}, no counts file is written.", date);
				continue;
			}

			Path monthPath = dailyPath.resolve(String.format("%02d", date.getMonthValue()));
			Files.createDirectories(monthPath);
			new CountsWriter(counts).write(monthPath.resolve(String.format("%02d", date.getDayOfMonth()) + ".xml.gz").toString());
			written++;
		}

		log.info("Wrote counts for {} of {} days to {}, skipped {} station days with incomplete data.",
			written, daily.size(), dailyPath, incomplete);
	}

	/**
	 * Reads one 'Stundenwerte' file in a single pass, appending every row of a matched station to the aggregations it
	 * belongs to and to the volumes of its day. The file is never held in memory, it is far too large for that.
	 */
	private void readHourlyTrafficVolume(Path pathToDisaggregatedData, Map<String, BAStCountStation> stations,
										 List<Aggregation> aggregations, Map<LocalDate, Map<String, DailyVolumes>> daily) {

		log.info("+++++++ Start reading traffic volume data from {} +++++++", pathToDisaggregatedData);

		// Try to use file inside zip file
		if (pathToDisaggregatedData.getFileName().toString().endsWith(".zip")) {
			try (FileSystem fs = FileSystems.newFileSystem(pathToDisaggregatedData, ClassLoader.getSystemClassLoader())) {

				Path inside;
				try (Stream<Path> stream = Files.walk(fs.getPath("/"))) {
					Optional<Path> opt = stream.filter(Files::isRegularFile).findFirst();
					if (opt.isEmpty()) {
						log.error("Zip file {} does not contain any file.", pathToDisaggregatedData);
						return;
					}
					inside = opt.get();
				}

				try (BufferedReader reader = Files.newBufferedReader(inside, StandardCharsets.ISO_8859_1)) {
					readHourlyTrafficVolume(reader, pathToDisaggregatedData, stations, aggregations, daily);
				}
			} catch (IOException e) {
				log.error("Error processing zip file {}", pathToDisaggregatedData, e);
			}
		} else {
			try (BufferedReader reader = IOUtils.getBufferedReader(pathToDisaggregatedData.toUri().toURL(), StandardCharsets.ISO_8859_1)) {
				readHourlyTrafficVolume(reader, pathToDisaggregatedData, stations, aggregations, daily);
			} catch (IOException e) {
				log.error("Error reading hourly volumes from {}", pathToDisaggregatedData, e);
			}
		}
	}

	private void readHourlyTrafficVolume(BufferedReader reader, Path source, Map<String, BAStCountStation> stations,
										 List<Aggregation> aggregations, Map<LocalDate, Map<String, DailyVolumes>> daily) throws IOException {

		CSVParser records = CSVFormat
			.Builder.create()
			.setAllowMissingColumnNames(true)
			.setDelimiter(';')
			.setHeader()
			.build()
			.parse(reader);

		long used = 0;
		long malformed = 0;
		boolean warnedAboutYear = false;
		boolean warnedAboutWeekday = false;

		for (CSVRecord row : records) {

			String number = row.get("Zst").trim();

			//Both directions of a station are read from the same row, but either of them may have been dropped by the
			//shape file, the ignore list or the map matching
			BAStCountStation direction1 = stations.get(number + "_1");
			BAStCountStation direction2 = stations.get(number + "_2");

			if (direction1 == null && direction2 == null)
				continue;

			LocalDate date = parseDate(row.get("Datum"));
			int hour = parseHour(row.get("Stunde"));

			if (date == null || hour < 0) {
				malformed++;
				continue;
			}

			if (!warnedAboutYear && date.getYear() != year) {
				log.warn("{} contains data of {}, but --year is {}. The counts are written with the given year.", source, date.getYear(), year);
				warnedAboutYear = true;
			}

			//The day of week is derived from the date, so that date ranges and weekdays cannot contradict each other
			if (!warnedAboutWeekday && !weekdayMatches(row, date)) {
				log.warn("Column 'Wotag' of {} disagrees with the day of week of its 'Datum'. The date is used.", source);
				warnedAboutWeekday = true;
			}

			for (BAStCountStation station : List.of(direction1, direction2)) {

				if (station == null)
					continue;

				double car = parseVolume(row, "KFZ_" + station.getDirectionField());
				double freight = parseVolume(row, "Lkw_" + station.getDirectionField());

				if (Double.isNaN(car) || Double.isNaN(freight))
					continue;

				for (Aggregation aggregation : aggregations) {
					if (aggregation.covers(date))
						station.getVolumes(aggregation.name()).add(hour, car, freight);
				}

				if (!skipDailyCounts) {
					DailyVolumes volumes = daily.computeIfAbsent(date, d -> new HashMap<>())
						.computeIfAbsent(station.getId(), id -> DailyVolumes.create());
					volumes.car()[hour] = car;
					volumes.freight()[hour] = freight;
				}

				used++;
			}
		}

		if (used == 0)
			log.warn("Records read from {} don't contain any of the matched stations ... ", source);

		log.info("Read {} station hours from {}, skipped {} rows that could not be parsed.", used, source, malformed);
	}

	/**
	 * Whether the 'Wotag' column, 1 for Monday to 7 for Sunday, agrees with the parsed date. Not all deliveries fill
	 * it correctly, which is why the date decides.
	 */
	private static boolean weekdayMatches(CSVRecord row, LocalDate date) {

		try {
			return Integer.parseInt(row.get("Wotag").trim()) == date.getDayOfWeek().getValue();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Parses the 'Datum' column, which is yyMMdd, e.g. 230101 for the first of January 2023.
	 */
	private static LocalDate parseDate(String date) {

		try {
			return LocalDate.parse(date.trim(), DATE_FORMAT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Parses the 'Stunde' column, which is 01 to 24, and returns it as hour of day 0 to 23. BASt hour 01 is the hour
	 * from 00:00 to 01:00.
	 */
	private static int parseHour(String hour) {

		try {
			int parsed = Integer.parseInt(hour.replace("\"", "").trim());
			return parsed >= 1 && parsed <= BAStCountStation.HOURS_PER_DAY ? parsed - 1 : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Reads one volume column, NaN if it is missing or not a number. A row where one of the columns cannot be read
	 * carries no usable hour for that direction.
	 */
	private static double parseVolume(CSVRecord row, String column) {

		try {
			return Double.parseDouble(row.get(column).trim());
		} catch (IllegalArgumentException e) {
			return Double.NaN;
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

		Map<Id<Link>, Geometry> geometries = readNetworkGeometries(crs);

		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", crs.getInputCRS());
		NetworkIndex<BAStCountStation> index = new NetworkIndex<>(filteredNetwork, geometries, searchRange, station -> {
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

	/**
	 * Reads the link geometries, if given. They are expected to be in the same crs as the network.
	 */
	private Map<Id<Link>, Geometry> readNetworkGeometries(CrsOptions crs) {

		if (networkGeometries == null)
			return Map.of();

		try {
			CoordinateReferenceSystem networkCRS = CRS.decode(crs.getInputCRS());
			Map<Id<Link>, Geometry> geometries = NetworkIndex.readGeometriesFromSumo(networkGeometries,
				CRS.findMathTransform(networkCRS, networkCRS, true));

			log.info("Read {} link geometries from {}", geometries.size(), networkGeometries);
			return geometries;
		} catch (IOException | FactoryException | TransformException e) {
			throw new RuntimeException("Link geometries could not be read from " + networkGeometries, e);
		}
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

	/**
	 * How the hourly values of the aggregated days are combined into one profile.
	 */
	enum Statistic {

		MEAN {
			@Override
			double apply(DoubleArrayList values) {

				double sum = 0;
				for (int i = 0; i < values.size(); i++)
					sum += values.getDouble(i);

				return sum / values.size();
			}
		},

		/**
		 * Robust against single days with an accident, a closure or a broken detector, which would otherwise shift the
		 * whole profile.
		 */
		MEDIAN {
			@Override
			double apply(DoubleArrayList values) {

				double[] sorted = values.toDoubleArray();
				Arrays.sort(sorted);

				int middle = sorted.length / 2;

				return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
			}
		};

		abstract double apply(DoubleArrayList values);
	}

	/**
	 * Hourly car and freight volumes of one station on one day. Hours the data does not cover stay {@link Double#NaN}.
	 */
	private record DailyVolumes(double[] car, double[] freight) {

		static DailyVolumes create() {

			double[] car = new double[BAStCountStation.HOURS_PER_DAY];
			double[] freight = new double[BAStCountStation.HOURS_PER_DAY];

			Arrays.fill(car, Double.NaN);
			Arrays.fill(freight, Double.NaN);

			return new DailyVolumes(car, freight);
		}

		boolean isComplete() {

			for (int hour = 0; hour < BAStCountStation.HOURS_PER_DAY; hour++) {
				if (Double.isNaN(car[hour]) || Double.isNaN(freight[hour]))
					return false;
			}

			return true;
		}
	}
}
