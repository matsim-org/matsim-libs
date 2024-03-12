/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.pt.stop2stop;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysis.aggregateStop2StopAggregations;

/**
 * Write a shape file with passenger volumes, delays and similar information for each stop to stop
 * segment of a pt {@link org.matsim.pt.transitSchedule.api.TransitLine}.
 *
 * @author vsp-gleich
 */
public class PtStop2StopAnalysis2Shp {

    private static final String[] headerCsv = {"link", "transitLine", "stop", "departures", "passengers", "totalVehicleCapacity"};
    private static final Logger log = LogManager.getLogger(PtStop2StopAnalysis2Shp.class);

    public static void writePtStop2StopAnalysisByTransitLine2ShpFile(
            final Scenario scenario, final List<PtStop2StopAnalysis.Stop2StopEntry> stop2StopEntriesForEachDeparture, String shpFileName, String coordinateSystem) {

        // sum per link and transit line
        Map<Id<Link>, Map<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation>> stop2stopByLinkAndTransitLineAggregates = aggregatePerLinkAndLine(stop2StopEntriesForEachDeparture);

        SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
        try {
            simpleFeatureBuilder.setCRS(MGC.getCRS(coordinateSystem));
        } catch (IllegalArgumentException e) {
            log.warn("Coordinate reference system \"" + coordinateSystem + "\" is unknown.");
        }

        simpleFeatureBuilder.setName("ptLinkPerLinePaxFeature");
        // note: column names may not be longer than 10 characters. Otherwise the name is cut after the 10th character and the avalue is NULL in QGis
        simpleFeatureBuilder.add("the_geom", LineString.class);
        simpleFeatureBuilder.add("linkId", String.class);
        simpleFeatureBuilder.add("lineId", String.class);
        // simpleFeatureBuilder.add("transitStopIdPrevious", String.class); //not unique, could be multiple
        // simpleFeatureBuilder.add("transitStopIdFollowing", String.class); //not unique, could be multiple
        simpleFeatureBuilder.add("departures", Integer.class);
        simpleFeatureBuilder.add("passengers", Double.class);
        simpleFeatureBuilder.add("totVehCapa", Double.class);
        simpleFeatureBuilder.add("loadFactor", Double.class);
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());

        Collection<SimpleFeature> features = new ArrayList<>();
        GeometryFactory geofac = new GeometryFactory();

        for(Map.Entry<Id<Link>, Map<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation>> entryLinkLevel: stop2stopByLinkAndTransitLineAggregates.entrySet()) {
            for (Map.Entry<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation> entryLineLevel : entryLinkLevel.getValue().entrySet()) {
                createLinkFeatureFromStop2StopAggregation(scenario, linkFeatureBuilder, features, geofac, entryLinkLevel.getKey(), entryLineLevel.getKey().toString(), entryLineLevel.getValue());
            }
        }

        // sum per link of all transit lines
        Map<Id<Link>, PtStop2StopAnalysis.Stop2StopAggregation> stop2stopByLinkAggregates = stop2StopEntriesForEachDeparture.stream()
                .flatMap(stop2StopEntry -> stop2StopEntry.linkIdsSincePreviousStop.stream()
                        .map(linkId -> new AbstractMap.SimpleEntry<>(linkId, stop2StopEntry)))
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> new PtStop2StopAnalysis.Stop2StopAggregation(1, entry.getValue().passengersAtArrival, entry.getValue().totalVehicleCapacity),
                        aggregateStop2StopAggregations(),
                        HashMap::new));

        for(Map.Entry<Id<Link>, PtStop2StopAnalysis.Stop2StopAggregation> entryLinkLevel: stop2stopByLinkAggregates.entrySet()) {
            createLinkFeatureFromStop2StopAggregation(scenario, linkFeatureBuilder, features, geofac, entryLinkLevel.getKey(), "all", entryLinkLevel.getValue());
        }

        // TODO: add stops?
        GeoFileWriter.writeGeometries(features, shpFileName);
    }

    public static void writePtStop2StopAnalysisByTransitLine2CsvFile(
            final List<PtStop2StopAnalysis.Stop2StopEntry> stop2StopEntriesForEachDeparture, String fileNameCsv, String separatorCsv) {

        // sum per link and transit line
        Map<Id<Link>, Map<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation>> stop2stopByLinkAndTransitLineAggregates = aggregatePerLinkAndLine(stop2StopEntriesForEachDeparture);

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileNameCsv),
                CSVFormat.DEFAULT.withDelimiter(separatorCsv.charAt(0)).withHeader(headerCsv))
        ) {
            for(Map.Entry<Id<Link>, Map<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation>> entryLinkLevel: stop2stopByLinkAndTransitLineAggregates.entrySet()) {
                for (Map.Entry<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation> entryLineLevel : entryLinkLevel.getValue().entrySet()) {
                    printer.print(entryLinkLevel.getKey());
                    printer.print(entryLineLevel.getKey());
                    printer.print(entryLineLevel.getValue().getDepartures());
                    printer.print(entryLineLevel.getValue().getPassengers());
                    printer.print(entryLineLevel.getValue().getTotalVehicleCapacity());
                    printer.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<Id<Link>, Map<Id<TransitLine>, PtStop2StopAnalysis.Stop2StopAggregation>> aggregatePerLinkAndLine(List<PtStop2StopAnalysis.Stop2StopEntry> stop2StopEntriesForEachDeparture) {
        return stop2StopEntriesForEachDeparture.stream()
                .flatMap(stop2StopEntry -> stop2StopEntry.linkIdsSincePreviousStop.stream()
                        .map(linkId -> new AbstractMap.SimpleEntry<>(linkId, stop2StopEntry)))
                .collect(Collectors.groupingBy(entry -> entry.getKey(), Collectors.toMap(
                        entry -> entry.getValue().transitLineId,
                        entry -> new PtStop2StopAnalysis.Stop2StopAggregation(1, entry.getValue().passengersAtArrival, entry.getValue().totalVehicleCapacity),
                        aggregateStop2StopAggregations(),
                        TreeMap::new)));
    }

    private static void createLinkFeatureFromStop2StopAggregation(Scenario scenario, SimpleFeatureBuilder linkFeatureBuilder, Collection<SimpleFeature> features, GeometryFactory geofac, Id<Link> linkId, String transitLineId, PtStop2StopAnalysis.Stop2StopAggregation stop2StopAggregation) {
        Link link = scenario.getNetwork().getLinks().get(linkId);
        LineString ls = geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
                MGC.coord2Coordinate(link.getToNode().getCoord())});
        Object[] linkFeatureAttributes = new Object[7];
        linkFeatureAttributes[0] = ls;
        linkFeatureAttributes[1] = linkId;
        linkFeatureAttributes[2] = transitLineId;
        linkFeatureAttributes[3] = stop2StopAggregation.getDepartures();
        linkFeatureAttributes[4] = stop2StopAggregation.getPassengers();
        linkFeatureAttributes[5] = stop2StopAggregation.getTotalVehicleCapacity();
        linkFeatureAttributes[6] = stop2StopAggregation.getPassengers() / stop2StopAggregation.getTotalVehicleCapacity();
        try {
            features.add(linkFeatureBuilder.buildFeature(stop2StopAggregation + "_line_" + transitLineId, linkFeatureAttributes));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

}
