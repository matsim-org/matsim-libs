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
package playground.yu.analysis.forZrh;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;
import org.xml.sax.SAXException;

import playground.yu.analysis.CalcLinksAvgSpeed;
import playground.yu.analysis.CalcNetAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;
import playground.yu.analysis.LegDistance;
import playground.yu.analysis.ModeSplit;
import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author ychen
 * 
 */
public class AnalysisTest4Zrh implements Analysis4Zrh {

	private static void printUsage() {
		System.out.println();
		System.out.println("AnalysisTest4Zrh:");
		System.out.println("----------------");
		System.out
				.println("Create an additional analysis for the runs, which were done with only org.matsim.run.Controler");
		System.out.println();
		System.out.println("usage: AnalysisTest args");
		System.out.println(" arg 0:\tname of scenario (for Zurich required)");
		System.out.println(" arg 1:\trunId");
		System.out
				.println(" arg 2:\tname incl. path to net file (.xml[.gz])(required)");
		System.out
				.println(" arg 3:\tpath to events file, plans file, and analysis files, without the last \"/\"");
		System.out.println(" args 4:\twhether there is plansfile (true/false)");
		System.out
				.println(" arg 5:\tsnapshot-period of OTFVis:  Specify how often a snapshot should be taken when reading the events, in seconds.");
		System.out
				.println(" arg 6:\tname incl. path to facilities file (.xml[.gz])(optional)");
		System.out
				.println(" arg 7:\tname incl. path to toll file (.xml)(optional)");
		System.out.println("----------------");
	}

