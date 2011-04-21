/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonWriter.java
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

package playground.anhorni.counts.fromEvents;

import java.io.File;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsGraphWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

public class CountsCreatorFromEvents {
	private Counts counts;
	private EventsManager events;
	private Network network;
	private String outpath;
	private final static Logger log = Logger.getLogger(CountsCreatorFromEvents.class);
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		CountsCreatorFromEvents creator = new CountsCreatorFromEvents();
		creator.run(args[0], args[1], args[2], args[3]);
				
		Gbl.printElapsedTime();
		log.info("Counts post creation finished #########################################################################");
	}
	
	public void run(String countsFile, String eventsFile, String networkFile, String outpath) {
		this.counts = new Counts();
		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(countsFile);
		
		log.info("reading the network ...");
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		this.network = scenario.getNetwork();
		
		this.events = EventsUtils.createEventsManager();
		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
		this.events.addHandler(analyzer);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		this.outpath = outpath;
		new File(this.outpath).mkdirs();
		this.createCounts(analyzer);
	}
	
	private void createCounts(VolumesAnalyzer analyzer) {		
		CountsComparisonAlgorithm comparator = new CountsComparisonAlgorithm(analyzer, this.counts, this.network, 1.0);
		comparator.run();
		comparator.getComparison();
		CountsGraphWriter cgw = new CountsGraphWriter(this.outpath, comparator.getComparison(), 0, true, true);
		cgw.setGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
		cgw.setGraphsCreator(new CountsErrorGraphCreator("errors"));
		cgw.setGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
		cgw.setGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
		cgw.createGraphs();
		
		CountSimComparisonTableWriter ctw=new CountSimComparisonTableWriter(comparator.getComparison(),Locale.ENGLISH);
		ctw.writeFile(this.outpath + "/counts.txt");
	}
}
