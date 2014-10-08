/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 *
 */
public class LinkTrafficVolumeExtractor {
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		int timeBin = 60;
		final String netFilename = "../psrc/network/psrc-wo-3212.xml.gz";
		final String plansFilename = "../runs/run668/it.1500/1500.plans.xml.gz";
		final String eventsFilename = "../runs/run668/it.1500/1500.analysis/6760.events.txt";
		final String outFilename = "../runs/run668/it.1500/1500.analysis/6760.volume.";

		Gbl.startMeasurement();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

//		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		EventsManager events = EventsUtils.createEventsManager();

		VolumesAnalyzer va = new VolumesAnalyzer(timeBin, 30 * 3600, network);
		events.addHandler(va);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		BufferedWriter writer;
		double[] xs = new double[(24 * 3600 + 1) / timeBin];
		double[] ys = new double[(24 * 3600 + 1) / timeBin];
		int index;
		try {
			writer = IOUtils.getBufferedWriter(outFilename + timeBin + ".txt");
			writer.write("TimeBin\tLinkVolume[veh/" + timeBin
					+ " s]\tLinkVolume[veh/h]\n");
			for (int anI = 0; anI < 24 * 3600; anI = anI + timeBin) {
				index = (anI) / timeBin;
				ys[index] = va.getVolumesForLink(Id.create("6760", Link.class))[index];
				writer.write(anI + "\t" + ys[index] + "\t" + ys[index] * 3600.0
						/ timeBin + "\n");
				ys[index] = ys[index] * 3600.0 / timeBin;
				xs[index] = (anI) / 3600.0;
				writer.flush();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		XYLineChart travelTimeChart = new XYLineChart(
				"Volumes of Link 6760 of psrc network", "time [h]",
				"Volume [veh/h]");
		travelTimeChart.addSeries("with timeBin " + timeBin / 60 + " min.", xs,
				ys);
		travelTimeChart.saveAsPng(outFilename + timeBin + ".png", 1024, 768);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
