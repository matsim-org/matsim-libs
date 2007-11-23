/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeCreator.java
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

package playground.fabrice.scenariogen;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.world.MatsimWorldReader;

import playground.fabrice.scenariogen.algorithms.PlansKnowledgeCreator;


public class KnowledgeCreator {

	// ////////////////////////////////////////////////////////////////////
	// Create Knowledge by looking at plans and mapping Act -> Facility
	// ////////////////////////////////////////////////////////////////////

	public KnowledgeCreator() {

		System.out.println("KnowledgeCreator():");

		showMem();
		
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		showMem();
		
		System.out.println("  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");
		


		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		showMem();
		
		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		
		showMem();
	
		System.out.println("  creating plans object... ");
		Plans plans = new Plans();
		System.out.println("  done.");

		System.out.println("  creating plans writer object... ");
		PlansWriter plans_writer = new PlansWriter(plans);
		plans.setPlansWriter(plans_writer);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  adding person knowledge... ");
		plans.addAlgorithm( new PlansKnowledgeCreator());
		plans.runAlgorithms();
		System.out.println("  done.");

		System.out.println("  writing plans xml file... ");
		plans_writer.write();
		System.out.println("  done.");

		System.out.println("Plans Knowledge created.");
		System.out.println();
	}

	// ////////////////////////////////////////////////////////////////////
	// main
	// ////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		Gbl.createConfig(args);
		
		new KnowledgeCreator();

		Gbl.printElapsedTime();
	}
	
	void showMem(){
		long freemem = Runtime.getRuntime().freeMemory();
		long totmem  = Runtime.getRuntime().totalMemory();
		freemem = freemem >> 20;
		totmem = totmem >> 20;
		System.out.println("Memory (Mb): " + (totmem-freemem) + " / " + totmem );
	}
}
