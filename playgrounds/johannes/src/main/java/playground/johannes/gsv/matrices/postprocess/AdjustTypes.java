/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.postprocess;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.gsv.matrices.plans2matrix.ReplaceMiscType;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.studies.matrix2014.analysis.ActTypeDistanceTask;
import playground.johannes.studies.matrix2014.sim.Simulator;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class AdjustTypes {

    private static final Logger logger = Logger.getLogger(AdjustTypes.class);

    private static final String COL_SEPARATOR = ";";

    private static final DistanceCalculator distCalc = WGS84DistanceCalculator.getInstance();

    private static List<String> dayLabels;

    private static Map<String, TObjectDoubleHashMap<String>> fallbackDayHists;

    public static void main(String args[]) throws IOException {
        BufferedReader reader = IOUtils.getBufferedReader(args[0]);
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[2], "NO", null);

        dayLabels = new ArrayList<>(5);
        dayLabels.add("1");
        dayLabels.add("2");
        dayLabels.add("5");
        dayLabels.add("6");
        dayLabels.add("7");

        NumericMatrix distances = new NumericMatrix();
        Set<String> subkeys = new HashSet<>();
        Map<String, Double> volumes = new HashMap<>();
        TObjectDoubleHashMap<String> volumesWeekType = new TObjectDoubleHashMap<>();
        TObjectDoubleHashMap<String> volumesWeek = new TObjectDoubleHashMap<>();

        fallbackDayHists = new HashMap<>();

        Discretizer discretizer = new LinearDiscretizer(100000);

        XMLHandler parser = new XMLHandler(new PlainFactory());
        parser.setValidating(false);
        parser.parse(args[3]);
        Set<? extends Person> persons = parser.getPersons();
        TaskRunner.run(new Route2GeoDistance(new Simulator.Route2GeoDistFunction()), persons);
        TaskRunner.run(new ReplaceActTypes(), persons);
        new ReplaceMiscType().apply(persons);
        logger.info("Cloning persons...");
        Random random = new XORShiftRandom();
        persons = PersonCloner.weightedClones((Collection<PlainPerson>) persons, 2000000, random);
        logger.info(String.format("Generated %s persons.", persons.size()));


        ActTypeDistanceTask task = new ActTypeDistanceTask();
        task.setOutputDirectory(args[4]);
        task.analyze(persons, null);
        Map<Double, TObjectDoubleHashMap<String>> distribution = task.getHistrograms();

//        List<String> untouched = new ArrayList<>();

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        String DE = "DE";
        String NUTS0_CODE = "NUTS0_CODE";

        logger.info("Loading file...");
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            String purpose = tokens[2];
            String year = tokens[3];
            String mode = tokens[4];
            String direction = tokens[5];
            String day = tokens[6];
            String season = tokens[7];
            Double volume = new Double(tokens[8]);

            Zone zi = zones.get(from);
            Zone zj = zones.get(to);

            boolean skipped = true;
            if (zi != null && zj != null) {
                if (DE.equalsIgnoreCase(zi.getAttribute(NUTS0_CODE)) && DE.equalsIgnoreCase(zj.getAttribute
                        (NUTS0_CODE))) {
                    double d = calculateDistance(from, to, distances, zones);
                    if (d >= 100000) {

                        String key = buildKey(from, to, purpose, year, mode, direction, day, season);
                        volumes.put(key, volume);

                        String weekTypeKey = buildWeekTypeKey(from, to, purpose, year, mode, direction, season);
                        subkeys.add(weekTypeKey);
                        volumesWeekType.adjustOrPutValue(weekTypeKey, volume, volume);

                        String weekKey = buildWeekKey(from, to, year, mode, direction, season);
                        volumesWeek.adjustOrPutValue(weekKey, volume, volume);

                        TObjectDoubleHashMap<String> fallbackDayHist = fallbackDayHists.get(purpose);
                        if (fallbackDayHist == null) {
                            fallbackDayHist = new TObjectDoubleHashMap<>();
                            fallbackDayHists.put(purpose, fallbackDayHist);
                        }
                        fallbackDayHist.adjustOrPutValue(day, volume, volume);

                        skipped = false;
                    }
                }
            }

            if (skipped) {
                writer.write(line);
                writer.newLine();
            }
        }


        for (TObjectDoubleHashMap<String> hist : fallbackDayHists.values()) {
            Histogram.normalize(hist);
        }


        logger.info("Processing matrix...");
        ProgressLogger.init(volumesWeek.size(), 2, 10);
        TObjectDoubleIterator<String> weekIt = volumesWeek.iterator();
        for (int i = 0; i < volumesWeek.size(); i++) {
            weekIt.advance();

            double weekVol = weekIt.value();

            String tokens[] = weekIt.key().split(COL_SEPARATOR);
            String from = tokens[0];
            String to = tokens[1];
            String year = tokens[2];
            String mode = tokens[3];
            String direction = tokens[4];
            String season = tokens[5];

            Double d = calculateDistance(from, to, distances, zones);
            d = discretizer.discretize(d);
            TObjectDoubleHashMap<String> purposeHist = distribution.get(d);

            TObjectDoubleIterator<String> purposeIt = purposeHist.iterator();
            for (int j = 0; j < purposeHist.size(); j++) {
                purposeIt.advance();

                String purpose = purposeIt.key();
                TObjectDoubleHashMap<String> dayHist = calcDayHist(from, to, purpose, year, mode, direction, season,
                        volumes);
                double purposeFraction = purposeIt.value();
                double purposeVol = weekVol * purposeFraction;

                TObjectDoubleIterator<String> dayIt = dayHist.iterator();
                for (int k = 0; k < dayHist.size(); k++) {
                    dayIt.advance();
                    double dayVol = purposeVol * dayIt.value();

                    writer.write(from);
                    writer.write(COL_SEPARATOR);
                    writer.write(to);
                    writer.write(COL_SEPARATOR);
                    writer.write(purpose);
                    writer.write(COL_SEPARATOR);
                    writer.write(year);
                    writer.write(COL_SEPARATOR);
                    writer.write(mode);
                    writer.write(COL_SEPARATOR);
                    writer.write(direction);
                    writer.write(COL_SEPARATOR);
                    writer.write(weekIt.key());
                    writer.write(COL_SEPARATOR);
                    writer.write(season);
                    writer.write(COL_SEPARATOR);
                    writer.write(String.valueOf(dayVol));
                    writer.newLine();
                }
            }
            ProgressLogger.step();
        }
        ProgressLogger.terminate();

        writer.close();
        logger.info("Done.");
    }

    private static TObjectDoubleHashMap<String> calcDayHist(String from, String to, String purpose, String year, String mode, String
            direction, String season, Map<String, Double> volumes) {

        TObjectDoubleHashMap<String> hist = new TObjectDoubleHashMap<>();
        for (String day : dayLabels) {
            StringBuilder builder = new StringBuilder(200);
            builder.append(from);
            builder.append(COL_SEPARATOR);
            builder.append(to);
            builder.append(COL_SEPARATOR);
            builder.append(purpose);
            builder.append(COL_SEPARATOR);
            builder.append(year);
            builder.append(COL_SEPARATOR);
            builder.append(mode);
            builder.append(COL_SEPARATOR);
            builder.append(direction);
            builder.append(COL_SEPARATOR);
            builder.append(day);
            builder.append(COL_SEPARATOR);
            builder.append(season);

            String key = builder.toString();

            Double vol = volumes.get(key);
            if (vol != null) hist.put(day, vol);
        }

        if (hist.size() > 5) {
            logger.warn("Falling back to default day histogram.");
            hist = fallbackDayHists.get(purpose);
        } else {
            Histogram.normalize(hist);
        }

        return hist;
    }

    private static double calculateDistance(String from, String to, NumericMatrix matrix, ZoneCollection zones) {
        Double d = matrix.get(from, to);
        if (d == null) {
            Zone zi = zones.get(from);
            Zone zj = zones.get(to);

            if (zi != null && zj != null) {
                d = distCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
                matrix.set(from, to, d);
            }
        }

        return d;
    }

    private static String buildKey(String from, String to, String purpose, String year, String mode, String
            direction, String day, String season) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(COL_SEPARATOR);
        builder.append(to);
        builder.append(COL_SEPARATOR);
        builder.append(purpose);
        builder.append(COL_SEPARATOR);
        builder.append(year);
        builder.append(COL_SEPARATOR);
        builder.append(mode);
        builder.append(COL_SEPARATOR);
        builder.append(direction);
        builder.append(COL_SEPARATOR);
        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(season);

        return builder.toString();
    }

    private static String buildWeekTypeKey(String from, String to, String purpose, String year, String mode, String
            direction, String season) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(COL_SEPARATOR);
        builder.append(to);
        builder.append(COL_SEPARATOR);
        builder.append(purpose);
        builder.append(COL_SEPARATOR);
        builder.append(year);
        builder.append(COL_SEPARATOR);
        builder.append(mode);
        builder.append(COL_SEPARATOR);
        builder.append(direction);
