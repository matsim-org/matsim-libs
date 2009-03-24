/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSelectedPlansTables.java
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

package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

public class AnalyzeFacilities {

	private Facilities facilities;
	private final static Logger log = Logger.getLogger(AnalyzeFacilities.class);
	
	public void run(Facilities facilities, NetworkLayer network) {
		this.facilities = facilities;

		write("./output/facilities_activities_summary.txt", network);
		System.out.println("finished");
	}

	private void write(String outfile, NetworkLayer network) {

		double[] capacityCountShop = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
		
		String[] NOGA_SHOP = {
				"B015211A",
				"B015211B",
				"B015211C",
				"B015211D",
				"B015211E",
				"B015212A",
				"B015221A",
				"B015222A",
				"B015223A",	
				"B015224A",	
				"B015225A",
				"B015227A",
				"B015227B"
		};
		
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
						
			Iterator<Facility> facility_it = this.facilities.getFacilities("shop").values().iterator();
			while (facility_it.hasNext()) {
				Facility facility = facility_it.next();
				
				ActivityOption actOpt = facility.getActivityOption("shop");							
				
				String desc = "noch nichts"; // actOpt.getDesc();
				for (int i = 0; i < 13; i++) {
					if (desc.contains(NOGA_SHOP[i])) {
						capacityCountShop[i] += actOpt.getCapacity();
					}
				}
			}
			for (int i = 0; i < 13; i++) {		
				out.write("SHOP : Total capacity for " + NOGA_SHOP[i] + ": " + capacityCountShop[i] + "\n");
			}
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
