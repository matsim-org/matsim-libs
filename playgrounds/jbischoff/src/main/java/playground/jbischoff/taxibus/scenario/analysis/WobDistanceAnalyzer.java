/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.scenario.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.math.DoubleMath;

/**
 * @author jbischoff
 *
 */

public class WobDistanceAnalyzer {

	private final String eventsFile;
	private final String outputfile;
	private final String linkdelayfile;
	private final String networkFile;

	private Scenario scenario;
	private WobTravelTimeAnalyzer kjaTravelTimeAnalyzer;

	private WobEventHandler kjaEventHandler;

	public static void main(String[] args) {
		String dir = "D:/runs-svn/vw_rufbus/vw040/";
		String events = dir + "vw040.output_events.xml.gz";
		String network = dir+"vw040.output_network.xml.gz";

		WobDistanceAnalyzer kjaDistanceAnalyzer = new WobDistanceAnalyzer(
				events, network,  dir + "outfile.txt", dir
						+ "delays.txt");
		kjaDistanceAnalyzer.run();
	}

	public WobDistanceAnalyzer(String eventsFile, String networkFile,
			 String outputfile, String linkdelays) {

		this.eventsFile = eventsFile;
		this.outputfile = outputfile;
		this.networkFile = networkFile;
		this.linkdelayfile = linkdelays;
	}

