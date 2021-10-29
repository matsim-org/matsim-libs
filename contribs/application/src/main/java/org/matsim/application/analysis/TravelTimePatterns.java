package org.matsim.application.analysis;

import com.google.common.base.Joiner;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsLayer;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;

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

	@CommandLine.Option(names = "--max-fc", defaultValue = "FC4", description = "Max detail level, where FC5 is the most detailed.")
	private HereMapsLayer.FC fc = HereMapsLayer.FC.FC4;

	@CommandLine.Option(names = "--weekday", defaultValue = "4", description = "Number of weekday to use, 0=Sunday")
	private int weekday = 3;

	@CommandLine.Option(names = "--output", defaultValue = "trafficPatterns.csv", description = "Output csv", required = true)
	private Path output;

	@CommandLine.Option(names = "--output-links", description = "Path to csv to write link attributes.")
	private Path linkOutput;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions();

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

				if (record.get("F_WEEKDAY").contains(","))
					values = speedValues.get(record.get("F_WEEKDAY").split(",")[weekday]);
				else if (record.get("T_WEEKDAY").contains(","))
					values = speedValues.get(record.get("T_WEEKDAY").split(",")[weekday]);

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

		if (linkOutput != null) {

			HereMapsLayer linkLayer = new HereMapsLayer("LINK", appId, appCode);

			HereMapsLayer geom = new HereMapsLayer("ROAD_GEOM", appId, appCode);

			Map<String, String> geometries = createGeometries(geom.fetchAll(bbox, fc));

			HereMapsLayer.Result links = linkLayer.fetchAll(bbox, fc);

			Set<String> written = new HashSet<>();

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

		return 0;
	}

	private Map<String, double[]> createSpeedValues(HereMapsLayer.Result st) {

		Map<String, double[]> map = new HashMap<>();

		for (CSVRecord record : st.getRecords()) {

			String id = record.get("PATTERN_ID");

			String[] values = record.get("SPEED_VALUES").split(",");

			map.put(id, Arrays.stream(values).mapToDouble(TravelTimePatterns::parseSpeed).toArray());
		}

		return map;
	}

	private Map<String, String> createGeometries(HereMapsLayer.Result geom) {

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

	private static double parseDouble(String s) {
		if (s.isBlank())
			return 0;

		return Double.parseDouble(s);
	}

	private static double parseSpeed(String s) {
		if (s.isBlank())
			return Double.NaN;

		return Integer.parseInt(s) / 3.6;
	}

}
