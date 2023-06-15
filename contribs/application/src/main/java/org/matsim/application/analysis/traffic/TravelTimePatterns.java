package org.matsim.application.analysis.traffic;

import com.google.common.base.Joiner;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsLayer;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "travel-time-patterns",
		description = "Download travel time pattern from online API.",
		mixinStandardHelpOptions = true
)
public class TravelTimePatterns implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(TravelTimePatterns.class);


	@CommandLine.Option(names = "--api", description = "Online API used. Choose from [HERE]", defaultValue = "HERE", required = false)
	private TravelTimeAnalysis.API api;

	@CommandLine.Option(names = "--app-id", description = "App id for HERE API", required = true)
	private String appId;

	@CommandLine.Option(names = "--app-code", description = "App code for HERE API", required = true)
	private String appCode;

	@CommandLine.Option(names = "--max-fc", defaultValue = "FC5", description = "Max detail level, where FC5 is the most detailed.")
	private HereMapsLayer.FC fc;

	@CommandLine.Option(names = "--weekday", defaultValue = "4", description = "Number of weekday to use, 0=Sunday")
	private int weekday = 3;

	@CommandLine.Option(names = "--output", defaultValue = "trafficPatterns.csv", description = "Output csv", required = true)
	private Path output;

	@CommandLine.Option(names = "--output-links", description = "Path to csv in order to write link attributes.")
	private Path linkOutput;

	@CommandLine.Option(names = "--output-network", description = "Write a time-variant in MATSim format.")
	private Path networkOutput;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions("EPSG:4326");

	public static void main(String[] args) {
		new TravelTimePatterns().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (api != TravelTimeAnalysis.API.HERE) {
			log.error("Only HERE API is currently supported.");
			return 2;
		}

		if (shp.getShapeFile() == null) {
			log.error("Shape file is required!");
			return 2;
		}

		Envelope bbox = shp.getGeometry().getEnvelopeInternal();

		bbox = JTS.transform(bbox, CRS.findMathTransform(CRS.decode(shp.getShapeCrs()), DefaultGeographicCRS.WGS84, true));

		HereMapsLayer patternLayer = new HereMapsLayer("TRAFFIC_PATTERN", appId, appCode);

		HereMapsLayer.Result st = patternLayer.fetch(HereMapsLayer.FC.STATIC, 0, 0);

		Map<String, double[]> speedValues = createSpeedValues(st);

		HereMapsLayer.Result patterns = patternLayer.fetchAll(bbox, fc);

		Map<String, Double> ffSpeed = new HashMap<>();
		Map<String, Double> avgSpeed = new HashMap<>();

		// from ref and to ref speeds
		Map<String, double[]> fSpeeds = new HashMap<>();
		Map<String, double[]> tSpeeds = new HashMap<>();

		try (CSVPrinter printer = csv.createPrinter(output)) {

			Set<String> written = new HashSet<>();

			printer.printRecord("LINK_ID", "time", "speed");

			for (CSVRecord record : patterns.getRecords()) {

				String id = record.get("LINK_ID");

				if (!written.add(id))
					continue;

				avgSpeed.put(id, parseSpeed(record.get("AVG_SPEED")));
				ffSpeed.put(id, parseSpeed(record.get("FREE_FLOW_SPEED")));

				double[] values = null;

				if (record.get("F_WEEKDAY").contains(",")) {
					values = speedValues.get(record.get("F_WEEKDAY").split(",")[weekday]);
					fSpeeds.put(id, values);
				} else if (record.get("T_WEEKDAY").contains(",")) {
					values = speedValues.get(record.get("T_WEEKDAY").split(",")[weekday]);
					tSpeeds.put(id, values);
				}

				if (values == null) {
					continue;
				}

				for (int i = 0; i < values.length; i++) {
					printer.print(id);
					printer.print(i * 900);
					printer.print(values[i]);
					printer.println();
				}

			}

		}

		HereMapsLayer linkLayer = new HereMapsLayer("LINK", appId, appCode);
		HereMapsLayer.Result links = null;

		Map<String, CSVRecord> linkRecords = new HashMap<>();

		// fetch link layer if required
		if (linkOutput != null || networkOutput != null) {
			links = linkLayer.fetchAll(bbox, fc);
			for (CSVRecord record : links.getRecords()) {
				linkRecords.put(record.get("LINK_ID"), record);
			}
		}

		if (linkOutput != null) {

			HereMapsLayer geom = new HereMapsLayer("ROAD_GEOM", appId, appCode);

			Map<String, String> geometries = createGeometries(geom.fetchAll(bbox, fc));

			Set<String> written = new HashSet<>();

			log.info("Writing attributes to {}", linkOutput);

			try (CSVPrinter printer = csv.createPrinter(this.linkOutput)) {

				for (String h : links.getHeader()) {
					printer.print(h);
				}

				printer.print("avgSpeed");
				printer.print("freeflowSpeed");
				printer.print("wkt");
				printer.println();

				for (CSVRecord record : links.getRecords()) {

					String id = record.get("LINK_ID");

					// only write each link one time
					if (!written.add(id))
						continue;

					for (String s : record) {
						printer.print(s);
					}

					printer.print(avgSpeed.get(id));
					printer.print(ffSpeed.get(id));
					printer.print(geometries.get(id));
					printer.println();

				}
			}
		}

		if (networkOutput != null) {


			NetworkConfigGroup group = new NetworkConfigGroup();
			group.setTimeVariantNetwork(true);

			Network network = NetworkUtils.createNetwork(group);
			List<NetworkChangeEvent> changeEvents = new ArrayList<>();

			ProjectionUtils.putCRS(network, crs.getTargetCRS());

			CoordinateTransformation ct = crs.getTransformation();

			NetworkFactory f = network.getFactory();

			HereMapsLayer attrLayer = new HereMapsLayer("LINK_ATTRIBUTE", appId, appCode);
			HereMapsLayer.Result attrs = attrLayer.fetchAll(bbox, fc);

			/*
			HereMapsLayer attr2Layer = new HereMapsLayer("LINK_ATTRIBUTE2", appId, appCode);
			HereMapsLayer.Result attrs2 = attr2Layer.fetchAll(bbox, fc);

			HereMapsLayer speedLayer = new HereMapsLayer("SPEED_LIMITS", appId, appCode);
			HereMapsLayer.Result speed = speedLayer.fetchAll(bbox, fc);

			HereMapsLayer signLayer = new HereMapsLayer("TRAFFIC_SIGN", appId, appCode);
			HereMapsLayer.Result sign = speedLayer.fetchAll(bbox, fc);
			 */

			Set<String> written = new HashSet<>();

			for (CSVRecord record : attrs.getRecords()) {

				String id = record.get("LINK_ID");

				if (!written.add(id))
					continue;

				CSVRecord linkRecord = linkRecords.get(id);

				double[] lat = parseCoords(linkRecord.get("LAT"));
				double[] lon = parseCoords(linkRecord.get("LON"));

				String t = record.get("TRAVEL_DIRECTION");

				Node ref = addOrGetNode(network, linkRecord.get("REF_NODE_ID"), ct.transform(new Coord(lon[0], lat[0])));
				Node nonRef = addOrGetNode(network, linkRecord.get("NONREF_NODE_ID"), ct.transform(new Coord(lon[1], lat[1])));

				double allowedSpeed = parseSpeedCategory(record.get("SPEED_CATEGORY"));

				Set<String> modes = HereMapsLayer.VehicleType.parse(Integer.parseInt(record.get("VEHICLE_TYPES"))).stream()
						.map(Object::toString).collect(Collectors.toSet());

				// create links for both directions
				if (t.equals("B")) {

					Link linkF = f.createLink(Id.createLinkId(id + "f"), ref, nonRef);
					Link linkT = f.createLink(Id.createLinkId(id + "t"), nonRef, ref);

					linkF.setLength(parseDouble(linkRecord.get("LINK_LENGTH")));
					linkT.setLength(parseDouble(linkRecord.get("LINK_LENGTH")));

					linkT.setNumberOfLanes(parseDouble(record.get("TO_REF_NUM_LANES")));
					linkF.setNumberOfLanes(parseDouble(record.get("FROM_REF_NUM_LANES")));

					linkF.setFreespeed(retrieveSpeed(ffSpeed, id, record));
					linkT.setFreespeed(retrieveSpeed(ffSpeed, id, record));

					linkF.setAllowedModes(modes);
					linkT.setAllowedModes(modes);

					linkF.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);
					linkT.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);

					changeEvents.addAll(createChangeEvents(linkF, fSpeeds.getOrDefault(id, null)));
					changeEvents.addAll(createChangeEvents(linkT, tSpeeds.getOrDefault(id, null)));

					network.addLink(linkF);
					network.addLink(linkT);

				} else if (t.equals("F")) {
					// from ref node
					Link link = f.createLink(Id.createLinkId(id), ref, nonRef);

					link.setLength(parseDouble(linkRecord.get("LINK_LENGTH")));
					link.setNumberOfLanes(parseDouble(record.get("FROM_REF_NUM_LANES")));

					link.setFreespeed(retrieveSpeed(ffSpeed, id, record));
					link.setAllowedModes(modes);
					link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);

					changeEvents.addAll(createChangeEvents(link, fSpeeds.getOrDefault(id, null)));

					network.addLink(link);

				} else if (t.equals("T")) {
					// to ref node
					Link link = f.createLink(Id.createLinkId(id), nonRef, ref);

					link.setLength(parseDouble(linkRecord.get("LINK_LENGTH")));
					link.setNumberOfLanes(parseDouble(record.get("TO_REF_NUM_LANES")));
					link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);

					link.setFreespeed(retrieveSpeed(ffSpeed, id, record));
					link.setAllowedModes(modes);

					changeEvents.addAll(createChangeEvents(link, tSpeeds.getOrDefault(id, null)));

					network.addLink(link);
				} else
					throw new IllegalStateException("Unknown travel direction: " + t);

			}

			new NetworkCleaner().run(network);

			log.info("Writing network to {}", networkOutput);

			NetworkUtils.writeNetwork(network, networkOutput.toString());

			String eventPath = networkOutput.toString().replace(".xml", "-events.xml");
			log.info("Writing {} change events to {}", changeEvents.size(), eventPath);

			NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
			writer.write(eventPath, changeEvents);
		}

		return 0;
	}

	/**
	 * Creates network change events from time bin interpretation.
	 */
	private Collection<NetworkChangeEvent> createChangeEvents(Link link, double[] speeds) {

		List<NetworkChangeEvent> events = new ArrayList<>();

		if (speeds == null)
			return events;

		double last = Double.MAX_VALUE;

		for (int i = 0; i < speeds.length; i++) {

			double v = speeds[i];
			if (v != last) {

				NetworkChangeEvent ev = new NetworkChangeEvent(i * 900);
				ev.addLink(link);
				ev.setFreespeedChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, v));
				events.add(ev);
				last = v;
			}
		}

		return events;
	}

	private static double retrieveSpeed(Map<String, Double> ffSpeed, String id, CSVRecord record) {

		double speed = ffSpeed.getOrDefault(id, 0d);

		return speed != 0 ? speed : HereMapsLayer.SpeedCategory.values()[Integer.parseInt(record.get("SPEED_CATEGORY"))].speed;
	}

	private static Node addOrGetNode(Network network, String nodeId, Coord transform) {

		Id<Node> id = Id.createNodeId(nodeId);

		if (network.getNodes().containsKey(id))
			return network.getNodes().get(id);

		Node node = network.getFactory().createNode(id, transform);
		network.addNode(node);
		return node;
	}

	private static Map<String, double[]> createSpeedValues(HereMapsLayer.Result st) {

		Map<String, double[]> map = new HashMap<>();

		for (CSVRecord record : st.getRecords()) {

			String id = record.get("PATTERN_ID");

			String[] values = record.get("SPEED_VALUES").split(",");

			map.put(id, Arrays.stream(values).mapToDouble(TravelTimePatterns::parseSpeed).toArray());
		}

		return map;
	}

	private static Map<String, String> createGeometries(HereMapsLayer.Result geom) {

		HashMap<String, String> map = new HashMap<>();

		for (CSVRecord record : geom.getRecords()) {

			String linkId = record.get("LINK_ID");

			if (record.get("LON").isBlank() || record.get("LAT").isBlank())
				continue;

			double[] lons = Arrays.stream(record.get("LON").split(",")).mapToDouble(TravelTimePatterns::parseDouble).toArray();
			double[] lats = Arrays.stream(record.get("LAT").split(",")).mapToDouble(TravelTimePatterns::parseDouble).toArray();

			// Coordinates are given as longs
			lons[0] *= 10e-6;
			lats[0] *= 10e-6;

			// sometimes it's malformed
			int n = Math.min(lons.length, lats.length);

			if (n <= 1)
				continue;

			for (int i = 1; i < n; i++) {
				lats[i] = lats[i - 1] + lats[i] * 10e-6;
				lons[i] = lons[i - 1] + lons[i] * 10e-6;
			}

			String[] wkt = new String[n];
			for (int i = 0; i < n; i++) {
				wkt[i] = lons[i] + " " + lats[i];
			}

			String s = "LINESTRING(" + Joiner.on(", ").join(wkt) + ")";
			map.put(linkId, s);
		}

		return map;
	}

	/**
	 * Converts HERE coordinates from [10^-5 degree WGS84]. Comma separated. Each value is relative to the previous.
	 */
	private static double[] parseCoords(String s) {
		double[] x = Arrays.stream(s.split(",")).mapToDouble(TravelTimePatterns::parseDouble).toArray();
		x[0] *= 10e-6;
		for (int i = 1; i < x.length; i++) {
			x[i] = x[i - 1] + x[i] * 10e-6;
		}

		return x;
	}

	private static double parseDouble(String s) {
		if (s.isBlank() || s.equals("null"))
			return 0;

		return Double.parseDouble(s);
	}

	private static double parseSpeed(String s) {
		if (s.isBlank())
			return Double.NaN;

		return Integer.parseInt(s) / 3.6;
	}

	private static double parseSpeedCategory(String s) {
		return switch (Integer.parseInt(s)) {
			case 1 -> 999;
			case 2 -> 130/3.6;
			case 3 -> 100/3.6;
			case 4 -> 90/3.6;
			case 5 -> 70/3.6;
			case 6 -> 50/3.6;
			case 7 -> 30/3.6;
			case 8 -> 11/3.6;
			default -> -1;
		};
	}

}
