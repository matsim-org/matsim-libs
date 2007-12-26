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
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.identifiers.IdI;
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
		NetworkLayerBuilder
				.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
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
		List<IdI> linkIDs=new ArrayList<IdI>();
		List<IdI> nodeIDs = new ArrayList<IdI>();
		linkIDs.add(new Id(101589));
		linkIDs.add(new Id(27469));
		linkIDs.add(new Id(102086));
		linkIDs.add(new Id(101867));
		linkIDs.add(new Id(102235));
		linkIDs.add(new Id(101944));
		linkIDs.add(new Id(101866));
		linkIDs.add(new Id(102224));
		linkIDs.add(new Id(102015));
		linkIDs.add(new Id(101346));
		linkIDs.add(new Id(101845));
		linkIDs.add(new Id(101487));
		linkIDs.add(new Id(102016));
		linkIDs.add(new Id(101417));
		linkIDs.add(new Id(102225));
		linkIDs.add(new Id(100970));
		linkIDs.add(new Id(102234));
		linkIDs.add(new Id(101588));
		linkIDs.add(new Id(101945));
		linkIDs.add(new Id(102087));
		linkIDs.add(new Id(100971));
		linkIDs.add(new Id(102017));
		linkIDs.add(new Id(102226));
		linkIDs.add(new Id(102160));
		linkIDs.add(new Id(27470));
		linkIDs.add(new Id(101804));
		linkIDs.add(new Id(101416));
		linkIDs.add(new Id(102083));
		linkIDs.add(new Id(102004));
		linkIDs.add(new Id(102014));
		linkIDs.add(new Id(102227));
		linkIDs.add(new Id(27789));
		linkIDs.add(new Id(102170));
		linkIDs.add(new Id(100936));
		linkIDs.add(new Id(101347));
		linkIDs.add(new Id(101805));
		linkIDs.add(new Id(101844));
		linkIDs.add(new Id(102082));
		linkIDs.add(new Id(102171));
		linkIDs.add(new Id(102161));
		linkIDs.add(new Id(100937));
		linkIDs.add(new Id(102131));
		linkIDs.add(new Id(101784));
		linkIDs.add(new Id(102176));
		linkIDs.add(new Id(27736));
		linkIDs.add(new Id(101785));
		linkIDs.add(new Id(27790));
		linkIDs.add(new Id(102130));
		linkIDs.add(new Id(27735));
		linkIDs.add(new Id(102177));
		linkIDs.add(new Id(102005));
		linkIDs.add(new Id(101486));

		nodeIDs.add(new Id(990262));
		nodeIDs.add(new Id(990340));
		nodeIDs.add(new Id(630401));
		nodeIDs.add(new Id(990253));
		nodeIDs.add(new Id(680303));
		nodeIDs.add(new Id(990218));
		nodeIDs.add(new Id(720464));
		nodeIDs.add(new Id(610604));
		nodeIDs.add(new Id(690447));
		nodeIDs.add(new Id(660374));
		nodeIDs.add(new Id(530030));
		nodeIDs.add(new Id(990266));
		nodeIDs.add(new Id(990285));
		nodeIDs.add(new Id(990311));
		nodeIDs.add(new Id(990370));
		nodeIDs.add(new Id(690611));
		nodeIDs.add(new Id(990378));
		nodeIDs.add(new Id(990683));
		nodeIDs.add(new Id(990204));
		nodeIDs.add(new Id(710012));
		nodeIDs.add(new Id(990222));
		nodeIDs.add(new Id(990217));

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
