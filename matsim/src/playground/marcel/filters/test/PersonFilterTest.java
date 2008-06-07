/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFilterTest.java
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

package playground.marcel.filters.test;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;

import playground.marcel.filters.filter.ActTypeFilter;
import playground.marcel.filters.filter.DepTimeFilter;
import playground.marcel.filters.filter.PersonFilterAlgorithm;
import playground.marcel.filters.filter.PersonIDFilter;
import playground.marcel.filters.filter.finalFilters.PersonIDsExporter;

/**
 * @author ychen
 */
public class PersonFilterTest {

	public static void testRunIDandActTypeundDepTimeFilter() {

		System.out.println("TEST RUN ---FilterTest---:");
		// reading all available input
		System.out.println("  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.createWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Plans plans = new Plans(Plans.USE_STREAMING);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonIDsExporter pid=new PersonIDsExporter();
		DepTimeFilter dtf=new DepTimeFilter();
		ActTypeFilter atf=new ActTypeFilter();
		PersonIDFilter idf=new PersonIDFilter(10);
		PersonFilterAlgorithm pfa=new PersonFilterAlgorithm();
		pfa.setNextFilter(idf);
		idf.setNextFilter(atf);
		atf.setNextFilter(dtf);
		dtf.setNextFilter(pid);
		plans.addAlgorithm(pfa);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		plans.runAlgorithms();
		System.out.println("we have "+pfa.getCount()+"persons at last -- FilterAlgorithm");
		System.out.println("we have "+idf.getCount()+"persons at last -- PersonIDFilter");
		System.out.println("we have "+atf.getCount()+"persons at last -- ActTypeFilter");
		System.out.println("we have "+dtf.getCount()+"persons at last -- DepTimeFilter");
		System.out.println("  done.");
		// writing all available input

		System.out.println("PersonFiterTEST SUCCEEDED.");
		System.out.println();
	}

	/**
	 * @param args test/yu/config_hms.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRunIDandActTypeundDepTimeFilter();
		Gbl.printElapsedTime();
	}
}
