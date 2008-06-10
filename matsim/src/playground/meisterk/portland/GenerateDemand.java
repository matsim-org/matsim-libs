/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateDemand.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.portland;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;

public class GenerateDemand {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);
		GenerateDemand.fusePlansAndFacilities();

	}

	private static void fusePlansAndFacilities() {

//		World world = Gbl.createWorld();

		System.out.println("Reading plans...");
		Plans plans = new Plans(false);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		System.out.println("Reading plans...done.");

		System.out.println("Reading facilities...");
		Facilities facilities = new Facilities();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilities);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(Gbl.getConfig().facilities().getInputFile());
		facilities.printFacilitiesCount();
		System.out.println("Reading facilities...done.");
		
	}
	
}
