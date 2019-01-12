/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package vwExamples.utils;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.File;
import java.util.Map;


public class CreateStopsFromGrid {
    private static final Logger LOG = Logger.getLogger(CreateStopsFromGrid.class);
    static double gridCellSize = 300; //Default gridCellSize

    public static void main(String[] args) {

        String networkFile = args[0];
        String drtTag = args[1];
        gridCellSize = Double.parseDouble(args[2]);

        CreateStopsFromGrid.run(networkFile, gridCellSize, drtTag);

    }

    public static void run(String networkFile, double gridCellSize, String drtTag) {
        LOG.info("Creating DRT Virtual Stops" + "Rasterdistance: " + gridCellSize);
        File networkFilePath = new File(networkFile);
        //String networkFile = "D:\\Matsim\\Axer\\BSWOB2.0\\input\\network\\drtServiceAreaNetwork.xml.gz";
        //final String networkModeDesignator = "drt";
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);
        String transitStopsOutputFile = networkFilePath.getParent() + "\\virtualStops.xml";
        TransitScheduleFactory f = new TransitScheduleFactoryImpl();
        TransitSchedule schedule = f.createTransitSchedule();

        NetworkFilterManager nfm = new NetworkFilterManager(network);
        nfm.addLinkFilter(new NetworkLinkFilter() {
            @Override
            public boolean judgeLink(Link l) {
                if (l.getAllowedModes().contains(drtTag)) {
                    if ((l.getFreespeed() >= 5) && l.getFreespeed() < 20 && l.getLength() > 15)
                        return true;
                }

                return false;
            }
        });
        Network stopNetwork = nfm.applyFilters();

        if (stopNetwork.getLinks().isEmpty()) {
            throw new RuntimeException("No links with networkModeDesignator " + drtTag + " found");
        }

        Map<String, Geometry> netGrid = DrtGridUtils.createGridFromNetwork(stopNetwork, gridCellSize);
        for (Geometry g : netGrid.values()) {
            Coord centroid = MGC.point2Coord(g.getCentroid());
            Link link = NetworkUtils.getNearestLink(stopNetwork, centroid);
            TransitStopFacility stop = f.createTransitStopFacility(
                    Id.create(link.getId().toString() + "_stop", TransitStopFacility.class), link.getCoord(), false);
            stop.setLinkId(link.getId());
            if (!schedule.getFacilities().containsKey(stop.getId())) {
                schedule.addStopFacility(stop);
            }
            Link backLink = NetworkUtils.findLinkInOppositeDirection(link);
            if (backLink != null) {
                TransitStopFacility backstop = f.createTransitStopFacility(
                        Id.create(backLink.getId().toString() + "_stop", TransitStopFacility.class),
                        backLink.getCoord(), false);
                backstop.setLinkId(backLink.getId());
                if (!schedule.getFacilities().containsKey(backstop.getId())) {
                    schedule.addStopFacility(backstop);
                }
            }

        }
        new TransitScheduleWriter(schedule).writeFile(transitStopsOutputFile);
        //writeShape(transitStopsOutputFile.replace(".xml", ".shp"), netGrid);

    }

//	private static void writeShape(String outfile, Map<String, Geometry> zones) {
//
//		CoordinateReferenceSystem crs;
//
//		crs = MGC.getCRS("EPSG:25832");
//
//		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class).setCrs(crs)
//				.setName("zone").create();
//
//		List<SimpleFeature> features = new ArrayList<>();
//
//		for (Entry<String, Geometry> z : zones.entrySet()) {
//			Object[] attribs = new Object[1];
//
//			attribs[0] = z.getKey();
//
//			features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
//		}
//
//		ShapeFileWriter.writeGeometries(features, outfile);
//
//	}
}
