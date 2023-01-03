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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * coordinates from HERE Maps. Please replace the API code with your own
 * and obey here's usage policy
 */
public class HereMapsRouteValidator implements TravelTimeDistanceValidator {
    private static final Logger log = LogManager.getLogger(HereMapsRouteValidator.class);
    private final String mode;
    private final String apiAccessKey;
    private final String outputPath;
    private final String date;
    private final CoordinateTransformation transformation;
    private final boolean writeDetailedFiles;

    /**
     * @param outputFolder   folder to write gzipped json files
     * @param apiAccessKey   your API Access Code (to be request on the here.com)
     * @param date           a date to run the validation for, format: 2017-06-08
     * @param transformation A coordinate transformation to WGS 84
     */
    public HereMapsRouteValidator(String outputFolder, String mode, String apiAccessKey, String date,
                                  CoordinateTransformation transformation, boolean writeDetailedFiles) {
        this.outputPath = outputFolder;
        this.mode = mode == null ? "car" : mode;
        this.apiAccessKey = apiAccessKey;
        this.date = date;
        this.transformation = transformation;
        this.writeDetailedFiles = writeDetailedFiles;

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
    public Tuple<Double, Double> getTravelTime(NetworkTrip trip) {
        String tripId = trip.getPersonId().toString() + "_" + trip.getDepartureTime();
        return getTravelTime(trip.getDepartureLocation(), trip.getArrivalLocation(), trip.getDepartureTime(), tripId);
    }

    @Override
    public Tuple<Double, Double> getTravelTime(Coord fromCoord, Coord toCoord, double departureTime, String tripId) {
        long travelTime = 0;
        long distance = 0;

        Coord from = transformation.transform(fromCoord);
        Coord to = transformation.transform(toCoord);

        Locale locale = new Locale("en", "UK");
        String pattern = "###.#####";

        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        df.applyPattern(pattern);

        String urlString = "https://router.hereapi.com/v8/routes?" + "&apiKey=" + apiAccessKey + "&transportmode=" + mode + "&origin="
                + df.format(from.getY()) + "," + df.format(from.getX()) + "&destination=" + df.format(to.getY()) + ","
                + df.format(to.getX()) + "&departureTime=" + date + "T" + Time.writeTime(departureTime)
                + "&return=summary";

        log.info(urlString);

        try {
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            JSONParser jp = new JSONParser();
            JSONObject jsonObject = (JSONObject) jp.parse(in);
            JSONArray routes = (JSONArray) jsonObject.get("routes");

            if (!routes.isEmpty()) {
                JSONObject route = (JSONObject) routes.get(0);
                JSONArray sections = (JSONArray) route.get("sections");
                JSONObject section = (JSONObject) sections.get(0);
                JSONObject summary = (JSONObject) section.get("summary");
                travelTime = (long) summary.get("duration");
                distance = (long) summary.get("length");
                if (writeDetailedFiles) {
                    String filename = outputPath + "/" + tripId + ".json.gz";
                    BufferedWriter bw = IOUtils.getBufferedWriter(filename);
                    bw.write(jsonObject.toString());
                    bw.flush();
                    bw.close();
                }
            }

            in.close();
        } catch (MalformedURLException e) {
            log.error("URL is not working. Please check your API key", e);
        } catch (IOException | ParseException e) {
            log.error("Cannot read the content on the URL properly. Please manually check the URL", e);
        }
        return new Tuple<Double, Double>((double) travelTime, (double) distance);
    }

}
