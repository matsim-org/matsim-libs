package org.matsim.application.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
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
 * See description.
 *
 * @author hzoerner
 */
@CommandLine.Command(name = "counts-from-bast", description = "Creates MATSim from BASt Stundenwerte.txt")
public class CreateCountsFromBAStData implements MATSimAppCommand {

	@CommandLine.Option(names = "--network", description = "path to MATSim network", required = true)
	private String network;

	@CommandLine.Option(names = "--road-types", description = "Define on which roads counts are created")
	private final List<String> roadTypes = List.of("motorway", "primary", "trunk");

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

	@CommandLine.Option(names = "--car-output", description = "output car counts path", defaultValue = "car-counts-from-bast.xml.gz")
	private Path carOutput;

	@CommandLine.Option(names = "--freight-output", description = "output freight counts path", defaultValue = "freight-counts-from-bast.xml.gz")
	private Path freightOutput;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private final CountsOption counts = new CountsOption();

	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions("EPSG:25832");

	private static final Logger log = LogManager.getLogger(CreateCountsFromBAStData.class);

	public static void main(String[] args) {
		new CreateCountsFromBAStData().execute(args);
	}

	@Override
	public Integer call() {
		Map<String, BAStCountStation> stations = readBAStCountStations(stationData, shp, counts);

		// Assigns link ids in the station objects
		matchBAStWithNetwork(network, stations, counts, crs);

		clean(stations);

		readHourlyTrafficVolume(primaryData, stations);
		readHourlyTrafficVolume(motorwayData, stations);

		log.info("+++++++ Map aggregated traffic volumes to count stations +++++++");
		Counts<Link> miv = new Counts<>();
		Counts<Link> freight = new Counts<>();
		stations.values().forEach(station -> mapTrafficVolumeToCount(station, miv, freight));

		miv.setYear(year);
		freight.setYear(year);

		log.info("+++++++ Write MATSim counts to {} and {} +++++++", carOutput, freightOutput);
		new CountsWriter(miv).write(carOutput.toString());
		new CountsWriter(freight).write(freightOutput.toString());

		return 0;
	}

	private void clean(Map<String, BAStCountStation> stations) {

		log.info("+++++++ Check stations for duplicates and missing link ids +++++++");

		//Set<Id<Link>>
		Set<Id<Link>> uniqueIds = stations.values().stream()
				.filter(BAStCountStation::hasMatchedLink)
				.filter(BAStCountStation::hasOppLink)
				.map(station -> List.of(station.getMatchedLink(), station.getOppLink()))
				.flatMap(Collection::stream)
				.map(Link::getId)
				.collect(Collectors.toSet());

		List<String> remove = new ArrayList<>();

		for (BAStCountStation station : stations.values()) {

			if (!station.hasMatchedLink() || !station.hasOppLink()) {
				remove.add(station.getId());
				continue;
			}

			var matched = station.getMatchedLink();
			var opp = station.getOppLink();

			if (matched.equals(opp)) {
				remove.add(station.getId());
				continue;
			}

			if (uniqueIds.contains(matched.getId()) && uniqueIds.contains(opp.getId())) {
				uniqueIds.remove(matched.getId());
				uniqueIds.remove(opp.getId());
			} else {
				remove.add(station.getId());
			}
		}

		remove.forEach(stations.keySet()::remove);

		log.info("+++++++ Removed {} stations +++++++", remove.size());
	}

