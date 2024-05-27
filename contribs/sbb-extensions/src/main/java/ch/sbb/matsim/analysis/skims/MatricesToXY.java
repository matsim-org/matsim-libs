/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.analysis.skims;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

/**
 * Creates a huge csv file with the data of all the matrices combined. Zones are identified by their identifier as well as one random coordinate per zone. Given the from-/to-coordinates for each
 * relation, the data can be easily visualized with Via's OD-Aggregator.
 * <p>
 * Please note that this process might require substantial memory, as all the data first needs to be loaded into memory before it is written out again.
 *
 * @author mrieser
 */
public class MatricesToXY {

    private final static Logger log = LogManager.getLogger(MatricesToXY.class);

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = args[0]; // path to a shape-file, e.g. /path/to/my-zones.shp
        String zonesIdAttributeName = args[1]; // name of the zone attribute to be used as identifier, e.g. "ZONE_ID"
        String matricesDirectory = args[2]; // path to the directory containing all the skim-matrices, e.g. /path/to/skims/
        String xyCsvOutputFilename = args[3]; // path to the csv-file to be written, e.g. /path/to/skim-data.csv

        log.info("loading zones from " + zonesShapeFilename);
        Collection<SimpleFeature> zones = new GeoFileReader().readFileAndInitialize(zonesShapeFilename);
        Map<String, SimpleFeature> zonesById = new HashMap<>();
        for (SimpleFeature zone : zones) {
            String zoneId = zone.getAttribute(zonesIdAttributeName).toString();
            zonesById.put(zoneId, zone);
        }

        log.info("Calculate one coordinate per zone");
        Map<String, Point> coords = new HashMap<>();
        for (Map.Entry<String, SimpleFeature> e : zonesById.entrySet()) {
            String zoneId = e.getKey();
            SimpleFeature f = e.getValue();
            Geometry g = (Geometry) f.getDefaultGeometry();
            if (g != null) {
                try {
                    Point pt = g.getInteriorPoint();
                    coords.put(zoneId, pt);
                } catch (Exception ex) {
                    log.warn("Problem calculating interior point. Using centroid for zone " + zoneId, ex);

                    Point pt = g.getCentroid();
                    coords.put(zoneId, pt);
                }
            }
        }

