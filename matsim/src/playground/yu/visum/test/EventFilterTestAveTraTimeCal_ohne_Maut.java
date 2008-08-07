/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterTestAveTraSpeCal_ohne_Maut.java
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

package playground.yu.visum.test;

import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;
import org.matsim.world.World;

import playground.yu.visum.filter.EventFilterAlgorithm;
import playground.yu.visum.filter.finalFilters.AveTraTimeCal;
import playground.yu.visum.writer.PrintStreamATTA;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * @author yu chen
 */
public class EventFilterTestAveTraTimeCal_ohne_Maut {

	public static void testRunAveTraTimeCal() throws IOException {

		World world = Gbl.getWorld();
		Config config = Gbl.getConfig();

		// network
		System.out.println("  creating network object... ");
		NetworkLayer network = (NetworkLayer) world.createLayer(
				NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");

		System.out.println("  reading network file... ");
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");
		// plans
		System.out.println("  creating plans object... ");
		Plans plans = new Plans(Plans.USE_STREAMING);
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		Events events = new Events();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		// TravelTimeCalculator ttcAlgo = new TravelTimeCalculator(network);
		// EventDepTimeFilter edtf=new EventDepTimeFilter();
		// VirtulLastEventFilter vlef=new VirtulLastEventFilter();
		// EventIDFilter eidf=new EventIDFilter();
		AveTraTimeCal attc = new AveTraTimeCal(plans, network);
		// edtf.setNextEventFilter(attc);
		// eidf.setNextEventFilter(attc);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(attc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos if streaming is on
		System.out
				.println("  reading events file and (probably) running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("we have " + efa.getCount()
				+ " events at last -- EventFilterAlgorithm.");
		System.out.println("we have " + attc.getCount()
				+ " events at last -- AveTraTimeCal.");
		System.out.println("  done.");

		// run algos if needed, only if streaming is off
		System.out
				.println("  running events algorithms if they weren't already while reading the events...");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam(
				"attribut_aveTraTime", "outputAttNetFile"));
		psUdaNet.output(attc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamATTA psLinkAtt = new PrintStreamLinkATT(config.getParam(
				"attribut_aveTraTime", "outputAttFile"), network);
		psLinkAtt.output(attc);
		psLinkAtt.close();
		System.out.println("  done.");
	}

	/**
	 * @param args -
	 *            test/yu/config_hm_ohne_Maut.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRunAveTraTimeCal();
		Gbl.printElapsedTime();
	}
}
