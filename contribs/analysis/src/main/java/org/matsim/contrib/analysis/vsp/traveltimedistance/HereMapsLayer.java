package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to load layer information from HERE API
 */
public final class HereMapsLayer {

	/**
	 * Backend URL
	 */
	public static final String URL = "https://pde.api.here.com/1/tile.txt";

	private static final Logger log = LogManager.getLogger(HereMapsLayer.class);

	private final String layerName;
	private final String appId;
	private final String appCode;

	/**
	 * Constructor.
	 */
	public HereMapsLayer(String layerName, String appId, String appCode) {
		this.layerName = layerName;
		this.appId = appId;
		this.appCode = appCode;
	}

	/**
	 * Fetches results for a single tile on the map.
	 *
	 * @see #fetchAll(Envelope, FC)
	 */
	public Result fetch(FC fc, int tileX, int tileY) {

		URI uri;
		try {
			uri = new URIBuilder(URL)
					.addParameter("layer", layerName + ((fc == FC.STATIC) ? "" : ("_" + fc)))
					.addParameter("level", String.valueOf(fc.level))
					.addParameter("tilex", String.valueOf(tileX))
					.addParameter("tiley", String.valueOf(tileY))
					.addParameter("app_id", appId)
					.addParameter("app_code", appCode)
					.build();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid URL");
		}


		HttpGet get = new HttpGet(uri);

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			try (CloseableHttpResponse resp = httpclient.execute(get)) {

				// Too many requests
				if (resp.getCode() == 429) {
					Thread.sleep(5000);
					return fetch(fc, tileX, tileY);
				}

				if (resp.getCode() != 200)
					throw new IllegalStateException("API returned error code " + resp.getCode());

				CSVParser parser = new CSVParser(new InputStreamReader(resp.getEntity().getContent()), CSVFormat.TDF.withFirstRecordAsHeader());
				return new Result(parser);

			} catch (InterruptedException e) {
				throw new IllegalStateException("Wait interrupted", e);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	/**
	 * Fetches all results for a bounding box and all detail levels less or equal to {@code maxFC}.
	 *
	 * @param bbox Bounding box in WGS84 coordinates
	 */
	public Result fetchAll(Envelope bbox, FC maxFC) {

		Result result = null;

		for (FC fc : FC.values()) {

			if (fc.compareTo(maxFC) > 0)
				return result;


			double tileSize = 180 / Math.pow(2, fc.level);


			int tileX1 = (int) Math.floor((bbox.getMinX() + 180.0) / tileSize);
			int tileX2 = (int) Math.floor((bbox.getMaxX() + 180.0) / tileSize);

			int tileY1 = (int) Math.floor((bbox.getMinY() + 90.0) / tileSize);
			int tileY2 = (int) Math.floor((bbox.getMaxY() + 90.0) / tileSize);


			log.info("Loading {} tiles: {} x {} - {} x {}, level: {}", layerName, tileX1, tileY1, tileX2, tileY2, fc);

			for (int i = tileX1; i <= tileX2; i++) {
				for (int j = tileY1; j <= tileY2; j++) {

					log.debug("Loading tile x: {}, y: {}, level: {}", i, j, fc);

					Result fetch = fetch(fc, i, j);

					if (result == null)
						result = fetch;
					else
						result.add(fetch);

					// Around 10 requests per second are allowed
					try {
						Thread.sleep(150);
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait interrupted", e);
					}

				}

			}

		}

		return result;
	}

	/**
	 * Contains the result of query to the layer in row / column format.
	 */
	public static final class Result {

		private final List<String> header;

		private final List<CSVRecord> records;

		private Result(CSVParser parser) throws IOException {
			header = parser.getHeaderNames();
			records = parser.getRecords();

		}

		void add(Result other) {

			if (!header.equals(other.header))
				throw new IllegalArgumentException("Header of these records are different");

			records.addAll(other.records);
		}

		/**
		 * Header of this data set.
		 */
		public List<String> getHeader() {
			return header;
		}

		/**
		 * List of records.
		 */
		public List<CSVRecord> getRecords() {
			return records;
		}

		@Override
		public String toString() {
			return "Result{" +
					"header=" + header + ", records=" + records.size() +
					'}';
		}
	}

	/**
	 * Functional class, describes the level of detail.
	 */
	public enum FC implements Comparable<FC> {
		FC1(9),
		FC2(10),
		FC3(11),
		FC4(12),
		FC5(13),
		STATIC(0);

		/**
		 * Level according to the map coordinates (similar to zoom level on osm or gmaps)
		 */
		final int level;

		FC(int level) {
			this.level = level;
		}
	}

	/**
	 * Enum with bitmasks for vehicle types.
	 */
	public enum VehicleType {
		car(1),
		bus(2),
		taxi(4),
		car_pool(8),
		pedestrian(16),
		truck(32),
		delivery(64),
		emergency(128),
		through_traffic(256),
		motorcycle(512),
		road_train(1024);

		/**
		 * The corresponding bit as integer 2^x
		 */
		final int bit;

		VehicleType(int bit) {
			this.bit = bit;
		}

		/**
		 * Parse contained vehicle types from a bit set.
		 */
		public static Set<VehicleType> parse(int value) {

			Set<VehicleType> set = new HashSet<>();

			for (VehicleType t : VehicleType.values()) {
				if ((value & t.bit) == t.bit)
					set.add(t);
			}

			return set;
		}

	}

	/**
	 * Speed category as defined by the API.
	 */
	public enum SpeedCategory {
		UNDEFINED(0),
		SC_1(150/3.6),
		SC_2(130/3.6),
		SC_3(100/3.6),
		SC_4(90/3.6),
		SC_5(70/3.6),
		SC_6(50/3.6),
		SC_7(30/3.6),
		SC_8(11/3.6),
		;

		/**
		 * Speed in m/s.
		 */
		public final double speed;

		SpeedCategory(double speed) {
			this.speed = speed;
		}
	}
}
