/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityProducer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.meisterk.facilities;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.algorithms.FacilitiesAllActivitiesFTE;
import org.matsim.facilities.algorithms.FacilitiesOpentimesKTIYear1;
import org.matsim.facilities.algorithms.FacilitiesRandomizeHectareCoordinates;
import org.matsim.gbl.Gbl;

public class FacilityProducer {

	private static void produceFacilitiesKTIYear1() throws Exception {
		
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		
		facilities.setName(
				"org.matsim.playground.meisterk.FacilityProducer::produceFacilitiesKTIYear1()"
				);
		
		System.out.println("  adding and running facilities algorithms... ");
		//facilities.addAlgorithm(new FacilitiesWork9To18());
		facilities.addAlgorithm(new FacilitiesAllActivitiesFTE());
		facilities.addAlgorithm(new FacilitiesOpentimesKTIYear1());
		facilities.runAlgorithms();
		System.out.println("  done.");

		System.out.println("  writing facilities file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilities);
		facilities_writer.write();
		System.out.println("  done.");

	}
	
	private static void resetOpenTimes() {
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		facilities.setName(
				"org.matsim.playground.meisterk.FacilityProducer::resetOpenTimes()"
				);
	
		System.out.println("  reading facilities xml file... ");
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		
		System.out.println("  adding and running facilities algorithms... ");
		facilities.addAlgorithm(new FacilitiesOpentimesKTIYear1());
		facilities.runAlgorithms();
		System.out.println("  done.");

		System.out.println("  writing facilities file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilities);
		facilities_writer.write();
		System.out.println("  done.");

	}
	
	private static void distributeFacilitiesInHectare() {
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		facilities.setName(
				"org.matsim.playground.meisterk.FacilityProducer::distributeFacilitiesInHectare()"
				);
	
		System.out.println("  reading facilities xml file... ");
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		
		System.out.println("  adding and running facilities algorithms... ");
		facilities.addAlgorithm(new FacilitiesRandomizeHectareCoordinates());
		facilities.runAlgorithms();
		System.out.println("  done.");

		System.out.println("  writing facilities file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilities);
		facilities_writer.write();
		System.out.println("  done.");

	}
	
	private static void writeGUESSFile() {
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		System.out.println("  reading facilities xml file... ");
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		
		System.out.println("  adding and running facilities algorithms... ");
		facilities.addAlgorithm(new FacilitiesRandomizeHectareCoordinates());
		facilities.addAlgorithm(new FacilitiesExportToGUESS());
		facilities.runAlgorithms();
		System.out.println("  done.");
		
	}
	
    //////////////////////////////////////////////////////////////////////
    // run method
    //////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

    	//produceFacilitiesKTIYear1();
    	//resetOpenTimes();
    	//distributeFacilitiesInHectare();
    	writeGUESSFile();
		
    	System.out.println();

    }

    //////////////////////////////////////////////////////////////////////
    // main
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws Exception {
    	
		Gbl.createConfig(args);
		Gbl.createWorld();
		
		run();
		
    }
}