	private void mapTrafficVolumeToCount(BAStCountStation station, Counts<Link> miv, Counts<Link> freight) {

		if (station.getMivTrafficVolume1().isEmpty()) {
			log.warn("No traffic counts available for station {}", station.getName());
			return;
		}

		Count<Link> mivCount = miv.createAndAddCount(station.getMatchedLink().getId(), station.getName());
		Count<Link> mivCountOpp = miv.createAndAddCount(station.getOppLink().getId(), station.getName());

		Count<Link> freightCount = freight.createAndAddCount(station.getMatchedLink().getId(), station.getName());
		Count<Link> freightCountOpp = freight.createAndAddCount(station.getOppLink().getId(), station.getName());

		var mivTrafficVolumes = station.getMivTrafficVolume1();
		var mivTrafficVolumesOpp = station.getMivTrafficVolume2();

		var freightTrafficVolumes = station.getFreightTrafficVolume1();
		var freightTrafficVolumesOpp = station.getFreightTrafficVolume2();

		for (String hour : mivTrafficVolumes.keySet()) {

			if (hour.startsWith("0")) hour.replace("0", "");
			int h = Integer.parseInt(hour);
			mivCount.createVolume(h, mivTrafficVolumes.get(hour));
			mivCountOpp.createVolume(h, mivTrafficVolumesOpp.get(hour));

			freightCount.createVolume(h, freightTrafficVolumes.get(hour));
			freightCountOpp.createVolume(h, freightTrafficVolumesOpp.get(hour));
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

		try{
			CSVParser records = CSVFormat
					.Builder.create()
					.setAllowMissingColumnNames(false)
					.setDelimiter(';')
					.setHeader()
					.build()
					.parse(reader);

			Set<String> keys = stations.keySet();

			// Filter for weekday Tuesday-Wednesday
			preFilteredRecords = StreamSupport.stream(records.spliterator(), false)
					.filter(record -> keys.contains(record.get("Zst")))//ONLY FOR DEBUGGING
					.filter(record -> {
						int day;
						try{
							day = Integer.parseInt(record.get("Wotag").replace(" ", ""));
						} catch (NumberFormatException e){
							log.warn("Error parsing week day number: {}", record.get("Wotag"));
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
					.map(record -> record.get("Zst"))
					.collect(Collectors.toSet());

			for (String number : stationIds) {
				BAStCountStation station = stations.get(number);
				String direction1 = station.getMatchedDir();
				String direction2 = station.getOppDir();

				String mivCol1 = "KFZ_" + direction1;
				String mivCol2 = "KFZ_" + direction2;
				String freightCol1 = "Lkw_" + direction1;
				String freightCol2 = "Lkw_" + direction2;

				log.info("Process data for count station {}", station.getName());

				List<CSVRecord> allEntriesOfStation = preFilteredRecords.stream()
						.filter(record -> record.get("Zst").equals(number)).toList();

				for (String hour : hours) {
					var hourlyTrafficVolumes = allEntriesOfStation.stream()
							.filter(record -> record.get("Stunde").replace("\"", "")
									.equals(hour)).toList();

					if (hourlyTrafficVolumes.isEmpty()) {
						log.warn("No volume for station {} at hour {}", station.getName(), hour);
						continue;
					}

					double divisor = hourlyTrafficVolumes.size();
					double sumMiv1 = hourlyTrafficVolumes.stream()
							.mapToDouble(record -> Double.parseDouble(record.get(mivCol1)))
							.sum();

					double sumMiv2 = hourlyTrafficVolumes.stream()
							.mapToDouble(record -> Double.parseDouble(record.get(mivCol2)))
							.sum();

					double meanMiv1 = sumMiv1 / divisor;
					double meanMiv2 = sumMiv2 / divisor;

					station.getMivTrafficVolume1().put(hour, meanMiv1);
					station.getMivTrafficVolume2().put(hour, meanMiv2);

					//Same procedure for freight
					double sumFreight1 = hourlyTrafficVolumes.stream()
							.mapToDouble(record -> Double.parseDouble(record.get(freightCol1)))
							.sum();

					double sumFreight2 = hourlyTrafficVolumes.stream()
							.mapToDouble(record -> Double.parseDouble(record.get(freightCol2)))
							.sum();

					double meanFreight1 = sumFreight1 / divisor;
					double meanFreight2 = sumFreight2 / divisor;

					station.getFreightTrafficVolume1().put(hour, meanFreight1);
					station.getFreightTrafficVolume2().put(hour, meanFreight2);
				}
			}
		} catch (IOException e) {
			log.error("Error reading hourly volumes", e);
		}
	}

	private boolean checkManualMatching(BAStCountStation station, Network network, Map<String, Id<Link>> manualMatched){
		if(manualMatched.size() > 2){
			throw new RuntimeException("Too many manual matched links for station " + station.getName());
		}
		// Check direction matching
		String dir1 = station.getDir1();
		String dir2 = station.getDir2();

		List<String> bastDirections = List.of(dir1, dir2);

		List<String> manualDir = manualMatched.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toList());

		if(!manualDir.containsAll(bastDirections))
			throw new RuntimeException("Wrong direction matching for station " + station.getName());


		//Check if link is in the network
		for(Id<Link> id: manualMatched.values()){

			if (!network.getLinks().containsKey(id))
				throw new RuntimeException("Manual matched station link " + id + " is not in the network!");
		}

		return true;
	}

	private void match(Network network, Index index, BAStCountStation station, CountsOption counts, CoordinateTransformation transformation) {

		Map<String, Id<Link>> manuallyMatched = counts.isManuallyMatched(station.getId());
		Link matched;
		if(manuallyMatched != null) {

			if(!checkManualMatching(station, network, manuallyMatched)){
				station.setHasNoMatchedLink();
				station.setHasNoOppLink();
				return;
			}

			String dir1 = station.getDir1();
			String key1 = dir1 + "_" + station.getId();

			String dir2 = station.getDir2();
			String key2 = dir2 + "_" + station.getId();

			Id<Link> matchedId = manuallyMatched.get(key1);
			matched = network.getLinks().get(matchedId);

			Id<Link> oppId = manuallyMatched.get(key2);
			Link opp = network.getLinks().get(oppId);

			station.setMatchedLink(matched);
			station.setOppLink(opp);

			station.overwriteDirections(dir1, dir2);

			index.remove(matched);
			index.remove(opp);
		} else {

			matched = NetworkUtils.getNearestLink(network, transformation.transform(station.getCoord()));

			if (matched == null) {
				log.debug("Query is used for matching!");
				matched = index.query(station);
				if (matched == null) {
					station.setHasNoMatchedLink();
					log.warn("Could not match station {}", station.getName());
					return;
				}
			}

			station.setMatchedLink(matched);

			Link opp = NetworkUtils.findLinkInOppositeDirection(matched);

			if (opp == null) {
				opp = index.query(station);
				if (opp == null) {
					log.warn("Could not match station {} to an opposite link", station.getName());
					station.setHasNoOppLink();
					return;
				}
			}
			station.setOppLink(opp);

			index.remove(matched);
			index.remove(opp);
		}
	}

	private List<Predicate<Link>> createRoadTypeFilter(List<String> types) {

		List<Predicate<Link>> filter = new ArrayList<>();

		for (String type : types) {

			Predicate<Link> p = link -> {
				var attr = link.getAttributes().getAttribute("type");
				if(attr == null)
					return true;

				Pattern pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE);
				return pattern.matcher(attr.toString()).find();
			};

			filter.add(p);
		}
		return filter;
	}

	private void matchBAStWithNetwork(String pathToNetwork, Map<String, BAStCountStation> stations, CountsOption countsOption, CrsOptions crs) {

		if(crs.getTargetCRS() != null)
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

		Index index = new Index(filteredNetwork, searchRange);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", crs.getInputCRS());

		log.info("+++++++ Match BASt stations with network +++++++");
		for (var station : stations.values())
			match(filteredNetwork, index, station, countsOption, coordinateTransformation);
	}

	private Map<String, BAStCountStation> readBAStCountStations(Path pathToAggregatedData, ShpOptions shp, CountsOption counts) {

		List<BAStCountStation> stations = new ArrayList<>();

		// The original bast file has windows encoding
		try (BufferedReader reader = Files.newBufferedReader(pathToAggregatedData, StandardCharsets.ISO_8859_1)) {

			CSVParser records = CSVFormat
					.Builder.create()
					.setAllowMissingColumnNames(false)
					.setDelimiter(';')
					.setHeader()
					.build()
					.parse(reader);

			for (var record : records) {

				String id = record.get("DZ_Nr");
				String name = record.get("DZ_Name");

				if (counts.isIgnored(id) || counts.isIgnored(name))
					continue;

				String dir1 = record.get("Hi_Ri1");
				String dir2 = record.get("Hi_Ri2");

				String x = record.get("Koor_UTM32_E").replace(".", "");
				String y = record.get("Koor_UTM32_N").replace(".", "");

				Coord coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));

				BAStCountStation station = new BAStCountStation(id, name, dir1, dir2, coord);
				stations.add(station);
			}
		} catch (IOException e) {
			log.error("Error reading count stations", e);
			throw new RuntimeException("Station data could not be read in ...");
		}

		Set<String> ignored = counts.getIgnored();
		final Predicate<BAStCountStation> optFilter = station -> !ignored.contains(station.getId());

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

	private static final class Index {

		private final STRtree index = new STRtree();
		private final GeometryFactory factory = new GeometryFactory();
		private final double range;

		public Index(Network network, double searchRange) {

			this.range = searchRange;

			for (Link link : network.getLinks().values()) {
				Envelope env = getLinkEnvelope(link);
				index.insert(env, link);
			}

			index.build();
		}

		@SuppressWarnings("unchecked")
		public Link query(BAStCountStation station) {

			Point p = MGC.coord2Point(station.getCoord());
			Envelope searchArea = p.buffer(this.range).getEnvelopeInternal();

			List<Link> result = index.query(searchArea);

			if (result.isEmpty()) return null;
			if (result.size() == 1) return result.get(0);

			// Find the closest link matching the direction
			Link closest = result.stream().findFirst().get();

			for (Link l : result) {

				if (station.getLinkDirection(l).equals(station.getMatchedDir())) continue;
				if (l.equals(station.getMatchedLink())) continue;

				double distance = link2LineString(l).distance(p);
				double curClosest = link2LineString(closest).distance(p);

				if (distance < curClosest) closest = l;
			}

			return closest;
		}

		private Envelope getLinkEnvelope(Link link) {
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

			return factory.createLineString(coordinates).getEnvelopeInternal();
		}

		private LineString link2LineString(Link link) {

			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

			return factory.createLineString(coordinates);
		}

		public void remove(Link link) {
			Envelope env = getLinkEnvelope(link);
			index.remove(env, link);
		}
	}
}
