/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFlowAndTravelTimeExtractor.java
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

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.yu.utils.io.SimpleWriter;

public class LinkFlowAndTravelTimeExtractor implements AfterMobsimListener,
		BeforeMobsimListener {
	private int timeBin, maxTime;
	private Network network;
	private VolumesAnalyzer volumesAnalyzer;
	private TravelTimeCalculator travelTimeCalculator;

	public LinkFlowAndTravelTimeExtractor(int timeBin, int maxTime,
			Network network,
			TravelTimeCalculatorConfigGroup travelTimeCalculatorConfigGroup) {
		this.timeBin = timeBin;
		this.maxTime = maxTime;
		this.network = network;
		volumesAnalyzer = new VolumesAnalyzer(this.timeBin, this.maxTime,
				network);
		travelTimeCalculator = new TravelTimeCalculator(network, this.timeBin,
				this.maxTime, travelTimeCalculatorConfigGroup);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();

		EventsManager eventsManager = controler.getEvents();

		write(controler.getControlerIO().getOutputFilename(
				controler.getConfig().controler().getRunId() + ".it"
						+ event.getIteration() + ".flowTravelTime.txt.gz"));

		eventsManager.removeHandler(volumesAnalyzer);
		eventsManager.removeHandler(travelTimeCalculator);

	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iteration = event.getIteration();
		volumesAnalyzer.reset(iteration);
		travelTimeCalculator.reset(iteration);

		EventsManager eventsManager = event.getControler().getEvents();
		eventsManager.addHandler(volumesAnalyzer);
		eventsManager.addHandler(travelTimeCalculator);
	}

	public void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		SimpleWriter.setIntermission("\t");
		// -------------WRITE FILEHEAD-----------------
		StringBuffer sb = new StringBuffer("LinkID");

		for (int i = 0; i < 24; i++) {
			sb.append("\tflow");
			sb.append(i);
			sb.append("-");
			sb.append(i + 1);
		}

		for (int i = 0; i < 24; i++) {
			sb.append("\ttt");
			sb.append(i);
			sb.append("-");
			sb.append(i + 1);
		}

		writer.writeln(sb);
		// -----------------WRITE CONTENT---------------
		for (Link link : network.getLinks().values()) {
			StringBuffer line = new StringBuffer(link.getId().toString());

			int[] vols = volumesAnalyzer.getVolumesForLink(link.getId());
			for (int i = 0; i < 24; i++) {
				SimpleWriter.appendIntermission(line);
				line.append(vols != null ? vols[i] : 0);
			}

			for (int i = 0; i < 24; i++) {
				SimpleWriter.appendIntermission(line);
				line.append(travelTimeCalculator.getLinkTravelTime(link,
						i * 3600d));
			}

			writer.writeln(line);
			writer.flush();
		}
		writer.close();
	}

	public static void main(String[] args) {
		Controler controler = new Controler(args[0]);
		controler.addControlerListener(new LinkFlowAndTravelTimeExtractor(3600,
				30 * 3600, controler.getNetwork(), controler.getConfig()
						.travelTimeCalculator()));
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
