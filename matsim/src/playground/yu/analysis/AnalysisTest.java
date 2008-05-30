/* *********************************************************************** *
 * project: org.matsim.*
 * AvgTolledTripLengthControler.java
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

/**
 *
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.io.IOUtils;

/**
 * @author ychen
 * 
 */
public class AnalysisTest {
	private static void printUsage() {
		System.out.println();
		System.out.println("AnalysisTest:");
		System.out.println("----------------");
		System.out
				.println("Create an additional analysis for the runs, which were done with only org.matsim.controler.Controler");
		System.out.println();
		System.out.println("usage: AnalysisTest args");
		System.out
				.println(" arg 0: name incl. path to net file (.xml[.gz])(required)");
		System.out
				.println(" arg 1: name incl. to events file (.txt[.gz])(required)");
		System.out.println(" arg 2: path to output file (.txt[.gz])(required)");
		System.out
				.println(" arg 3: name incl. path to plans file (.xml[.gz])(optional)");
		System.out.println("----------------");
	}

	private static BufferedWriter writer;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length < 3) {
			printUsage();
			System.exit(0);
		}

		final String netFilename = args[0];
		final String eventsFilename = args[1];
		final String outputpath = args[2];
		String plansFilename = null;
		if (args.length == 4)
			plansFilename = args[3];

		Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(network);

		OnRouteModalSplit orms = null;
		TravelTimeModalSplit ttms = null;
		CalcAverageTripLength catl = null;

		if (plansFilename != null) {
			Plans plans = new Plans();

			catl = new CalcAverageTripLength();
			plans.addAlgorithm(catl);

			PlansReaderI plansReader = new MatsimPlansReader(plans);
			plansReader.readFile(plansFilename);
			plans.runAlgorithms();

			orms = new OnRouteModalSplit(network, plans);
			ttms = new TravelTimeModalSplit(network, plans);
		}

		Events events = new Events();

		CalcTrafficPerformance ctpf = new CalcTrafficPerformance(network);
		CalcNetAvgSpeed cas = new CalcNetAvgSpeed(network);
		CalcLinkAvgSpeed clas = new CalcLinkAvgSpeed(network);
		LegDistance ld = new LegDistance(network);

		events.addHandler(ctpf);
		events.addHandler(cas);
		events.addHandler(clas);
		events.addHandler(ld);

		if (orms != null)
			events.addHandler(orms);
		if (ttms != null)
			events.addHandler(ttms);

		new MatsimEventsReader(events).readFile(eventsFilename);

		if (orms != null) {
			orms.write(outputpath + "onRoute.txt.gz");
			orms.writeCharts(outputpath + "onRoute.png");
		}
		if (ttms != null) {
			ttms.write(outputpath + "traveltimes.txt.gz");
			ttms.writeCharts(outputpath + "traveltimes");
		}
		clas.write(outputpath + "avgSpeed.txt.gz");
		clas.writeChart(outputpath + "avgSpeedCityArea.png");
		ld.write(outputpath + "legDistances.txt.gz");
		ld.writeCharts(outputpath + "legDistances");

		try {
			writer = IOUtils.getBufferedWriter(outputpath + "output.txt");
			writer.write("netfile:\t" + netFilename + "\neventsFile:\t"
					+ eventsFilename + "\noutputpath:\t" + outputpath + "\n");
			if (catl != null)
				writer.write("avg. Trip length:\t"
						+ catl.getAverageTripLength() + " [m]\n");
			writer.write("traffic performance:\t"
					+ ctpf.getTrafficPerformance() + " [Pkm]\n");
			writer.write("avg. speed of the total network:\t"
					+ cas.getNetAvgSpeed() + " [km/h]\n");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
