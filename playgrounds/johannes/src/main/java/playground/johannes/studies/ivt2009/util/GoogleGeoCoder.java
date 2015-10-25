/* *********************************************************************** *
 * project: org.matsim.*
 * GeoCoder2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.studies.ivt2009.util;

import com.google.code.geocoder.AdvancedGeoCoder;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderStatus;
import com.google.code.geocoder.model.LatLng;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class GoogleGeoCoder {

	private static final Logger logger = Logger.getLogger(GoogleGeoCoder.class);

	private static final String cacheFile = System.getProperty("user.home") + "/.geocoder.cache";

	private final Geocoder geocoder;

	private final long sleepInterval;

	private Map<String, LatLng> cache;

	private boolean overQueryLimit = false;

	private BufferedWriter writer;

	public GoogleGeoCoder() {
		this(null, 0, 0);
	}

	public GoogleGeoCoder(String proxy, int port, long sleep) {
		HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		if (proxy != null) {
			httpClient.getHostConfiguration().setProxy(proxy, port);
		}
		geocoder = new AdvancedGeoCoder(httpClient);

		this.sleepInterval = sleep;

		cache = new HashMap<String, LatLng>();
		readCache();
	}

	public LatLng requestCoordinate(String query) {
		if (overQueryLimit)
			return null;

		if (query.trim().isEmpty())
			return null;

		Level level = Logger.getRootLogger().getLevel();
		Logger.getRootLogger().setLevel(Level.INFO);

		LatLng coord = null;

		if (!cache.containsKey(query)) {
			try {
				Thread.sleep(sleepInterval);
				GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(query).setLanguage("de").getGeocoderRequest();
				GeocodeResponse response = geocoder.geocode(geocoderRequest);
				if (response.getStatus() == GeocoderStatus.OK) {
					coord = response.getResults().get(0).getGeometry().getLocation();
					addToCache(query, coord);
				} else if (response.getStatus() == GeocoderStatus.ZERO_RESULTS) {
					logger.warn(String.format("No results for query \"%s\" found.", query));
					addToCache(query, null);
				} else if (response.getStatus() == GeocoderStatus.OVER_QUERY_LIMIT) {
					logger.warn("Query limit exceeded.");
					overQueryLimit = true;
				} else {
					logger.warn(String.format("Request failed with error \"%s\".", response.getStatus().name()));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			coord = cache.get(query);
		}

		Logger.getRootLogger().setLevel(level);

		return coord;
	}

	private void readCache() {
		try {
			File file = new File(cacheFile);
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String tokens[] = line.split("\t", -1);

					String query = tokens[0];
					String coordStr = tokens[1];

					LatLng latlng = null;
					if (!coordStr.isEmpty()) {
						String coords[] = coordStr.split(",");
						latlng = new LatLng(coords[1], coords[0]);
					}
					cache.put(query, latlng);
				}

				reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addToCache(String query, LatLng coord) {
		cache.put(query, coord);

		try {
			if (writer == null) {
				File file = new File(cacheFile);
				if (file.exists()) {
					writer = new BufferedWriter(new FileWriter(cacheFile, true));
				} else {
					writer = new BufferedWriter(new FileWriter(cacheFile));
				}
			}
			writer.write(query);
			writer.write("\t");
			if (coord != null) {
				writer.write(String.valueOf(coord.getLng()));
				writer.write(",");
				writer.write(String.valueOf(coord.getLat()));
			}
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
