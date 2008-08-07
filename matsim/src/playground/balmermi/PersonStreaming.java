/* *********************************************************************** *
 * project: org.matsim.*
 * PersonStreaming.java
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

package playground.balmermi;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;
import org.matsim.population.PlansWriter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

import playground.balmermi.algos.PersonXY2Facility;

public class PersonStreaming {

	public static void run() {

		System.out.println("person streaming...");
		
		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		Gbl.getWorld().complete();
		System.out.println("  done.");

		System.out.println("  reading the network xml file...");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		Gbl.getWorld().complete();
		System.out.println("  done.");

		new WorldCheck().run(Gbl.getWorld());
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		new WorldCheck().run(Gbl.getWorld());

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Plans plans = new Plans(Plans.USE_STREAMING);
		PlansWriter plansWriter = new PlansWriter(plans);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new PersonXY2Facility(facilities));
//		PersonSubTourAnalysis psta = new PersonSubTourAnalysis();
//		plans.addAlgorithm(psta);
//		PersonInitDemandSummaryTable pidst = new PersonInitDemandSummaryTable("output/output_persons.txt");
//		plans.addAlgorithm(pidst);
//		plans.addAlgorithm(new PersonCalcTripDistances());
//		PersonTripSummaryTable ptst = new PersonTripSummaryTable("output/output_trip-summary-table.txt");
//		plans.addAlgorithm(ptst);
		
//		DoAndUndo dau = new DoAndUndo();
//		plans.addAlgorithm(dau);

//		plans.addAlgorithm(new XY2Links(network));
//		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
//		PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
//		preprocess.run(network);
//		plans.addAlgorithm(new PlansCalcRouteLandmarks(network, preprocess, timeCostCalc, timeCostCalc));
		
//		plans.addAlgorithm(dau);

//		PersonLinkRoutesTable plrt = new PersonLinkRoutesTable("output/linkroutes");
//		plans.addAlgorithm(plrt);
		System.out.println("  done.");
		
		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  finishing algorithms... ");
//		psta.writeSubtourTripCntVsModeCnt("output/TripsPerSubtourVsModeCnt.txt");
//		psta.writeSubtourDistVsModeCnt("output/SubtourDistVsModeCnt.txt");
//		psta.writeSubtourTripCntVsSubtourCnt("output/SubtourTripCntVsSubtourCnt.txt");
//		psta.writeSubtourDistVsModeDistSum("output/SubtourDistVsModeDistSum.txt");
//		pidst.close();
//		ptst.close();
//		plrt.close();
//		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		run();

		Gbl.printElapsedTime();
	}
}
