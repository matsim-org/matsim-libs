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
  
package parking.createParkingNetwork;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


import parking.capacityCalculation.LinkLengthBasedCapacityCalculator;
import parking.capacityCalculation.UseParkingCapacityFromNetwork;

public class CreateParkingNetwork {

	public static void main(String[] args) throws IOException {
		String networkOld = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/parking/bc-run/newnet.xml.gz";
		String networkNew = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/parking/bc-run/newnet_parking.xml.gz";
		String parkCap = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/parking/bc-run/parkCap.csv";
		String csv = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/parking/shp/parkinglocations.csv";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
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
		Set<Coord> parkCoords = new HashSet<>();
		TabularFileParserConfig tbc = new TabularFileParserConfig();
		tbc.setDelimiterTags(new String[] {";"});
		tbc.setFileName(csv);
		new TabularFileParser().parse(tbc, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				parkCoords.add(ct.transform(new Coord(Double.parseDouble(row[1]),Double.parseDouble(row[0]))));
			}
		});
		BufferedWriter bw = IOUtils.getBufferedWriter(parkCap);
		bw.write("linkId;ParkCapacity");
		for (Coord c : parkCoords) {
			Link l;
			do {
			l = NetworkUtils.getNearestLink(network, c);
			if (l.getAllowedModes().contains(TransportMode.car)) break;
			else {
				c = new Coord (c.getX()-10+r.nextInt(20),c.getY()-10+r.nextInt(20));
			}
			} while (true);
			double capacity = (double) l.getAttributes().getAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME);
			capacity += minParkCapacity + r.nextInt(maxParkCapacity-minParkCapacity); 
			l.getAttributes().putAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME, capacity);
			
		}
		network.getLinks().values().forEach(l -> {
			try {
				bw.write("\n"+l.getId()+";"+l.getAttributes().getAttribute(UseParkingCapacityFromNetwork.CAP_ATT_NAME));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		
		bw.flush();
		bw.close();
		new NetworkWriter(network).write(networkNew);
	}
		
}
