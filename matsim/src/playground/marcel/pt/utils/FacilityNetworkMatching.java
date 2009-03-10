/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityNetworkMatching.java
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

package playground.marcel.pt.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

public class FacilityNetworkMatching {

	public static void dumpMapping(final Facilities facilities, final String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			for (Facility f : facilities.getFacilities().values()) {
				if (f.getLink() != null) {
					writer.write(f.getId().toString());
					writer.write('\t');
					writer.write(f.getLink().getId().toString());
					writer.write('\n');
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void loadMapping(final Facilities facilities, final Network network, final World world, final String filename) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, '\t');
				Facility f = facilities.getFacilities().get(new IdImpl(parts[0]));
				Link l = network.getLinks().get(new IdImpl(parts[1]));
				if (f == null || l == null) {
					System.err.println("Could not load facility of link: " + line);
				} else {
					facilitySetLink(f, l, world);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void facilitySetLink(final Facility f, final Link l, final World world) {
		Link oldL = f.getLink();
		if (oldL != null) {
			world.removeMapping(f, oldL);
		}
		world.addMapping(f, l);
	}
}
