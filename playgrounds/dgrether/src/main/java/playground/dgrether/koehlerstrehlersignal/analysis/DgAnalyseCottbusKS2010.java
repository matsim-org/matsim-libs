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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.CountSimComparison;
import org.matsim.signalsystems.data.SignalsData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.analysis.categoryhistogram.CategoryHistogramWriter;
import playground.dgrether.analysis.flightlhi.LegModeHistogramImproved;
import playground.dgrether.analysis.simsimanalyser.CountsShapefileWriter;
import playground.dgrether.analysis.simsimanalyser.SimSimAnalysis;
import playground.dgrether.analysis.simsimanalyser.SimSimShapefileWriter;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.InMemoryEventsManager;
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
		public Network network;
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
		public DgMfd mfd;
		public double totalDelay;
		public double deltaTotalDelay;
		public LegModeHistogramImproved legHistogram;
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
	private boolean useInMemoryEvents; 

	private void setUseInMemoryEvents(boolean useInMemoryEvents) {
		this.useInMemoryEvents = useInMemoryEvents;
	}


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
			Result baseResult = baseCase.get(r.extent).get(r.timeConfig);
			r.travelTimeDelta = r.travelTime - baseResult.travelTime;
			r.travelTimePercent = r.travelTime / baseResult.travelTime * 100.0;
			r.personsDelta = r.numberOfPersons - baseResult.numberOfPersons;
			r.deltaTotalDelay = r.totalDelay - baseResult.totalDelay;
			if (! r.runInfo.baseCase) {
//				this.createAndWriteSimSimComparison(baseResult, r);
				log.warn("sim sim compare currently disabled");
			}

			this.writeMfd(r);
			this.writeLhi(r);
		}
	}

	private void writeLhi(Result result){
		CategoryHistogramWriter writer2 = new CategoryHistogramWriter();
		String baseFilename = result.runLoader.getIterationFilename(result.runInfo.iteration, "leg_histogram_improved");
		writer2.writeCsv(result.legHistogram.getCategoryHistogram(), baseFilename);
		writer2.writeGraphics(result.legHistogram.getCategoryHistogram(), baseFilename);
	}
	
	private void writeMfd(Result result) {
		String filename = result.runLoader.getIterationFilename(result.runInfo.iteration, "mfd_"+ result.timeConfig.name+ "_" + result.extent.name + ".txt");
		result.mfd.writeFile(filename);
	}

	private void createAndWriteSimSimComparison(Result baseResult, Result result) {
		SimSimAnalysis countsAnalysis = new SimSimAnalysis();
		Map<Id, List<CountSimComparison>> countSimCompMap = countsAnalysis.createCountSimComparisonByLinkId(result.network, baseResult.volumes, result.volumes);
		String shapeBase = baseResult.runInfo.runId + "_it_" + baseResult.runInfo.iteration + "_vs_";
		shapeBase += result.runInfo.runId + "_it_" + result.runInfo.iteration;

		String shapefile = shapeBase + "_simcountcomparison";
		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
		new CountsShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);

		shapefile = shapeBase + "_simsimcomparison";
		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
		new SimSimShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);
	}

	private void writeAverageTravelTimesToFile(String file) {
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
		header.append("total delay[s]");
		header.append("\t");
		header.append("total delay[hh:mm:ss]");
		header.append("\t");
		header.append("delta total delay");
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
			out.append(formatDouble(r.travelTime));
			out.append("\t");
			out.append(Time.writeTime(r.travelTime));
			out.append("\t");
			out.append(formatDouble(r.travelTimeDelta));
			out.append("\t");
			out.append(Time.writeTime(r.travelTimeDelta));
			out.append("\t");
			out.append(formatDouble(r.travelTimePercent));
			out.append("\t");
			out.append(formatDouble(r.numberOfPersons));
			out.append("\t");
			out.append(formatDouble(r.personsDelta));
			out.append("\t");
			out.append(formatDouble(r.averageTravelTime));
			out.append("\t");
			out.append(formatDouble(r.totalDelay));
			out.append("\t");
			out.append(Time.writeTime(r.totalDelay));
			out.append("\t");
			out.append(formatDouble(r.deltaTotalDelay));
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

	private String formatDouble(double d){
		DecimalFormat format = new DecimalFormat("#.#");
		return format.format(d);
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
			//			String runDirectory = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runId+"/";
			RunResultsLoader runDir = new RunResultsLoader(runDirectory, runId);
			String eventsFilename = runDir.getEventsFilename(runInfo.iteration);
			
			InMemoryEventsManager inMemoryEvents = null;
			if (useInMemoryEvents) {
				inMemoryEvents = new InMemoryEventsManager();
				MatsimEventsReader reader = new MatsimEventsReader(inMemoryEvents);
				reader.readFile(eventsFilename);
			}

			for (Extent extent : extents) {
				Network net; 
				if (extent.network != null) {
					net = extent.network;
				}
				else {
					net = runDir.getNetwork();
				}
				
				EventsManager eventsManagerUnfiltered = new EventsManagerImpl();
				LegModeHistogramImproved lhi = new LegModeHistogramImproved();
				eventsManagerUnfiltered.addHandler(lhi);
				this.processEvents(eventsManagerUnfiltered, inMemoryEvents, eventsFilename);
				
				for (TimeConfig time : times) {
					Result result = new Result();
					result.runInfo = runInfo;
					result.runLoader = runDir;
					result.extent = extent;
					result.timeConfig = time;
					result.network = net;
					result.legHistogram = lhi;
					results.addResult(result);

					EventsFilterManager eventsManager = new EventsFilterManagerImpl();
					TimeEventFilter tef = new TimeEventFilter();
					tef.setStartTime(time.startTime);
					tef.setEndTime(time.endTime);
					eventsManager.addFilter(tef);

					DgAverageTravelTimeSpeed avgTtSpeed = new DgAverageTravelTimeSpeed(net);
					eventsManager.addHandler(avgTtSpeed);

					VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600, net);
					eventsManager.addHandler(volumes);

					DgMfd mfd = new DgMfd(net);
					eventsManager.addHandler(mfd);

					SignalsData signals = runDir.getSignals();
					TTTotalDelay totalDelay = new TTTotalDelay(net, signals);
					eventsManager.addHandler(totalDelay);

					this.processEvents(eventsManager, inMemoryEvents, eventsFilename);

					result.volumes = volumes;
					result.travelTime = avgTtSpeed.getTravelTime();
					result.numberOfPersons = avgTtSpeed.getNumberOfPersons();
					result.mfd = mfd;
					result.totalDelay = totalDelay.getTotalDelay();


					log.info("Total travel time : " + avgTtSpeed.getTravelTime() + " number of persons: " + avgTtSpeed.getNumberOfPersons());
				}
			}
		}
		log.info("Calculated results.");
	}
	
	private void processEvents(EventsManager eventsManager, InMemoryEventsManager inMemoryEvents, String eventsFilename){
		if (inMemoryEvents != null){
			for (Event e : inMemoryEvents.getEvents()){
				eventsManager.processEvent(e);
			}
		}
		else {
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
		}
	}


	private static List<TimeConfig> createTimeConfig(){
		TimeConfig morning = new TimeConfig();
		morning.startTime = 5.0 * 3600.0;
		morning.endTime  = 10.0 * 3600.0;
		morning.name = "morning";
		TimeConfig evening = new TimeConfig();
		evening.startTime = 13.0 * 3600.0;
		evening.endTime = 20.0 * 3600.0;
		evening.name = "afternoon";
		TimeConfig all = new TimeConfig();
		all.startTime = 0.0;
		all.endTime = 24.0 * 3600.0;
		all.name = "all_day";
		List<TimeConfig> list = new ArrayList<TimeConfig>();
				list.add(morning);
				list.add(evening);
		list.add(all);
		return list;
	}

	private static List<RunInfo> createRunsIdList(){
		List<RunInfo> l = new ArrayList<RunInfo>();
		RunInfo ri = null;
//		add1712BaseCaseAnalysis(l);
//		add1712BaseCaseRoutesTimesRuns(l);
		
//		add1722BaseCaseAnalysis(l);
		add1724FromScratchFlowCapRuns(l);

		//		add1722BaseCaseRoutesRuns(l);
		//		add1722BaseCaseRoutesTimesRuns(l);
		//		add1726BaseCaseLongRerouteRuns(l);
		//		add1722BaseCaseButKsModelBasedOn1712Runs(l);
//				add1940Runs(l);
		return l;
	}
	
	private static void add1712BaseCaseAnalysis(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
		ri.iteration = 0;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
		ri.baseCase  = true;
		ri.iteration = 1000;
		l.add(ri);
	}

	private static void add1722BaseCaseAnalysis(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case it 0";
		ri.iteration = 0;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case it 1000";
		ri.iteration = 1000;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1740";
		ri.remark = "base case it 2000";
		ri.baseCase  = true;
		ri.iteration = 2000;
		l.add(ri);
	}

	
	private static void add1712BaseCaseRoutesTimesRuns(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
//		ri.baseCase  = true;
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1745";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark  = "base case 1712 it 2000";
		l.add(ri);
		//
		//		ri = new RunInfo();
		//		ri.runId = "1746";
		//		ri.iteration = 2000;
		//		ri.remark = "continue 1712, com > 10";
		//		l.add(ri);
		//		//
		//		ri = new RunInfo();
		//		ri.runId = "1747";
		//		ri.iteration = 2000;
		//		ri.remark  = "continue 1712, com > 50";
		//		l.add(ri);
		//		//
		//		ri = new RunInfo();
		//		ri.runId = "1748";
		//		ri.iteration = 2000;
		//		ri.remark  = "continue 1712, sylvia";
		//		l.add(ri);
	}


	private static void add1722BaseCaseRoutesTimesRuns(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.iteration = 1000;
		ri.remark  = "base case, 0.7 cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1740";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark  = "base case 1722 it 2000";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1737";
		ri.iteration = 2000;
		ri.remark  = "continue 1722, com > 10, new";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1741";
		ri.iteration = 2000;
		ri.runId = "1722";
		ri.remark  = "sylvia: continue base case 1722 for 1000 iterations";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1742";
		ri.iteration = 1000;
		ri.remark  = "start it 0: continue base case 1722 for 1000 iterations";
		l.add(ri);
	}

	private static void add1722BaseCaseRoutesRuns(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.iteration = 1000;
		ri.remark  = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1900";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark  = "base case 1722 it 2000, no time choice";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1901";
		ri.iteration = 2000;
		ri.remark  = "continue 1722, com > 10, new, no time choice";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1902";
		ri.iteration = 2000;
		ri.remark  = "sylvia: continue base case 1722 for 1000 iterations, no time choice";
		l.add(ri);
	}

	private static void add1940Runs(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1940";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark  = "base case, 0.5 cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1941";
		ri.iteration = 1000;
		ri.remark  = "base case, 0.3 cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1942";
		ri.iteration = 1000;
		ri.remark  = "base case, 0.1 cap";
		l.add(ri);
	}


	private static void add1726BaseCaseLongRerouteRuns(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1726";
		ri.remark = "base_case_more_learning";
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1743";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark  = "continue base case 1726 for 1000 iterations";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1744";
		ri.iteration = 1000;
		ri.remark  = "start it 0: continue base case 1726 for 1000 iterations";
		l.add(ri);

	}	

	private static void add1722BaseCaseButKsModelBasedOn1712Runs(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1735";
		ri.iteration = 2000;
		ri.remark = "continue 1722, com > 50";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1736";
		ri.iteration = 2000;
		ri.remark  = "continue 1722, com > 10";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1740";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark  = "base case 1722 it 2000";
		l.add(ri);
	}

	private static void add1724FromScratchFlowCapRuns(List<RunInfo> l){
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case, it 1000, 0.7 flow cap";
		ri.baseCase = true;
		ri.iteration = 1000;
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = "1724";
		ri.remark = "base case, it 1000, 0.5 flow cap";
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1725";
		ri.remark = "base case, it 1000, 0.3 flow cap";
		ri.iteration = 1000;
		l.add(ri);
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
		//		String filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
		//				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple = CottbusUtils.loadCottbusFeature(filterFeatureFilename);
		Envelope env = getTransformedEnvelope(featureTuple);
		Extent e = new Extent();
		//		e.name = "Cottbus Kreis BB";
		//		e.envelope = env;
		//		l.add(e);

		filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
		//		filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
		//				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
		featureTuple = CottbusUtils.loadFeature(filterFeatureFilename);

		String signalsBBNet = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/network_small_clean.xml.gz";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(signalsBBNet);
		
		env = getTransformedEnvelope(featureTuple);
		e = new Extent();
		e.name = "signals_bb";
		e.envelope = env;
		e.network = sc.getNetwork();
		l.add(e);

		e = new Extent();
		e.name = "all";
		l.add(e);

		return l;
	}

	private static String createRunIdIterationString(List<RunInfo> runInfos){
		String lastRunId = null;
		Integer lastIteration = null;
		String result = "";
		for (RunInfo ri : runInfos){
			if (! ri.runId.equals(lastRunId)){
				lastRunId = ri.runId;
				result += "_" + ri.runId;
			}
			if (! ri.iteration.equals(lastIteration)){
				lastIteration = ri.iteration;
				result += "_it_" + ri.iteration.toString();
			}
		}
		return result;
	}

	public static void main(String[] args) {
		List<RunInfo> runIds = createRunsIdList();
		String runIdsString = createRunIdIterationString(runIds);
		String outputDirectory = DgPaths.SHAREDSVN + "projects/cottbus/cb2ks2010/results/";
		String outputFilename = outputDirectory + "2013-08-30_travel_times_extent" + runIdsString +".txt";
		System.out.println(outputFilename);
		List<TimeConfig> times = createTimeConfig();
		List<Extent> extents = createExtentList();
		DgAnalyseCottbusKS2010 ana = new DgAnalyseCottbusKS2010();
		ana.setUseInMemoryEvents(true);
		ana.calculateResults(runIds, times, extents);
		ana.analyseResults();
		//		String outputDirectory = "C:/Users/Atany/Desktop/SHK/SVN/shared-svn/projects/cottbus/cb2ks2010/results/";
		log.info("Output written to " + outputFilename);
		ana.writeAverageTravelTimesToFile(outputFilename);

	}



}
