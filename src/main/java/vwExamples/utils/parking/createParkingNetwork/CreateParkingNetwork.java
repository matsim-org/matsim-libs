/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.parking.createParkingNetwork;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import vwExamples.utils.parking.capacityCalculation.LinkLengthBasedCapacityCalculator;
import vwExamples.utils.parking.capacityCalculation.UseParkingCapacityFromNetwork;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author steffenaxer
 * This class adds parking capacities to an existing network file
 * It requires as input a parkingLocationsFile which contains only longitude and latitude information
 * Each entry in parkingLocationsFile is a parking area. The capacity is scaled with minParkCapacity and maxParkCapacity
 */

public class CreateParkingNetwork {

    public Map<String, Double> customCapacityLinks;

    public CreateParkingNetwork() {
        this.customCapacityLinks = new HashMap<>();
    }

    public void run(String parkingLocationsFile, String inputnetwork, String outputnetwork) throws IOException {
        // Input
        String networkOld = inputnetwork;
        String networkNew = outputnetwork;

        // Manual information about parking locations, only GPS WGS84 Coordinates
        // String csv = "D:\\Matsim\\Axer\\BSWOB2.0\\input\\shp\\parkinglocations.csv";
        String csv = parkingLocationsFile;

        // Output
        //String parkCap = "D:\\Matsim\\Axer\\BSWOB2.0\\input\\shp\\parkCap.csv";

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                "EPSG:25832");

        // For each parking great parking area we generate a parking capacity that is between 60 - 300 slots
        int minParkCapacity = 60;
        int maxParkCapacity = 300;
        Random r = MatsimRandom.getRandom();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkOld);
        LinkLengthBasedCapacityCalculator calc = new LinkLengthBasedCapacityCalculator();
        for (Link l : network.getLinks().values()) {
            if (l.getAllowedModes().contains(TransportMode.car)) {
                double capacity = calc.getLinkCapacity(l);
                l.getAttributes().putAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME, capacity);
            }

        }

        //Overwrite specific links with custom parking capacities
        for (Entry<String, Double> parklink : customCapacityLinks.entrySet()) {
            Id<Link> parklinkId = Id.createLinkId(parklink.getKey());
            network.getLinks().get(parklinkId).getAttributes().putAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME, parklink.getValue());
        }


        Set<Coord> parkCoords = new HashSet<>();
        TabularFileParserConfig tbc = new TabularFileParserConfig();
        tbc.setDelimiterTags(new String[]{";"});
        tbc.setFileName(csv);
        new TabularFileParser().parse(tbc, new TabularFileHandler() {

            @Override
            public void startRow(String[] row) {
                parkCoords.add(ct.transform(new Coord(Double.parseDouble(row[1]), Double.parseDouble(row[0]))));
            }
        });

        for (Coord c : parkCoords) {
            Link l;
            do {
                l = NetworkUtils.getNearestLink(network, c);
                if (l.getAllowedModes().contains(TransportMode.car))
                    break;
                else {
                    c = new Coord(c.getX() - 10 + r.nextInt(20), c.getY() - 10 + r.nextInt(20));
                }
            } while (true);
            double capacity = (double) l.getAttributes().getAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME);
            capacity += minParkCapacity + r.nextInt(maxParkCapacity - minParkCapacity);
            l.getAttributes().putAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME, capacity);

        }

        // Write CSV file with parking spots and capacity
//		{
//			BufferedWriter bw = IOUtils.getBufferedWriter(parkCap);
//			bw.write("linkId;ParkCapacity");
//			network.getLinks().values().forEach(l -> {
//				try {
//					bw.write("\n" + l.getId() + ";"
//							+ l.getAttributes().getAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME));
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			});
//
//			bw.flush();
//			bw.close();
//		}

        new NetworkWriter(network).write(networkNew);

    }

    public void main(String[] args) throws IOException {

    }

}
