/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalyseCottbusBasecase
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.NetworkFilter;
import playground.dgrether.events.DgNetShrinkImproved;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.TimeEventFilter;
import playground.dgrether.koehlerstrehlersignal.run.Cottbus2KS2010;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.signalsystems.utils.DgScenarioUtils;

import com.vividsolutions.jts.geom.Envelope;


public class DgAnalyseCottbusBasecase {


	private static final Logger log = Logger.getLogger(DgAnalyseCottbusBasecase.class);

	private static class TimeConfig {
		String name;
		double startTime; 
		double endTime; 
	}

	private static class Extent {
		String name;
		Envelope envelope;
	}

	private static class RunInfo {
		String runId;
		String remark;
	}
	
	//Average traveltime
	// Average speed
	// Macroscopic fundamental diagram
	// Leg histogram, see DgCottbusLegHistogram or LHI
	// Traffic difference qgis

	private static void analyse(List<RunInfo> runInfos, int iteration, List<TimeConfig> times, List<Extent> extents) {
		StringBuilder header = new StringBuilder("runId");
		header.append("\t");
		header.append("run?");
		header.append("\t");
		header.append("extent");
		header.append("\t");
		header.append("time interval");
		header.append("\t");
		header.append("travel time [s]");
		header.append("\t");
		header.append("travel time [hh:mm:ss]");
		header.append("\t");
		header.append("average travel time");
		header.append("\t");
		header.append("number of drivers");
		header.append("\t");
		List<String> lines = new ArrayList<String>();
		lines.add(header.toString());
		String outputDirectory = null;
		for (RunInfo runInfo: runInfos) {
			String runId = runInfo.runId;
			String runDirectory = DgPaths.REPOS + "runs-svn/run"+runId+"/";
			outputDirectory = runDirectory;
			
			OutputDirectoryHierarchy outputDir = new OutputDirectoryHierarchy(runDirectory, runId, false, false);
			String networkFilename = outputDir.getOutputFilename("output_network.xml.gz");
			String eventsFilename = outputDir.getIterationFilename(iteration, "events.xml.gz");
			String populationFilename = outputDir.getOutputFilename("output_plans.xml.gz");
			String lanesFilename = outputDir.getOutputFilename("output_lanes.xml.gz");
			String signalsystemsFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS);
			String signalgroupsFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS);
			String signalcontrolFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL);
			String ambertimesFilename = outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_AMBER_TIMES);
			
			Scenario scenario = DgScenarioUtils.loadScenario(networkFilename, populationFilename, lanesFilename, 
					signalsystemsFilename, signalgroupsFilename, signalcontrolFilename);
			
			
			for (Extent extent : extents) {
				for (TimeConfig time : times) {
					NetworkFilter netFilter;
					if (extent.envelope != null) {
						Network net = new DgNetShrinkImproved().createSmallNetwork(scenario.getNetwork(), extent.envelope);
						netFilter = new NetworkFilter(net);
					}
					else {
						netFilter = new NetworkFilter(scenario.getNetwork());
					}
					
					EventsFilterManager eventsManager = new EventsFilterManagerImpl();
					TimeEventFilter tef = new TimeEventFilter();
					tef.setStartTime(time.startTime);
					tef.setEndTime(time.endTime);
					eventsManager.addFilter(tef);
					
					DgAverageTravelTimeSpeed avgTtSpeed = new DgAverageTravelTimeSpeed(netFilter);
					eventsManager.addHandler(avgTtSpeed);
					
					MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
					reader.readFile(eventsFilename);
					double averageTT = avgTtSpeed.getTravelTime() / avgTtSpeed.getNumberOfPersons();
					StringBuilder out = new StringBuilder(runId);
					out.append("\t");
					out.append(runInfo.remark);
					out.append("\t");
					out.append(extent.name);
					out.append("\t");
					out.append(time.name);
					out.append("\t");
					out.append(Double.toString(avgTtSpeed.getTravelTime()));
					out.append("\t");
					out.append(Time.writeTime(avgTtSpeed.getTravelTime()));
					out.append("\t");
					out.append(Double.toString(averageTT));
					out.append("\t");
					out.append(Double.toString(avgTtSpeed.getNumberOfPersons()));
					lines.add(out.toString());
					log.info(out.toString());
					log.info("Total travel time : " + avgTtSpeed.getTravelTime() + " Average tt: " + averageTT + " number of persons: " + avgTtSpeed.getNumberOfPersons());
				}
			}
		}
		String outputFilename = outputDirectory + "travel_times_extent.txt";
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFilename);
		try {
			bw.write(header.toString());
			log.info("Result");
			for (String l : lines) {
				System.out.println(l);
				bw.write(l);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Analysis completed.");
	}

	private static List<TimeConfig> createTimeConfig(){
		TimeConfig morning = new TimeConfig();
		morning.startTime = 5.5 * 3600.0;
		morning.endTime  = 9.5 * 3600.0;
		morning.name = "morning";
		TimeConfig evening = new TimeConfig();
		evening.startTime = 13.5 * 3600.0;
		evening.endTime = 18.5 * 3600.0;
		evening.name = "afternoon";
		List<TimeConfig> list = new ArrayList<TimeConfig>();
		list.add(morning);
		list.add(evening);
		return list;
	}

	private static List<RunInfo> createRunsIdList(){
		List<RunInfo> l = new ArrayList<RunInfo>();
		RunInfo ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1730";
		ri.remark = "from scratch, com > 50";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1731";
		ri.remark  = "from scratch, com > 10";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1732";
		ri.remark = "continue 1712, com > 50";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1733";
		ri.remark  = "continue 1712, com > 10";
		l.add(ri);
		return l;
	}

	private static Envelope transform(Envelope env, CoordinateReferenceSystem from, CoordinateReferenceSystem to) {
		RuntimeException ex; 
		try {
			MathTransform transformation = CRS.findMathTransform(from, to, true);
			Envelope transEnv = JTS.transform(env, transformation);
			return transEnv;
		} catch (TransformException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		}
		catch (FactoryException e) {
			e.printStackTrace();
			ex = new RuntimeException(e);
		}
		throw ex;
	}

	private static Envelope getTransformedEnvelope(Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple) {
		BoundingBox bounds = featureTuple.getSecond().getBounds();
		Envelope env = new Envelope(bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
		env = transform(env, featureTuple.getFirst(), Cottbus2KS2010.CRS);
		return env;
	}

	private static List<Extent> createExtentList(){
		List<Extent> l = new ArrayList<Extent>();
		String filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple = CottbusUtils.loadCottbusFeature(filterFeatureFilename);
		Envelope env = getTransformedEnvelope(featureTuple);
		Extent e = new Extent();
		e.name = "Cottbus Kreis BB";
		e.envelope = env;
		l.add(e);
		
		filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
		featureTuple = CottbusUtils.loadFeature(filterFeatureFilename);
		
		env = getTransformedEnvelope(featureTuple);
		e = new Extent();
		e.name = "Signals BB";
		e.envelope = env;
		l.add(e);
		
		e = new Extent();
		e.name = "all";
		l.add(e);
		
		return l;
	}

	public static void main(String[] args) {
		int iteration = 1000;
		List<RunInfo> runIds = createRunsIdList();
		List<TimeConfig> times = createTimeConfig();
		List<Extent> extents = createExtentList();
		analyse(runIds, iteration, times, extents);
	}
}
