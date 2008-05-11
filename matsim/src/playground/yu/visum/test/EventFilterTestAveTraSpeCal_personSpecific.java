/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterTestAveTraSpeCal_personSpecific.java
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
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;

import playground.yu.visum.filter.EventFilterAlgorithm;
import playground.yu.visum.filter.EventFilterPersonSpecific;
import playground.yu.visum.filter.PersonFilterAlgorithm;
import playground.yu.visum.filter.PersonIDFilter;
import playground.yu.visum.filter.finalFilters.AveTraSpeCal;
import playground.yu.visum.filter.finalFilters.PersonIDsExporter;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * This class offers a test, that contains: [to create Network object] [to read
 * networkfile] [to create plans object] [to set plans algorithms
 * (PersonFilterAlgorithm, PersonIDsExporter)] [to create events reader] [to
 * read plans file] [to running plans algorithms] [to set events algorithms
 * (EventFilterAlgorithm, EventFilterPersonSpecific, AveTraSpeCal)] [to read
 * events file] [to run events algorithms] [to print additiv netFile of
 * Visum...] [to print attributsFile of link...]
 *
 * @author yu chen
 */
public class EventFilterTestAveTraSpeCal_personSpecific {

	/**
	 * @param args
	 *            "test/yu/config_hm.xml config_v1.dtd"
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRunAveTraSpeCal();
		Gbl.printElapsedTime();
	}

	/**
	 * @throws IOException
	 */

	public static void testRunAveTraSpeCal() throws IOException {

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

		System.out.println("  setting plans algorithms... ");
		PersonIDFilter pidf = new PersonIDFilter(100);
		PersonIDsExporter pide = new PersonIDsExporter();
		PersonFilterAlgorithm pfa = new PersonFilterAlgorithm();
		pidf.setNextFilter(pide);
		pfa.setNextFilter(pidf);
		plans.addAlgorithm(pfa);
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		Events events = new Events();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		plans.runAlgorithms();
		System.out.println("we have " + pfa.getCount()
				+ "persons at last -- FilterAlgorithm");
		System.out.println("we have " + pide.getCount()
				+ "persons at last -- DepTimeFilter");
		System.out.println("  done.");

		System.out.println("  setting events algorithms...");
		EventFilterPersonSpecific efpsc = new EventFilterPersonSpecific(pide
				.idSet());
		AveTraSpeCal atsc = new AveTraSpeCal(plans, network);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(efpsc);
		efpsc.setNextFilter(atsc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos
		System.out.println("  reading events file and running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("we have\t" + atsc.getCount()
				+ "\tevents\tat last -- AveTraSpeCal.");
		System.out.println("  done.");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam(
				"attribut_aveTraSpe", "outputAttNetFile"));
		psUdaNet.output(atsc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamLinkATT psLinkAtt = new PrintStreamLinkATT(config.getParam(
				"attribut_aveTraSpe", "outputAttFile"), network);
		psLinkAtt.output(atsc);
		psLinkAtt.close();
		System.out.println("\tdone.");
	}
}