        log.info("loading car travel times");
        FloatMatrix<String> carTravelTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(carTravelTimes, new File(matricesDirectory, CalculateSkimMatrices.CAR_TRAVELTIMES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading car distances");
        FloatMatrix<String> carDistances = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(carDistances, new File(matricesDirectory, CalculateSkimMatrices.CAR_DISTANCES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt adaption times");
        FloatMatrix<String> ptAdaptionTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptAdaptionTimes, new File(matricesDirectory, CalculateSkimMatrices.PT_ADAPTIONTIMES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt frequencies");
        FloatMatrix<String> ptFrequencies = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptFrequencies, new File(matricesDirectory, CalculateSkimMatrices.PT_FREQUENCIES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt distances");
        FloatMatrix<String> ptDistances = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptDistances, new File(matricesDirectory, CalculateSkimMatrices.PT_DISTANCES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt travel times");
        FloatMatrix<String> ptTravelTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptTravelTimes, new File(matricesDirectory, CalculateSkimMatrices.PT_TRAVELTIMES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt access times");
        FloatMatrix<String> ptAccessTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptAccessTimes, new File(matricesDirectory, CalculateSkimMatrices.PT_ACCESSTIMES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt egress times");
        FloatMatrix<String> ptEgressTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptEgressTimes, new File(matricesDirectory, CalculateSkimMatrices.PT_EGRESSTIMES_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading pt transfer counts");
        FloatMatrix<String> ptTransferCounts = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptTransferCounts, new File(matricesDirectory, CalculateSkimMatrices.PT_TRANSFERCOUNTS_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading rail shares by distance");
        FloatMatrix<String> ptRailShareDistances = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptRailShareDistances, new File(matricesDirectory, CalculateSkimMatrices.PT_TRAINSHARE_BYDISTANCE_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading rail shares by time");
        FloatMatrix<String> ptRailShareTimes = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(ptRailShareTimes, new File(matricesDirectory, CalculateSkimMatrices.PT_TRAINSHARE_BYTIME_FILENAME).getAbsolutePath(), id -> id);

        log.info("loading beeline distances");
        FloatMatrix<String> beelineDistances = new FloatMatrix<>(zonesById.keySet(), Float.NaN);
        FloatMatrixIO.readAsCSV(beelineDistances, new File(matricesDirectory, CalculateSkimMatrices.BEELINE_DISTANCE_FILENAME).getAbsolutePath(), id -> id);

        log.info("Start writing xy csv to " + xyCsvOutputFilename);
        try (BufferedWriter writer = IOUtils.getBufferedWriter(xyCsvOutputFilename)) {
            writer.write(
                    "FROM,FROM_X,FROM_Y,TO,TO_X,TO_Y,CAR_TRAVELTIME,CAR_DISTANCE,PT_ADAPTIONTIME,PT_FREQUENCY,PT_DISTANCE,PT_TRAVELTIME,PT_ACCESSTIME,PT_EGRESSTIME,PT_TRANSFERCOUNT,PT_TRAINSHARE_DIST,PT_TRAINSHARE_TIME,BEELINE_DISTANCE\n");

            for (Map.Entry<String, Point> fromE : coords.entrySet()) {
                String fromId = fromE.getKey();
                Point fromPoint = fromE.getValue();
                for (Map.Entry<String, Point> toE : coords.entrySet()) {
                    String toId = toE.getKey();
                    Point toPoint = toE.getValue();

                    float carTravelTime = carTravelTimes.get(fromId, toId);
                    float carDistance = carDistances.get(fromId, toId);
                    float ptAdaptionTime = ptAdaptionTimes.get(fromId, toId);
                    float ptFrequency = ptFrequencies.get(fromId, toId);
                    float ptDistance = ptDistances.get(fromId, toId);
                    float ptTravelTime = ptTravelTimes.get(fromId, toId);
                    float ptAccessTime = ptAccessTimes.get(fromId, toId);
                    float ptEgressTime = ptEgressTimes.get(fromId, toId);
                    float ptTransferCount = ptTransferCounts.get(fromId, toId);
                    float ptRailShareDist = ptRailShareDistances.get(fromId, toId);
                    float ptRailShareTime = ptRailShareTimes.get(fromId, toId);
                    float beelineDistance = beelineDistances.get(fromId, toId);

                    writer.write(fromId);
                    writer.append(',');
                    writer.write(Integer.toString((int) fromPoint.getX()));
                    writer.append(',');
                    writer.write(Integer.toString((int) fromPoint.getY()));
                    writer.append(',');
                    writer.write(toId);
                    writer.append(',');
                    writer.write(Integer.toString((int) toPoint.getX()));
                    writer.append(',');
                    writer.write(Integer.toString((int) toPoint.getY()));
                    writer.append(',');
                    writer.write(Float.toString(carTravelTime));
                    writer.append(',');
                    writer.write(Float.toString(carDistance));
                    writer.append(',');
                    writer.write(Float.toString(ptAdaptionTime));
                    writer.append(',');
                    writer.write(Float.toString(ptFrequency));
                    writer.append(',');
                    writer.write(Float.toString(ptDistance));
                    writer.append(',');
                    writer.write(Float.toString(ptTravelTime));
                    writer.append(',');
                    writer.write(Float.toString(ptAccessTime));
                    writer.append(',');
                    writer.write(Float.toString(ptEgressTime));
                    writer.append(',');
                    writer.write(Float.toString(ptTransferCount));
                    writer.append(',');
                    writer.write(Float.toString(ptRailShareDist));
                    writer.append(',');
                    writer.write(Float.toString(ptRailShareTime));
                    writer.append(',');
                    writer.write(Float.toString(beelineDistance));
                    writer.append('\n');
                }
            }
        }
        log.info("done.");
    }

}
