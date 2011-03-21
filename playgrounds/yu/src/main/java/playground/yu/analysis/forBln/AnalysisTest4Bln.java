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
package playground.yu.analysis.forBln;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vis.otfvis.OTFEvent2MVI;
import org.xml.sax.SAXException;

import playground.yu.analysis.CalcLinksAvgSpeed;
import playground.yu.analysis.CalcNetAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;
import playground.yu.analysis.EnRouteModalSplit;
import playground.yu.analysis.LegDistance;
import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.ModeSplit;
import playground.yu.analysis.MyCalcAverageTripLength;
import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author ychen
 *
 */
public class AnalysisTest4Bln implements Analysis4Bln {
	private static boolean withToll = false;

	private static void printUsage() {
		System.out.println();
		System.out.println("AnalysisTest:");
		System.out.println("----------------");
		System.out
				.println("Create an additional analysis for the runs, which were done with only org.matsim.controler.Controler");
		System.out.println();
		System.out.println("usage: AnalysisTest args");
		System.out
				.println(" arg 0: name incl. path to network file (.xml[.gz])(required)");
		System.out
				.println(" arg 1: name incl. path to events file (.txt[.gz])(required)");
		System.out.println(" arg 2: path to output file (required)");
		System.out
				.println(" arg 3: name incl. path to plans file (.xml[.gz])(optional)");
		System.out
				.println(" arg 4: name of scenario (optional, for Zurich required)");
		System.out
				.println(" arg 5: name incl. path to toll file (.xml)(optional)");
		System.out
				.println(" arg 6: snapshot-period:  Specify how often a snapshot should be taken when reading the events, in seconds.");
		System.out.println(" arg 7: runId");
		System.out.println("----------------");
	}

	private static void runIntern(final String[] args, final String scenario) {
		final String netFilename = args[0];
		final String eventsFilename = args[1];
		String eventsOutputFilename = args[1].replaceFirst("events",
				"events4mvi");
		String outputBase = args[2] + args[args.length - 1] + "."
				+ (scenario.equals("normal") ? "" : scenario + ".");
		String plansFilename = null;
		if (args.length >= 4) {
			if (args[3].endsWith("xml") || args[3].endsWith("xml.gz"))
				plansFilename = args[3];
		}
		String tollFilename = (withToll) ? args[args.length - 3] : null;

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		// toll
		RoadPricingScheme toll = null;
		if (withToll) {
			sc.getConfig().scenario().setUseRoadpricing(true);
			try {
				new RoadPricingReaderXMLv1(sc.getRoadPricingScheme())
						.parse(tollFilename);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			toll = sc.getRoadPricingScheme();
		}

		// EventsHandlers with parameter of "Population":
		EnRouteModalSplit orms = null;
		LegTravelTimeModalSplit lttms = null;
		// PersonAlgorithm
		CalcAverageTripLength catl = null;
		DailyDistance4Bln dd = null;
		DailyEnRouteTime4Bln dert = null;
		ModeSplit ms = null;
		LegDistance ld = null;
		// only PersonAlgorithm begins.
		if (plansFilename != null) {
			Population population = sc.getPopulation();

			catl = new MyCalcAverageTripLength(network);
			ms = new ModeSplit(toll);
			orms = new EnRouteModalSplit(scenario, population, toll);
			lttms = new LegTravelTimeModalSplit(population, toll);
			dd = new DailyDistance4Bln(toll, network);
			dert = new DailyEnRouteTime4Bln(toll);
			ld = new LegDistance(network, toll, population);
			// in future, add some PersonAlgorithm and EventsHandler

			new MatsimPopulationReader(sc).readFile(plansFilename);

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
		if (scenario.equals("Zurich")) {
			clas = new CalcLinksAvgSpeed(network, 682845.0, 247388.0, 2000.0);
		} else if (withToll) {
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

			orms.write(outputBase + "onRoute.txt");
			orms.writeCharts(outputBase);
		}
		if (lttms != null) {
			lttms.write(outputBase + "legtraveltimes.txt.gz");
			lttms.writeCharts(outputBase + "legtraveltimes");
		}
		clas.write(outputBase + "avgSpeed.txt.gz");
		clas.writeChart(outputBase + "avgSpeedCityArea.png");
		ld.write(outputBase + "legDistances.txt.gz");
		ld.writeCharts(outputBase + "legDistances");

		SimpleWriter sw = new SimpleWriter(outputBase + "output.txt");
		sw.write("netfile:\t" + netFilename + "\neventsFile:\t"
				+ eventsFilename + "\noutputpath:\t" + outputBase + "\n");
		if (catl != null)
			sw.write("avg. Trip length:\t" + catl.getAverageTripLength()
					+ " [m]\n");
		sw.write("traffic performance (car):\t" + ctpf.getTrafficPerformance()
				+ " [Pkm]\n");
		sw.write("avg. speed of the total network (car):\t"
				+ cas.getNetAvgSpeed() + " [km/h]\n");
		sw.close();

		if (dd != null)
			dd.write(outputBase);
		if (dert != null)
			dert.write(outputBase);
		if (ms != null)
			ms.write(outputBase);
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

			QSim qsim = new QSim(sc, new EventsManagerImpl());

			new OTFEvent2MVI(qsim.getNetsimNetwork(), eventsOutputFilename,
					eventsFilename.split("events.txt.gz")[0]
							+ (scenario.equals("normal") ? "" : scenario + ".")
							+ "otfvis.mvi", Integer
							.parseInt(args[args.length - 2])).convert(sc.getConfig());
		}
		System.out.println("done.");
	}

	public static void run(final String[] args) {
		runIntern(args, "normal");
	}

	public static void runScenario(final String[] args, String scenario) {
		runIntern(args, scenario);
	}

	public static void runTollScenario(String[] args, String scenario) {
		withToll = true;
		runIntern(args, scenario);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length < 3) {
			printUsage();
			System.exit(0);
		} else if (args[3].equals(BERLIN) || args[4].equals(BERLIN))
			runScenario(args, BERLIN);
		else if (args[3].equals(BERLIN_HUNDEKOPF)
				|| args[4].equals(BERLIN_HUNDEKOPF))
			runTollScenario(args, BERLIN_HUNDEKOPF);
		else if (args[3].equals(TOTAL_BERLIN) || args[4].equals(TOTAL_BERLIN))
			runTollScenario(args, TOTAL_BERLIN);
		else if (args[3].equals(BERLIN_VERFLECHTUNGSRAUM)
				|| args[4].equals(BERLIN_VERFLECHTUNGSRAUM))
			runTollScenario(args, BERLIN_VERFLECHTUNGSRAUM);
		else if (args[3].equals(BERLIN_BRANDENBURG)
				|| args[4].equals(BERLIN_BRANDENBURG))
			runTollScenario(args, BERLIN_BRANDENBURG);
		else
			run(args);

	}
}
