/* *********************************************************************** *
 * project: org.matsim.*
 * PrintFacilityNetworkMatching.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel;

import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.world.World;

import playground.marcel.pt.utils.FacilityNetworkMatching;

public class PrintFacilityNetworkMatching {

	public static void printMatching(final Facilities facilities) {
		for (Facility f : facilities.getFacilities().values()) {
			System.out.println("Facility " + f.getId().toString() + " --> Link " + f.getLink().getId().toString());
		}
	}
	
	public static void printMatching(final String networkFilename, final String facilitiesFilename, final String mappingFilename) {
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);
		FacilitiesImpl facilities = new FacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(facilitiesFilename);
		
		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();
		
		if (mappingFilename != null) {
			FacilityNetworkMatching.loadMapping(facilities, network, world, mappingFilename);
		}
		
		printMatching(facilities);
	}
	
	public static void main(String[] args) {
		printMatching("../thesis-data/examples/minibln/network.xml", "../thesis-data/examples/minibln/facilities.xml", "../thesis-data/examples/minibln/facilityMatching.txt");
//		printMatching(args[0], args[1], args[2]);
	}

	
}
