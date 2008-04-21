/* *********************************************************************** *
 * project: org.matsim.*
 * NewPlansTest.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.MatsimWorldReader;

import playground.marcel.filters.filter.PersonFilterAlgorithm;
import playground.marcel.filters.filter.PersonRouteFilter;
import playground.marcel.filters.filter.finalFilters.NewPlansWriter;

/**
 * @author ychen
 */
public class NewPlansTest extends Plans{

	public static void testRun() {

		System.out.println("TEST RUN ---FilterTest---:");
		// reading all available input

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Plans plans = new Plans(Plans.USE_STREAMING);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonFilterAlgorithm pfa=new PersonFilterAlgorithm();
		List<Id> linkIDs=new ArrayList<Id>();
		List<Id> nodeIDs = new ArrayList<Id>();
		linkIDs.add(new IdImpl(101589));
		linkIDs.add(new IdImpl(27469));
		linkIDs.add(new IdImpl(102086));
		linkIDs.add(new IdImpl(101867));
		linkIDs.add(new IdImpl(102235));
		linkIDs.add(new IdImpl(101944));
		linkIDs.add(new IdImpl(101866));
		linkIDs.add(new IdImpl(102224));
		linkIDs.add(new IdImpl(102015));
		linkIDs.add(new IdImpl(101346));
		linkIDs.add(new IdImpl(101845));
		linkIDs.add(new IdImpl(101487));
		linkIDs.add(new IdImpl(102016));
		linkIDs.add(new IdImpl(101417));
		linkIDs.add(new IdImpl(102225));
		linkIDs.add(new IdImpl(100970));
		linkIDs.add(new IdImpl(102234));
		linkIDs.add(new IdImpl(101588));
		linkIDs.add(new IdImpl(101945));
		linkIDs.add(new IdImpl(102087));
		linkIDs.add(new IdImpl(100971));
		linkIDs.add(new IdImpl(102017));
		linkIDs.add(new IdImpl(102226));
		linkIDs.add(new IdImpl(102160));
		linkIDs.add(new IdImpl(27470));
		linkIDs.add(new IdImpl(101804));
		linkIDs.add(new IdImpl(101416));
		linkIDs.add(new IdImpl(102083));
		linkIDs.add(new IdImpl(102004));
		linkIDs.add(new IdImpl(102014));
		linkIDs.add(new IdImpl(102227));
		linkIDs.add(new IdImpl(27789));
		linkIDs.add(new IdImpl(102170));
		linkIDs.add(new IdImpl(100936));
		linkIDs.add(new IdImpl(101347));
		linkIDs.add(new IdImpl(101805));
		linkIDs.add(new IdImpl(101844));
		linkIDs.add(new IdImpl(102082));
		linkIDs.add(new IdImpl(102171));
		linkIDs.add(new IdImpl(102161));
		linkIDs.add(new IdImpl(100937));
		linkIDs.add(new IdImpl(102131));
		linkIDs.add(new IdImpl(101784));
		linkIDs.add(new IdImpl(102176));
		linkIDs.add(new IdImpl(27736));
		linkIDs.add(new IdImpl(101785));
		linkIDs.add(new IdImpl(27790));
		linkIDs.add(new IdImpl(102130));
		linkIDs.add(new IdImpl(27735));
		linkIDs.add(new IdImpl(102177));
		linkIDs.add(new IdImpl(102005));
		linkIDs.add(new IdImpl(101486));

		nodeIDs.add(new IdImpl(990262));
		nodeIDs.add(new IdImpl(990340));
		nodeIDs.add(new IdImpl(630401));
		nodeIDs.add(new IdImpl(990253));
		nodeIDs.add(new IdImpl(680303));
		nodeIDs.add(new IdImpl(990218));
		nodeIDs.add(new IdImpl(720464));
		nodeIDs.add(new IdImpl(610604));
		nodeIDs.add(new IdImpl(690447));
		nodeIDs.add(new IdImpl(660374));
		nodeIDs.add(new IdImpl(530030));
		nodeIDs.add(new IdImpl(990266));
		nodeIDs.add(new IdImpl(990285));
		nodeIDs.add(new IdImpl(990311));
		nodeIDs.add(new IdImpl(990370));
		nodeIDs.add(new IdImpl(690611));
		nodeIDs.add(new IdImpl(990378));
		nodeIDs.add(new IdImpl(990683));
		nodeIDs.add(new IdImpl(990204));
		nodeIDs.add(new IdImpl(710012));
		nodeIDs.add(new IdImpl(990222));
		nodeIDs.add(new IdImpl(990217));

		PersonRouteFilter prf = new PersonRouteFilter(linkIDs, nodeIDs);
		NewPlansWriter npw=new NewPlansWriter(plans);
		pfa.setNextFilter(prf);
		prf.setNextFilter(npw);
		plans.addAlgorithm(pfa);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		plans.runAlgorithms();
		npw.writeEndPlans();
//		//////////////////////////////////////////////////////////////////////////////////////////////////

		System.out.println("we have "+pfa.getCount()+"persons at last -- PersonFilterAlgorithm.");
		System.out.println("we have "+prf.getCount()+"persons at last -- PersonRouteFilter.");
		System.out.println("we have "+npw.getCount()+"persons at last -- NewPlansWriter.");
		System.out.println("  done.");
		// writing all available input

		System.out.println("NewPlansWriter SUCCEEDED.");
	}

	/**
	 * @param args
	 *            test/yu/config_newPlan.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRun();
		Gbl.printElapsedTime();
	}
}