//        builder.append(COL_SEPARATOR);
//        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(season);

        return builder.toString();
    }

    private static String buildWeekKey(String from, String to, String year, String mode, String
            direction, String season) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(COL_SEPARATOR);
        builder.append(to);
        builder.append(COL_SEPARATOR);
//        builder.append(purpose);
//        builder.append(COL_SEPARATOR);
        builder.append(year);
        builder.append(COL_SEPARATOR);
        builder.append(mode);
        builder.append(COL_SEPARATOR);
        builder.append(direction);
//        builder.append(COL_SEPARATOR);
//        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(season);

        return builder.toString();
    }

    private static class ReplaceActTypes implements EpisodeTask {

        private static Map<String, String> typeMapping;

        public Map<String, String> getTypeMapping() {
            if (typeMapping == null) {
                typeMapping = new HashMap<>();
//                typeMapping.put("vacations_short", ActivityTypes.VACATIONS_SHORT);
//                typeMapping.put("vacations_long", ActivityTypes.VACATIONS_LONG);
                typeMapping.put("visit", ActivityTypes.LEISURE);
                typeMapping.put("culture", ActivityTypes.LEISURE);
                typeMapping.put("gastro", ActivityTypes.LEISURE);
//                typeMapping.put(ActivityTypes.BUSINESS, ActivityTypes.WORK);
                typeMapping.put("private", ActivityTypes.MISC);
                typeMapping.put("pickdrop", ActivityTypes.MISC);
                typeMapping.put("sport", ActivityTypes.LEISURE);
//                typeMapping.put("wecommuter", ActivityTypes.WORK);
            }

            return typeMapping;
        }

        @Override
        public void apply(Episode plan) {
            for (Attributable act : plan.getActivities()) {
                String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                String newType = getTypeMapping().get(type);
                if (newType != null) {
                    act.setAttribute(CommonKeys.ACTIVITY_TYPE, newType);
                }
            }
        }
    }
}
