/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Class to write all calculated results of the analyze tool that is given in the constructor.
 * 
 * @author tthunig
 *
 */
public final class TtAnalyzedGeneralResultsWriter {

	private static final Logger log = Logger.getLogger(TtAnalyzedGeneralResultsWriter.class);
	
	private Scenario scenario;
	private TtGeneralAnalysis handler;
	private String outputDirBase;
	private PrintStream overallItWritingStream;
	private int lastIteration;
	
	private enum RelCumFreqType {
		DepPerTime, ArrPerTime, TripsPerDuration, TripsPerDist, TripsPerAvgSpeed
	}
	
	private CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
	
	@Inject
	public TtAnalyzedGeneralResultsWriter(Scenario scenario, TtGeneralAnalysis handler) {
		this.handler = handler;
		this.outputDirBase = scenario.getConfig().controler().getOutputDirectory();
		this.lastIteration = scenario.getConfig().controler().getLastIteration();
		this.scenario = scenario;
		
		// prepare file for the results of all iterations
		prepareOverallItWriting();
	}

	private void prepareOverallItWriting() {
		// create output dir for overall iteration analysis
		String lastItDir = this.outputDirBase + "ITERS/it." + this.lastIteration + "/";
		new File(lastItDir).mkdir();
		String lastItOutputDir = lastItDir + "analysis/";
		new File(lastItOutputDir).mkdir();

		// create writing stream
		try {
			this.overallItWritingStream = new PrintStream(new File(lastItOutputDir + "overallIterationAnalysis.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// write header
		String header = "it\ttotal tt[s]\ttotal delay[s]\ttotal dist[m]\tavg trip speed[m/s]";
		this.overallItWritingStream.println(header);
	}

	public void writeIterationResults(int iteration) {
		log.info("Starting to write analysis of iteration " + iteration + "...");
		addLineToOverallItResults(iteration);
		writeItOnlyResults(iteration);
	}

	private void addLineToOverallItResults(int iteration) {
		// get results
		double totalTTIt = handler.getTotalTt();
		double totalDealyIt = handler.getTotalDelay();
		double totalDistIt = handler.getTotalDistance();
		double avgTripSpeedIt = handler.getAverageTripSpeed();
		
		// write results
		StringBuffer line = new StringBuffer();
		line.append(iteration + "\t" + totalTTIt + "\t" + totalDealyIt + "\t" 
				+ totalDistIt + "\t" + avgTripSpeedIt);
		this.overallItWritingStream.println(line.toString());
	}

	private void writeItOnlyResults(int iteration) {
		// create output dir for this iteration analysis
		String outputDir = this.outputDirBase + "ITERS/it." + iteration + "/analysis/";
		new File(outputDir).mkdir();

		// write iteration specific analysis
		log.info("Results of iteration " + iteration + ":");
		writeRelCumFreqData(outputDir, RelCumFreqType.DepPerTime, handler.getRelativeCumulativeFrequencyOfDeparturesPerTimeInterval());
		writeRelCumFreqData(outputDir, RelCumFreqType.ArrPerTime, handler.getRelativeCumulativeFrequencyOfArrivalsPerTimeInterval());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerDuration, handler.getRelativeCumulativeFrequencyOfTripsPerDuration());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerDist, handler.getRelativeCumulativeFrequencyOfTripsPerDistance());
		writeRelCumFreqData(outputDir, RelCumFreqType.TripsPerAvgSpeed, handler.getRelativeCumulativeFrequencyOfTripsPerSpeed());
	}

	private void writeRelCumFreqData(String outputDir, RelCumFreqType type, Map<Double, Double> relCumFreqMap) {
		PrintStream stream;
		String filename = outputDir + type + ".txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "";
		switch (type){
		case DepPerTime:
			header = "time of day\trelative cumulative frequency of departures";
			break;
		case ArrPerTime:
			header = "time of day\trelative cumulative frequency of arrivals";
			break;
		case TripsPerDuration:
			header = "trip duration\trelative cumulative frequency of trips";
			break;
		case TripsPerDist:
			header = "trip distance\trelative cumulative frequency of trips";
			break;
		case TripsPerAvgSpeed:
			header = "avg trip speed\trelative cumulative frequency of trips";
			break;
		}
		stream.println(header);
		
		for (Entry<Double, Double> entry : relCumFreqMap.entrySet()) {
			StringBuffer line = new StringBuffer();
			line.append(entry.getKey() + "\t" + entry.getValue());
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}

	public void closeAllStreams() {
		this.overallItWritingStream.close();
	}

	public void writeSpatialAnaylsis(int iteration) {		
		PolygonFeatureFactory factory = createFeatureType(this.crs);
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Map<Id<Link>, Double> totalDelayPerLink = handler.getTotalDelayPerLink();
		Map<Id<Link>, Double> avgDelayPerLink = handler.getAvgDelayPerLink();
		Map<Id<Link>, Integer> numberOfVehPerLink = handler.getNumberOfVehPerLink();
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Id<Link> linkId = link.getId();
			
			double linkTotalDelay = 0.0;
			if (totalDelayPerLink.containsKey(linkId))
				linkTotalDelay = totalDelayPerLink.get(linkId);
			
			double linkAvgDelay = 0.0;
			if (avgDelayPerLink.containsKey(linkId))
				linkAvgDelay = avgDelayPerLink.get(linkId);
			
			int linkNumberOfVeh = 0;
			if (numberOfVehPerLink.containsKey(linkId))
				linkNumberOfVeh = numberOfVehPerLink.get(linkId);
			
			features.add(createFeature(link, geofac, factory, linkTotalDelay, linkAvgDelay, linkNumberOfVeh));
		}
		
		ShapeFileWriter.writeGeometries(features, this.outputDirBase + "ITERS/it." + iteration + "/analysis/spatialAnalysis.shp");
	}
	
	private PolygonFeatureFactory createFeatureType(CoordinateReferenceSystem crs) {
		PolygonFeatureFactory.Builder builder = new PolygonFeatureFactory.Builder();
		builder.setCrs(crs);
		builder.setName("link");
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("type", String.class);
	
		builder.addAttribute("totalDelay", Double.class);
		builder.addAttribute("avgDelayPerVeh", Double.class);
		builder.addAttribute("#veh", Integer.class);
		// TODO avg speed per link
		
//		// absolute flow values of run1
//		for (int i = firstHour; i < lastHour; i++){
//			builder.addAttribute("h" + (i + 1)+"_abs1", Double.class);
//		}
		
		return builder.create();
	}
	
	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolygonFeatureFactory factory, double linkTotalDelay, double linkAvgDelay, int linkNumberOfVeh) {
		Coordinate[] coords = PolygonFeatureGenerator.createPolygonCoordsForLink(link, 20.0);
		List<Object> attribs = new ArrayList<>();
		attribs.add(link.getId().toString());
		attribs.add(link.getFromNode().getId().toString());
		attribs.add(link.getToNode().getId().toString());
		attribs.add(link.getLength());
		attribs.add(link.getFreespeed());
		attribs.add(link.getCapacity());
		attribs.add(link.getNumberOfLanes());
		attribs.add(((LinkImpl) link).getType());
		attribs.add(linkTotalDelay);
		attribs.add(linkAvgDelay);
		attribs.add(linkNumberOfVeh);
	
		return factory.createPolygon(coords, attribs.toArray(), link.getId().toString());
	}

	
}