	public void run() {
		Config config = ConfigUtils.createConfig();

		this.scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario).readFile(networkFile);
		analyzeEvents();
		generateAndWriteOutput();
		writeLinkDelays();
	}

	private void writeLinkDelays() {
		try {

			BufferedWriter writer = IOUtils.getBufferedWriter(linkdelayfile);
			writer.append("link\tabsoluteDelay\tagents\tdelayPerUser");
			for (Entry<Id<Link>, Double> e : this.kjaEventHandler
					.getDelayPerLink().entrySet()) {
				double usersPerLink = this.kjaEventHandler.getAgentsPerLink()
						.get(e.getKey());
				double delayPerUser = e.getValue() / usersPerLink;
				if (Double.isNaN(delayPerUser))
					delayPerUser = 0.0;
				writer.newLine();
				writer.append(e.getKey() + "\t" + e.getValue() + "\t"
						+ usersPerLink + "\t" + delayPerUser);
			}
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String prettyPrintSeconds(double seconds) {
		long s = (long) seconds;
		return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60,
				(s % 60));
	}

	private void generateAndWriteOutput() {

		DecimalFormat df = new DecimalFormat("#############.00");
		DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(dfs);

		DecimalFormat whole = new DecimalFormat("#########");
		whole.setDecimalFormatSymbols(dfs);
		try {

			BufferedWriter writer = IOUtils.getBufferedWriter(outputfile);

			writer.append("Report Input events file:\t\t" + this.eventsFile);
			writer.newLine();

			writer.append("Report Input network file:\t\t" + this.networkFile);
			writer.newLine();

			writer.append("Travel distances");
			writer.newLine();
			double averageDistance = kjaEventHandler.getDrivenDistances()/kjaEventHandler.getLegs();
			writer.append("Overall distance driven by car (km)\t\t"
					+ df.format(averageDistance
							* kjaEventHandler.getDrivenDistances())
					+ "\n");
			writer.append("Average car leg distance (km)\t\t"
					+ df.format(averageDistance / 1000) + "\n");
			writer.append("Car legs per activity\n");

			for (Entry<String, Double> entry : this.kjaEventHandler
					.getTravelDistanceToActivity().entrySet()) {
				writer.append("Activity:" + entry.getKey());
				double legsPerActivity = this.kjaEventHandler
						.getTravelLegsPerActivity().get(entry.getKey());
				double averageDistToActivity = entry.getValue()
						/ legsPerActivity;
				writer.append("\ttotal legs\t" + whole.format(legsPerActivity));
				writer.newLine();
				writer.append("\tOverall distance (km)\t"
						+ df.format(entry.getValue() / 1000));
				writer.newLine();
				writer.append("\taverage distance (km)\t"
						+ df.format(averageDistToActivity / 1000));
				writer.newLine();

			}
			writer.append("Beeline distance derivation");
			writer.newLine();
			writer.append("Average car trip beeline derivation \t\t"
					+ DoubleMath.mean(kjaEventHandler
							.getDrivenBeeLineDerivationFactor().values())
					+ "\n");

			writer.append("Travel times");
			writer.newLine();

			writer.append("Car legs per activity\n");

			for (Entry<String, Double> entry : kjaEventHandler
					.getTravelTimeToActivity().entrySet()) {
				writer.append("Activity:" + entry.getKey());
				writer.newLine();
				double averageTtToActivity = entry.getValue()
						/ kjaEventHandler.getTravelLegsPerActivity().get(
								entry.getKey());
				writer.append("\ttotal legs\t"
						+ whole.format(kjaEventHandler
								.getTravelLegsPerActivity().get(entry.getKey())));
				writer.newLine();
				writer.append("\tOverall travelTime (hh:mm:ss)\t"
						+ prettyPrintSeconds(entry.getValue()));
				writer.newLine();

				writer.append("\taverage travelTime (hh:mm:ss)\t"
						+ prettyPrintSeconds(averageTtToActivity));
				writer.newLine();

			}
			writer.append("Congestion delays:\n");
			double averageDelayPerLink = DoubleMath.mean(this.kjaEventHandler
					.getDelayPerLink().values());
			writer.append("Overall delay\t\t"
					+ prettyPrintSeconds(averageDelayPerLink
							* this.kjaEventHandler.getDelayPerLink().size()));
			writer.newLine();
			writer.append("Average delay per Link \t\t"
					+ prettyPrintSeconds(averageDelayPerLink));
			writer.newLine();
			double averageDelayPerAgent = DoubleMath.mean(this.kjaEventHandler
					.getDelayPerAgent().values());
			writer.append("Average delay per User \t\t"
					+ prettyPrintSeconds(averageDelayPerAgent));
			writer.newLine();
			writer.append(kjaTravelTimeAnalyzer.getResults());

			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void analyzeEvents() {
		EventsManager events = EventsUtils.createEventsManager();

		kjaTravelTimeAnalyzer = new WobTravelTimeAnalyzer();
		kjaTravelTimeAnalyzer.init((MutableScenario) scenario);
		kjaTravelTimeAnalyzer.preProcessData();

		List<EventHandler> handler = kjaTravelTimeAnalyzer.getEventHandler();
		for (EventHandler eh : handler) {
			events.addHandler(eh);
		}
		kjaEventHandler = new WobEventHandler(scenario.getNetwork(),
				kjaTravelTimeAnalyzer.getPtDriverIdAnalyzer());
		events.addHandler(kjaEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		kjaTravelTimeAnalyzer.postProcessData();

	}

	static void addStringDoubleToMap(Map<String, Double> map, String key,
			Double value) {
		double beforeValue = 0.;
		if (map.containsKey(key)) {
			beforeValue = map.get(key);
		}
		double newValue = beforeValue + value;
		map.put(key, newValue);

	}

	static void addIdDoubleToMap(Map<Id<Link>, Double> map, Id<Link> key,
			Double value) {
		double beforeValue = 0.;
		if (map.containsKey(key)) {
			beforeValue = map.get(key);
		}
		double newValue = beforeValue + value;
		map.put(key, newValue);

	}

	static void addPersonIdDoubleToMap(Map<Id<Person>, Double> map,
			Id<Person> key, Double value) {
		double beforeValue = 0.;
		if (map.containsKey(key)) {
			beforeValue = map.get(key);
		}
		double newValue = beforeValue + value;
		map.put(key, newValue);

	}
}
