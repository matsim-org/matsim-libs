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

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;
import org.xml.sax.SAXException;

/**
 * @author ychen
 * 
 */
public class AnalysisTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "./test/yu/analysis/input/ch.xml";
		final String plansFilename = "./test/yu/analysis/input/100ITERs_pt-6t-12output_plans.xml";
		final String rpFilename = "./test/yu/analysis/input/rpZH1_05.xml";
		final String eventsFilename = "./test/yu/analysis/input/100.events_pt-6t-12.txt";
		final String outputFilename = "./test/yu/analysis/output/outputPt-6t-12.txt";
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/analysis/analysisConfig.xml" });

		World world = Gbl.getWorld();
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		CalcAverageTripLength catl = new CalcAverageTripLength();
		population.addAlgorithm(catl);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		world.setPopulation(population);

		Events events = new Events();

		RoadPricingScheme toll = new RoadPricingScheme(network);
		CalcAverageTolledTripLength cattl = null;
		if (Gbl.useRoadPricing()) {
			RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(
					network);
			try {
				rpReader.parse(rpFilename);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			toll = rpReader.getScheme();
			CalcPaidToll cpt = new CalcPaidToll(network, toll);
			cattl = new CalcAverageTolledTripLength(network, toll);
			events.addHandler(cpt);
			events.addHandler(cattl);
		}

		CalcTrafficPerformance ctpf = new CalcTrafficPerformance(network);
		CalcAvgSpeed cas = new CalcAvgSpeed(network);

		events.addHandler(ctpf);
		events.addHandler(cas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			out.write("netsfile:\t" + netFilename + "\neventsfile:\t"
					+ eventsFilename + "\nplansfile:\t" + plansFilename
					+ "\nroadpricingfile:\t" + rpFilename + "\n");
			out.write("avg. trip length:\t" + catl.getAverageTripLength()
					+ "\t[m]\n");
			if (cattl != null)
				out.write("avg. tolled trip length:\t"
						+ cattl.getAverageTripLength() + "\t[m]\n");
			out.write("traffic performance:\t" + ctpf.getTrafficPerformance()
					+ "\t[Pkm]\n");
			out
					.write("avg. travel Speed:\t" + cas.getAvgSpeed()
							+ "\t[km/h]\n");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