	private static void runIntern(final String[] args, final String scenario) {
		final String netFilename = args[2];

		String[] tmp = args[3].split("it.");
		String outputBase = args[3] + "/" + args[1] + "." + tmp[tmp.length - 1]
				+ ".";
		final String eventsFilename = outputBase + "events.txt.gz";

		boolean hasPop = Boolean.parseBoolean(args[4]);
		String plansFilename = Boolean.parseBoolean(args[4]) ? outputBase
				+ "plans.xml.gz" : null;

		String eventsOutputFilename = outputBase + "events4mvi.txt.gz";
		String outputBase4analysis = outputBase + "analysis"
				+ (scenario.equals(KANTON_ZURICH) ? ".Kanton/" : "/") + args[1]// runId
				+ "." + (scenario.equals("normal") ? "" : scenario + ".");

		String tollFilename = (!scenario.equals(KANTON_ZURICH)) ? null
				: args[args.length - 1];// last parameter, man needs tollFile
		// only for Kanton_Zurich.

		String facilitiesFilename = null;
		if (args.length >= 7) {
			try {
				if (new MatsimFileTypeGuesser(args[6]).getGuessedFileType()
						.equals(FileType.Facilities)) {
					facilitiesFilename = args[6];
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ScenarioImpl s = new ScenarioImpl();

		NetworkLayer network = s.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);
		// facilities
		ActivityFacilitiesImpl af = null;
		if (facilitiesFilename != null) {
			af = s.getActivityFacilities();
			new MatsimFacilitiesReader(af).readFile(facilitiesFilename);
		}
		// toll
		RoadPricingScheme toll = null;
		if (scenario.equals(KANTON_ZURICH)) {
			RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1();
			try {
				tollReader.parse(tollFilename);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			toll = tollReader.getScheme();
		}

		// EventsHandlers with parameter of "Population":
		EnRouteModalSplit4Zrh orms = null;
		LegTravelTimeModalSplit4Zrh lttms = null;
		// PersonAlgorithm
		CalcAverageTripLength catl = null;
		DailyDistance4Zrh dd = null;
		DailyEnRouteTime4Zrh dert = null;
		ModeSplit ms = null;
		LegDistance ld = null;
		// only PersonAlgorithm begins.
		if (plansFilename != null) {
			PopulationImpl population = s.getPopulation();

			catl = new CalcAverageTripLength();
			ms = new ModeSplit(toll);
			orms = new EnRouteModalSplit4Zrh(scenario, population, toll);
			lttms = new LegTravelTimeModalSplit4Zrh(population, toll);
			dd = new DailyDistance4Zrh(toll);
			dert = new DailyEnRouteTime4Zrh(toll);
			ld = new LegDistance(network, toll, population);
			// in future, add some PersonAlgorithm and EventsHandler

			new MatsimPopulationReader(s).readFile(plansFilename);

			catl.run(population);
			dd.run(population);
			dert.run(population);
			ms.run(population);
		} else {
			ld = new LegDistance(network);
		}
		// only PersonAlgorithm ends.
		EventsManagerImpl events = new EventsManagerImpl();
		// EventsHandlers without parameter of "Population":
		CalcTrafficPerformance ctpf = new CalcTrafficPerformance(network, toll);
		CalcNetAvgSpeed cas = new CalcNetAvgSpeed(network, toll);
		CalcLinksAvgSpeed clas = null;
		if (scenario.equals(ZURICH)) {
			clas = new CalcLinksAvgSpeed(network, 682845.0, 247388.0, 2000.0);
		} else if (scenario.equals(KANTON_ZURICH)) {
			clas = new CalcLinksAvgSpeed(network, toll);
		} else {
			clas = new CalcLinksAvgSpeed(network);
		}

		events.addHandler(ctpf);
		events.addHandler(cas);
		events.addHandler(clas);
		events.addHandler(ld);

		if (orms != null)
			events.addHandler(orms);
		if (lttms != null)
			events.addHandler(lttms);

		new MatsimEventsReader(events).readFile(eventsFilename);

		if (orms != null) {

			orms.write(outputBase4analysis + "onRoute.txt");
			orms.writeCharts(outputBase4analysis);
		}
		if (lttms != null) {
			lttms.write(outputBase4analysis + "legtraveltimes.txt.gz");
			lttms.writeCharts(outputBase4analysis + "legtraveltimes");
		}
		clas.write(outputBase4analysis + "avgSpeed.txt.gz");
		clas.writeChart(outputBase4analysis + "avgSpeedCityArea.png");
		ld.write(outputBase4analysis + "legDistances.txt.gz");
		ld.writeCharts(outputBase4analysis + "legDistances");

		SimpleWriter sw = new SimpleWriter(outputBase4analysis + "output.txt");
		sw.write("netfile:\t" + netFilename + "\neventsFile:\t"
				+ eventsFilename + "\noutputpath:\t" + outputBase4analysis
				+ "\n");
		if (catl != null)
			sw.write("avg. Trip length:\t" + catl.getAverageTripLength()
					+ " [m]\n");
		sw.write("traffic performance (car):\t" + ctpf.getTrafficPerformance()
				+ " [Pkm]\n");
		sw.write("avg. speed of the total network (car):\t"
				+ cas.getNetAvgSpeed() + " [km/h]\n");
		sw.close();

		if (dd != null)
			dd.write(outputBase4analysis);
		if (dert != null)
			dert.write(outputBase4analysis);
		if (ms != null)
			ms.write(outputBase4analysis);
		// otfvis
		if (toll == null) {
			SimpleReader sr = new SimpleReader(eventsFilename);
			SimpleWriter sw2 = new SimpleWriter(eventsOutputFilename);

			String line = sr.readLine();
			sw2.writeln(line);
			// after filehead
			double time = 0;
			while (line != null && time < 108000.0) {
				line = sr.readLine();
				if (line != null) {
					sw2.writeln(line);
					time = Double.parseDouble(line.split("\t")[0]);
				}
			}
			sr.close();
			sw2.close();

			new OTFEvent2MVI(new QueueNetwork(network), eventsOutputFilename,
					outputBase
							+ (scenario.equals("normal") ? "" : scenario + ".")
							+ "otfvis.mvi", Integer.parseInt(args[5])// time
			// interval
			).convert();
		}
		System.out.println("done.");
	}

	public static void run(final String[] args) {
		runIntern(args, "normal");
	}

	public static void runZurich(final String[] args) {
		runIntern(args, ZURICH);
	}

	public static void runKantonZurich(String[] args) {
		runIntern(args, KANTON_ZURICH);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length < 5) {
			System.err.println("too few parameters!");
			printUsage();
			System.exit(1);
		} else if (args[0].equals(ZURICH)) {
			runZurich(args);
		} else if (args[0].equals(KANTON_ZURICH)) {
			runKantonZurich(args);
		} else {
			run(args);
		}
	}
}
