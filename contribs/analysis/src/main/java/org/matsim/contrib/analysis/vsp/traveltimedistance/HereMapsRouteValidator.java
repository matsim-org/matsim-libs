/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.analysis.vsp.traveltimedistance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author jbischoff this class requests travel times and distances between two
 *         coordinates from HERE Maps. Please replace the API code with your own
 *         and obey here's usage policy
 *
 */
public class HereMapsRouteValidator implements TravelTimeDistanceValidator {

	final String appId;
	final String appCode;
	final String outputPath;
	final String date;
	final CoordinateTransformation transformation;
	boolean writeDetailedFiles = true;

	/**
	 * 
	 * @param outputFolder
	 *            folder to write gzipped json files
	 * @param appId
	 *            your here App ID
	 * @param appCode
	 *            your here App Code
	 * @param date
	 *            a date to run the validation for, format: 2017-06-08
	 * @param transformation
	 *            A coordinate transformation to WGS 84
	 */
	public HereMapsRouteValidator(String outputFolder, String appId, String appCode, String date,
			CoordinateTransformation transformation) {
		this.outputPath = outputFolder;
		this.appId = appId;
		this.appCode = appCode;
		this.date = date;
		this.transformation = transformation;
		File outDir = new File(outputFolder);
		if (!outDir.exists()) {
			outDir.mkdirs();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.analysis.vsp.traveltimes.TravelTimeValidator#
	 * getTravelTime(org.matsim.contrib.analysis.vsp.traveltimes.CarTrip)
	 */
	@Override
	public Tuple<Double, Double> getTravelTime(CarTrip trip) {

		long travelTime = 0;
		long distance = 0;
		Coord from = transformation.transform(trip.getDepartureLocation());
		Coord to = transformation.transform(trip.getArrivalLocation());
		String filename = outputPath + "/" + trip.getPersonId() + "_" + trip.getDepartureTime() + ".json.gz";
		Locale locale = new Locale("en", "UK");
		String pattern = "###.#####";

		DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		df.applyPattern(pattern);

		String urlString = "http://route.cit.api.here.com/routing/7.2/calculateroute.json?app_id=" + appId
				+ "&app_code=" + appCode + "&waypoint0=geo!" + df.format(from.getY()) + "," + df.format(from.getX())
				+ "&waypoint1=geo!" + df.format(to.getY()) + "," + df.format(to.getX()) + "&departure=" + date + "T"
				+ Time.writeTime(trip.getDepartureTime()) + "&mode=fastest;car;traffic:enabled";

		try {
			System.out.println(urlString);
			URL url = new URL(urlString);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			JSONParser jp = new JSONParser();

			JSONObject jsonObject = (JSONObject) jp.parse(in);
			JSONObject route = (JSONObject) ((JSONArray) ((JSONObject) jsonObject.get("response")).get("route")).get(0);
			JSONObject summary = (JSONObject) route.get("summary");
			travelTime = (long) summary.get("travelTime");
			distance = (long) summary.get("distance");

			// System.out.println(travelTime + " "+ baseTime + " "+distance);
			if (writeDetailedFiles){
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			bw.write(jsonObject.toString());
			bw.flush();
			bw.close();
			}
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		} catch (ParseException e) {
		}

		return new Tuple<Double, Double>((double) travelTime, (double) distance);
	}

	/**
	 * @return the writeDetailedFiles
	 */
	public boolean isWriteDetailedFiles() {
		return writeDetailedFiles;
	}

	/**
	 * @param writeDetailedFiles
	 *            the writeDetailedFiles to set
	 */
	public void setWriteDetailedFiles(boolean writeDetailedFiles) {
		this.writeDetailedFiles = writeDetailedFiles;
	}

}
