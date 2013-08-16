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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.CountSimComparison;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.NetworkFilter;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.analysis.simsimanalyser.CountsShapefileWriter;
import playground.dgrether.analysis.simsimanalyser.SimSimCountsAnalysis;
import playground.dgrether.events.DgNetShrinkImproved;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.TimeEventFilter;
import playground.dgrether.koehlerstrehlersignal.run.Cottbus2KS2010;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;

import com.vividsolutions.jts.geom.Envelope;


public class DgAnalyseCottbusKS2010 {

	private static final Logger log = Logger.getLogger(DgAnalyseCottbusKS2010.class);

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
		public boolean baseCase = false;
		public Integer iteration;
	}
	
	private static class Result {
		public RunInfo runInfo;
		public RunResultsLoader runLoader;
		public Extent extent;
		public TimeConfig timeConfig;
		public double travelTime;
		public double travelTimeDelta;
		public double averageTravelTime;
		public double numberOfPersons;
		public double travelTimePercent;
		public double personsDelta;
		public VolumesAnalyzer volumes;
		public Network network;
		
	}
	
	private static class Results {
		private Map<RunInfo, Map<Extent, Map<TimeConfig,Result>>> resultMap = new HashMap();  
		
		private void addResult(Result result) {
			Map<Extent, Map<TimeConfig, Result>> m = resultMap.get(result.runInfo);
			if (m == null) {
				m = new HashMap();
				resultMap.put(result.runInfo, m);
			}
			Map<TimeConfig, Result> m2 = m.get(result.extent);
			if (m2 == null) {
				m2 = new HashMap();
				m.put(result.extent, m2);
			}
			m2.put(result.timeConfig, result);
		}
		
		public List<Result> getResults() {
			List<Result> ret = new ArrayList<Result>();
			for (Map<Extent, Map<TimeConfig, Result>> m : resultMap.values()) {
				for (Map<TimeConfig, Result> m2 : m.values()) {
					ret.addAll(m2.values());
				}
			}
			return ret;
		}
		
	}
	
	private Results results = new Results(); 

	
	private void analyseResults() {
		Map<Extent, Map<TimeConfig, Result>> baseCase = null;
		for (RunInfo r : results.resultMap.keySet()) {
			if (r.baseCase) {
				baseCase = results.resultMap.get(r);
			}
		}
		for (Result r : results.getResults()) {
			double averageTT = r.travelTime / r.numberOfPersons;
			r.averageTravelTime = averageTT;
//			if (! r.runInfo.baseCase) {
				Result baseResult = baseCase.get(r.extent).get(r.timeConfig);
				r.travelTimeDelta = r.travelTime - baseResult.travelTime;
				r.travelTimePercent = r.travelTime / baseResult.travelTime * 100.0;
				r.personsDelta = r.numberOfPersons - baseResult.numberOfPersons;
//			}
				this.createSimSimComparison(baseResult, r);
		}
	}
	
	private void createSimSimComparison(Result baseResult, Result result) {
		SimSimCountsAnalysis countsAnalysis = new SimSimCountsAnalysis();
		Map<Id, List<CountSimComparison>> countSimCompMap = countsAnalysis.createCountSimComparisonByLinkId(result.network, baseResult.volumes, result.volumes);
		String shapeBase = baseResult.runInfo.runId + "_it_" + baseResult.runInfo.iteration + "_vs_";
		shapeBase += result.runInfo.runId + "_it_" + result.runInfo.iteration + "_simsimcomparison.shp";
		String shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapeBase);
		new CountsShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile, countSimCompMap);
	}
	
	private void writeFile(String file) {
		List<String> lines = new ArrayList<String>();
		StringBuilder header = new StringBuilder();
		header.append("runId");
		header.append("\t");
		header.append("Iteration");
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
		header.append("delta travel time [s]");
		header.append("\t");
		header.append("delta travel time [hh:mm:ss]");
		header.append("\t");
		header.append("delta travel time [%]");
		header.append("\t");
		header.append("number of drivers");
		header.append("\t");
		header.append("delta drivers");
		header.append("\t");
		header.append("average travel time");
		header.append("\t");
		lines.add(header.toString());
		for (Result r : results.getResults()) {
			StringBuilder out = new StringBuilder();
			out.append(r.runInfo.runId);
			out.append("\t");		
			out.append(Integer.toString(r.runInfo.iteration));
			out.append("\t");
			out.append(r.runInfo.remark);
			out.append("\t");
			out.append(r.extent.name);
			out.append("\t");
			out.append(r.timeConfig.name);
			out.append("\t");
			out.append(Double.toString(r.travelTime));
			out.append("\t");
			out.append(Time.writeTime(r.travelTime));
			out.append("\t");
			out.append(Double.toString(r.travelTimeDelta));
			out.append("\t");
			out.append(Time.writeTime(r.travelTimeDelta));
			out.append("\t");
			out.append(Double.toString(r.travelTimePercent));
			out.append("\t");
			out.append(Double.toString(r.numberOfPersons));
			out.append("\t");
			out.append(Double.toString(r.personsDelta));
			out.append("\t");
			out.append(Double.toString(r.averageTravelTime));
			out.append("\t");
				lines.add(out.toString());
			log.info(out.toString());
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try {
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
	}
	
	//Average traveltime
	// Average speed
	// Macroscopic fundamental diagram
	// Leg histogram, see DgCottbusLegHistogram or LHI
	// Traffic difference qgis

	private void calculateResults(List<RunInfo> runInfos, List<TimeConfig> times, List<Extent> extents) {
		for (RunInfo runInfo: runInfos) {
			String runId = runInfo.runId;
			String runDirectory = DgPaths.REPOS + "runs-svn/run"+runId+"/";
			RunResultsLoader runDir = new RunResultsLoader(runDirectory, runId);
			
			for (Extent extent : extents) {
				for (TimeConfig time : times) {
					NetworkFilter netFilter;
					Network net; 
					if (extent.envelope != null) {
						net = new DgNetShrinkImproved().createSmallNetwork(runDir.getNetwork(), extent.envelope);
					}
					else {
						net = runDir.getNetwork();
					}
					netFilter = new NetworkFilter(net);
					
					Result result = new Result();
					result.runInfo = runInfo;
					result.runLoader = runDir;
					result.extent = extent;
					result.timeConfig = time;
					result.network = net;
					results.addResult(result);
					
					EventsFilterManager eventsManager = new EventsFilterManagerImpl();
					TimeEventFilter tef = new TimeEventFilter();
					tef.setStartTime(time.startTime);
					tef.setEndTime(time.endTime);
					eventsManager.addFilter(tef);
					
					DgAverageTravelTimeSpeed avgTtSpeed = new DgAverageTravelTimeSpeed(netFilter);
					eventsManager.addHandler(avgTtSpeed);
					
					VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600, net);
					eventsManager.addHandler(volumes);
					
					MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
					reader.readFile(runDir.getEventsFilename(runInfo.iteration));

					result.volumes = volumes;
					result.travelTime = avgTtSpeed.getTravelTime();
					result.numberOfPersons = avgTtSpeed.getNumberOfPersons();
					
					log.info("Total travel time : " + avgTtSpeed.getTravelTime() + " number of persons: " + avgTtSpeed.getNumberOfPersons());
				}
			}
		}
		log.info("Calculated results.");
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
		ri.baseCase  = true;
		ri.iteration = 1000;
		l.add(ri);
//		ri = new RunInfo();
//		ri.runId = "1730";
//		ri.remark = "from scratch, com > 50";
//		l.add(ri);
//		ri = new RunInfo();
//		ri.runId = "1731";
//		ri.remark  = "from scratch, com > 10";
//		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1732";
		ri.iteration = 0;
		ri.remark = "continue 1712, com > 50";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1733";
		ri.iteration = 0;
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
		List<RunInfo> runIds = createRunsIdList();
		List<TimeConfig> times = createTimeConfig();
		List<Extent> extents = createExtentList();
		DgAnalyseCottbusKS2010 ana = new DgAnalyseCottbusKS2010();
		ana.calculateResults(runIds, times, extents);
		ana.analyseResults();
		String outputDirectory = DgPaths.SHAREDSVN + "projects/cottbus/cb2ks2010/results/";
		String outputFilename = outputDirectory + "2013-08-16_travel_times_extent_it_0.txt";
		ana.writeFile(outputFilename);

	}


}
