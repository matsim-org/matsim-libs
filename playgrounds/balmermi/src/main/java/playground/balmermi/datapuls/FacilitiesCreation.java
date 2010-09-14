///* *********************************************************************** *
// * project: org.matsim.*
// * FacilitiesCreation.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.balmermi.datapuls;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.ScenarioImpl;
//import org.matsim.core.facilities.ActivityFacilitiesImpl;
//import org.matsim.core.facilities.FacilitiesWriter;
//import org.matsim.core.facilities.MatsimFacilitiesReader;
//
//import playground.balmermi.datapuls.modules.FacilitiesAddDataPulsBuildings;
//import playground.balmermi.datapuls.modules.FacilitiesPrepareEC2000;
//
//public class FacilitiesCreation {
//
//	private final static Logger log = Logger.getLogger(FacilitiesCreation.class);
//
//	//////////////////////////////////////////////////////////////////////
//	// printUsage
//	//////////////////////////////////////////////////////////////////////
//
//	private static void printUsage() {
//		System.out.println();
//		System.out.println("FacilitiesCreation");
//		System.out.println();
//		System.out.println("Usage: FacilitiesCreation enterprizeFacilities ttaFacilities datapulsBuildings outputFacilities");
//		System.out.println();
//		System.out.println("---------------------");
//		System.out.println("2009, matsim.org");
//		System.out.println();
//	}
//
//	//////////////////////////////////////////////////////////////////////
//	// main
//	//////////////////////////////////////////////////////////////////////
//
//	public static void main(String[] args) {
//		if (args.length != 4) { printUsage(); return; }
//		
//		ScenarioImpl scenario = new ScenarioImpl();
//		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
//		new MatsimFacilitiesReader(scenario).readFile(args[0].trim());
//		log.info("number of facilities ec2000: "+facilities.getFacilities().size());
//
//		new FacilitiesPrepareEC2000().run(facilities);
//		log.info("number of facilities ec2000: "+facilities.getFacilities().size());
//
//		ScenarioImpl ttaScenario = new ScenarioImpl();
//		ActivityFacilitiesImpl facilities_tta = ttaScenario.getActivityFacilities();
//		new MatsimFacilitiesReader(ttaScenario).readFile(args[1].trim());
//		log.info("number of facilities tta: "+facilities_tta.getFacilities().size());
//		
//		facilities.getFacilities().putAll(facilities_tta.getFacilities());
//		facilities_tta = null;
//		log.info("number of facilities ec2000 & tta: "+facilities.getFacilities().size());
//
//		new FacilitiesAddDataPulsBuildings(args[2]).run(facilities);
//		log.info("number of facilities ec2000 & tta & datapuls: "+facilities.getFacilities().size());
//
//		new FacilitiesWriter(facilities).write(args[3]);
//	}
//}
