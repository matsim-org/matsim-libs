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
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
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
	private String networkFile;
	private String outpath;
	private final static Logger log = Logger.getLogger(CountsCreatorFromEvents.class);
	
	public static final String COUNTS = "counts";
	public static final String DISTANCEFILTER = "distanceFilter";
	public static final String DISTANCEFITLERCENTERNODE = "distanceFilterCenterNode";
	
	private Double distanceFilter = null;
	private String distanceFilterCenterNode = null;
	private double countsScaleFactor = 1.0;
	private String inputCountsFile = null;
	private String outputFormat = null;
	private String coordinateSystem = null;
	
	/*
	 * arg0: config file
	 * arg1: events file
	 * arg2: outpath
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		CountsCreatorFromEvents creator = new CountsCreatorFromEvents();
		creator.run(args[0], args[1], args[2]);
				
		Gbl.printElapsedTime();
		log.info("Counts post creation finished #########################################################################");
	}
	
	public void run(String configFile, String eventsFile, String outpath) {
		this.counts = new Counts();
		
		log.info("reading config: " + configFile);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
		this.readConfig(scenario);
				
		log.info("reading counts: " + this.inputCountsFile);	
		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(this.inputCountsFile);
		
		log.info("reading the network: " + this.networkFile);
		new MatsimNetworkReader(scenario).readFile(this.networkFile);
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
	
	private void readConfig(final Scenario scenario) {
		Config config = scenario.getConfig();
		
		this.coordinateSystem = config.global().getCoordinateSystem();
		log.info("Coordinate system: " + this.coordinateSystem);
		
		this.networkFile = config.network().getInputFile();
		log.info("Network: " + this.networkFile);
		
		this.inputCountsFile = config.counts().getCountsFileName();
		log.info("Counts file: " + this.inputCountsFile);
		
		this.outputFormat = config.counts().getOutputFormat();
		log.info("Output formats: " + this.outputFormat);
		
		this.distanceFilterCenterNode = config.counts().getDistanceFilterCenterNode();
		log.info("Distance filter center node: " + this.distanceFilterCenterNode);
		
		this.distanceFilter = config.counts().getDistanceFilter();
		log.info("Distance filter: " + this.distanceFilter);
		
		this.countsScaleFactor = config.counts().getCountsScaleFactor();
		log.info("Counts scale factor: " + this.countsScaleFactor);
	}
	
	private void createCounts(VolumesAnalyzer analyzer) {		
		CountsComparisonAlgorithm comparator = new CountsComparisonAlgorithm(analyzer, this.counts, this.network, this.countsScaleFactor);
		if ((this.distanceFilter != null) && (this.distanceFilterCenterNode != null)) {
			comparator.setDistanceFilter(this.distanceFilter, this.distanceFilterCenterNode);
		}
		comparator.run();
						
		if (this.outputFormat.contains("html") || this.outputFormat.contains("all")) {
				boolean htmlset = true;
				boolean pdfset = true;
				CountsGraphWriter cgw = new CountsGraphWriter(this.outpath, comparator.getComparison(), 0, htmlset, pdfset);
				cgw.setGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
				cgw.setGraphsCreator(new CountsErrorGraphCreator("errors"));
				cgw.setGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
				cgw.setGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
				cgw.createGraphs();
		}
		if (this.outputFormat.contains("kml")|| this.outputFormat.contains("all")) {
			String filename = this.outpath + "countscompare.kmz";
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					comparator.getComparison(), this.network, 
					TransformationFactory.getCoordinateTransformation(this.coordinateSystem, TransformationFactory.WGS84 ));
			kmlWriter.setIterationNumber(0);
			kmlWriter.writeFile(filename);
		}
		if (this.outputFormat.contains("txt")||	this.outputFormat.contains("all")) {
			String filename = this.outpath +  "countscompare.txt";
			CountSimComparisonTableWriter ctw=new CountSimComparisonTableWriter(comparator.getComparison(),Locale.ENGLISH);
			ctw.writeFile(filename);
		}
	}
}
