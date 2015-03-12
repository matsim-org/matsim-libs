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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.CountSimComparison;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsReader20;
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
import playground.dgrether.analysis.simsimanalyser.SimSimAnalysis;
import playground.dgrether.analysis.simsimanalyser.SimSimShapefileWriter;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.InMemoryEventsManager;
import playground.dgrether.events.filters.TimeEventFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;

import com.vividsolutions.jts.geom.Envelope;


public class DgAnalyseCottbusKS2010 {

	private static final Logger log = Logger.getLogger(DgAnalyseCottbusKS2010.class);

	private static final CoordinateReferenceSystem crs = MGC
			.getCRS(TransformationFactory.WGS84_UTM33N);
	
	static class TimeConfig {
		String name;
		double startTime; 
		double endTime; 
	}

	static class Extent {
		String name;
		Envelope envelope;
		Network network;
		boolean createPersonDiff = false;
	}

	static class RunInfo {
		String runId;
		String remark;
		boolean baseCase = false;
		Integer iteration;
		double flowcap = 0.7;
		double storagecap = 0.7;
	}

	static class Result {
		RunInfo runInfo;
		RunResultsLoader runLoader;
		Extent extent;
		TimeConfig timeConfig;
		Double travelTime;
		Double travelTimeDelta;
		Double averageTravelTime;
		Double numberOfPersons;
		Double travelTimePercent;
		Double personsDelta;
		VolumesAnalyzer volumes;
		Network network;
		DgMfd mfd;
		Double totalDelay;
		Double deltaTotalDelay;
		LegModeHistogramImproved legHistogram;
		Double distanceMeter;
		Double noTrips;
		Double deltaDistance;
		Double deltaNoTrips;
		Double speedKmH;
		Double deltaSpeedKmH;
		Double delayPercent;
		Double distancePercent;
		public Set<Id<Person>> seenPersonIds;
	}

	static class Results {
		private Map<RunInfo, Map<Extent, Map<TimeConfig,Result>>> resultMap = new HashMap<>();  

		private void addResult(Result result) {
			Map<Extent, Map<TimeConfig, Result>> m = resultMap.get(result.runInfo);
			if (m == null) {
				m = new HashMap<>();
				resultMap.put(result.runInfo, m);
			}
			Map<TimeConfig, Result> m2 = m.get(result.extent);
			if (m2 == null) {
				m2 = new HashMap<>();
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
			Collections.sort(ret, new ResultComparator());
			return ret;
		}

		public Map<Extent, List<Result>> getResultsByExtent() {
			Map<Extent, List<Result>> ret = new HashMap<Extent, List<Result>>();
			for (Map<Extent, Map<TimeConfig, Result>> m : resultMap.values()) {
				for (Entry<Extent, Map<TimeConfig, Result>>  m2 : m.entrySet()) {
					if (! ret.containsKey(m2.getKey())) {
						ret.put(m2.getKey(), new ArrayList<Result>());
					}
					for (Result r : m2.getValue().values()) {
						ret.get(m2.getKey()).add(r);
						Collections.sort(ret.get(m2.getKey()), new ResultComparator());
					}
				}
			}
			return ret;
		}

	}
	
	static class ResultComparator implements Comparator<Result>{
		@Override
		public int compare(Result r1, Result r2) {
			if (r1.timeConfig.name.equals(r2.timeConfig.name)) {
				if (r1.extent.name.equals(r2.extent.name)) {
					return r1.runInfo.remark.compareTo(r2.runInfo.remark);
				}
				return r1.extent.name.compareTo(r2.extent.name);
			}
			return r1.timeConfig.name.compareTo(r2.timeConfig.name);
		}
		
	}

	private Results results = new Results();
	private boolean useInMemoryEvents;

	private void setUseInMemoryEvents(boolean useInMemoryEvents) {
		this.useInMemoryEvents = useInMemoryEvents;
	}

	private void analyseResult(Result r){
		double averageTT = r.travelTime / r.numberOfPersons;
		r.averageTravelTime = averageTT;
		r.speedKmH = (r.distanceMeter/ r.travelTime) * 3.6;

	}		
	
	
	


	private void compareWithBaseCaseResult(Result r, Result baseResult){
		r.travelTimeDelta = r.travelTime - baseResult.travelTime;
		r.travelTimePercent = r.travelTime / baseResult.travelTime * 100.0;
		r.deltaTotalDelay = r.totalDelay - baseResult.totalDelay;
		r.delayPercent = r.totalDelay / baseResult.totalDelay * 100.0;
		r.deltaDistance = r.distanceMeter - baseResult.distanceMeter;
		r.distancePercent = r.distanceMeter / baseResult.distanceMeter * 100.0;
		
		r.deltaSpeedKmH = r.speedKmH - baseResult.speedKmH;
		r.deltaNoTrips = r.noTrips - baseResult.noTrips;
		r.personsDelta = r.numberOfPersons - baseResult.numberOfPersons;
	}
	
	private void analyseResults() {
		Map<Extent, Map<TimeConfig, Result>> baseCase = null;
		for (RunInfo r : results.resultMap.keySet()) {
			if (r.baseCase) {
				baseCase = results.resultMap.get(r);
			}
		}
		for (Result r : results.getResults()) {
			analyseResult(r);
			Result baseResult = baseCase.get(r.extent).get(r.timeConfig);
			analyseResult(baseResult);
			compareWithBaseCaseResult(r, baseResult);
			
			if (! r.runInfo.baseCase && r.extent.createPersonDiff) {
				this.createAndWritePersonDiff(baseResult, r);
//				this.createAndWriteSimSimComparison(baseResult, r);
				log.warn("sim sim compare currently disabled");
			}

			this.writeMfd(r);
			this.writeLhi(r);
		}
	}

	private void createAndWritePersonDiff(Result baseResult, Result r) {
		Set<Id<Person>> allPersonIds = new HashSet<>();
		allPersonIds.addAll(baseResult.seenPersonIds);
		allPersonIds.addAll(r.seenPersonIds);
		Set<Id<Person>> disattractedPersonsIds = new HashSet<>();
		Set<Id<Person>> attractedPersonsIds = new HashSet<>();
		for (Id<Person> id : allPersonIds) {
			if (baseResult.seenPersonIds.contains(id) && ! r.seenPersonIds.contains(id)) {
				disattractedPersonsIds.add(id);
			}
			if (! baseResult.seenPersonIds.contains(id) && r.seenPersonIds.contains(id)) {
				attractedPersonsIds.add(id);
			}
		}
		
		Network n = baseResult.runLoader.getNetwork();
		Population pop = baseResult.runLoader.getPopulation();
		String outDir = DgPaths.REPOS + "shared-svn/projects/cottbus/data/optimization/cb2ks2010/diffs/";
		File out = IOUtils.createDirectory(outDir + baseResult.runInfo.runId + "_vs_" + r.runInfo.runId + "_plans_base_case_disattracted/");
		Population newPop = this.getFilteredPopulation(pop, disattractedPersonsIds);
		DgSelectedPlans2ESRIShape sps = new DgSelectedPlans2ESRIShape(newPop, n, DgAnalyseCottbusKS2010.crs, out.getAbsolutePath());
		sps.writeActs(baseResult.runInfo.runId + "_vs_" + r.runInfo.runId + "_disattracted_acts");

		out = IOUtils.createDirectory(outDir + baseResult.runInfo.runId + "_vs_" + r.runInfo.runId + "_plans_base_case_attracted/");
		newPop = this.getFilteredPopulation(pop, attractedPersonsIds);
		sps = new DgSelectedPlans2ESRIShape(newPop, n, DgAnalyseCottbusKS2010.crs, out.getAbsolutePath());
		sps.writeActs(baseResult.runInfo.runId + "_vs_" + r.runInfo.runId + "_attracted_acts");
	}
	
	private Population getFilteredPopulation(Population pop, Set<Id<Person>> personIdsOfInterest){
		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		for (Person person : pop.getPersons().values()) {
			if (personIdsOfInterest.contains(person.getId())) {
				newPop.addPerson(person);
			}
		}
		return newPop;
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
//		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
//		new CountsShapefileWriter(result.network, Cottbus2KS2010.CRS).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);

		shapefile = shapeBase + "_simsimcomparison";
		shapefile = result.runLoader.getIterationFilename(result.runInfo.iteration, shapefile);
		new SimSimShapefileWriter(result.network, DgAnalyseCottbusKS2010.crs).writeShape(shapefile + ".shp", countSimCompMap, baseResult.runInfo.runId, result.runInfo.runId);
	}


	private void calculateResults(List<RunInfo> runInfos, List<TimeConfig> times, List<Extent> extents) {
		for (RunInfo runInfo: runInfos) {
			String runId = runInfo.runId;
			String runDirectory = DgPaths.REPOS + "runs-svn/cottbus/run"+runId+"/";
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

					DgMfd mfd = new DgMfd(net, runInfo.storagecap);
					eventsManager.addHandler(mfd);

					TtTotalDelay totalDelay = new TtTotalDelay(net);
					eventsManager.addHandler(totalDelay);

					this.processEvents(eventsManager, inMemoryEvents, eventsFilename);

					mfd.completedEventsHandling();
					result.volumes = volumes;
					result.travelTime = avgTtSpeed.getTravelTime();
					result.numberOfPersons = avgTtSpeed.getNumberOfPersons();
					result.mfd = mfd;
					result.totalDelay = totalDelay.getTotalDelay();
					result.distanceMeter = avgTtSpeed.getDistanceMeter();
					result.noTrips = avgTtSpeed.getNumberOfTrips();
					result.seenPersonIds = avgTtSpeed.getSeenPersonIds();
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


	public static List<TimeConfig> createTimeConfig(){
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
//		list.add(evening);
//		list.add(all);
		return list;
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
		env = transform(env, featureTuple.getFirst(), DgAnalyseCottbusKS2010.crs);
		return env;
	}

	/**
	 * In this method one can compose spatial extends that are used for analysis.
	 */
	public static List<Extent> createExtentList(){
		List<Extent> l = new ArrayList<Extent>();
		String filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		//		String filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
		//				+ "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple = CottbusUtils.loadCottbusFeature(filterFeatureFilename);
		Envelope env = getTransformedEnvelope(featureTuple);
		Extent e = null;
		
		//		e.name = "Cottbus Kreis BB";
		//		e.envelope = env;
		//		l.add(e);

		filterFeatureFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
		//		filterFeatureFilename = "C:/Users/Atany/Desktop/SHK/SVN/"
		//				+ "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/shapes/bounding_box.shp";
//		featureTuple = CottbusUtils.loadFeature(filterFeatureFilename);
//		env = getTransformedEnvelope(featureTuple);

		String signalsBBNet = DgPaths.REPOS + "shared-svn/projects/cottbus/data/optimization/cb2ks2010/2013-07-31_minflow_10_evening_peak/network_small.xml.gz";
		Scenario scSignalsBoundingBox = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scSignalsBoundingBox);
		netReader.readFile(signalsBBNet);

		String signalSystemsFile = DgPaths.REPOS + "shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_systems_no_13.xml";
		SignalSystemsData signalSystems = new SignalSystemsDataImpl();
		SignalSystemsReader20 signalsReader = new SignalSystemsReader20(signalSystems);
		signalsReader.readFile(signalSystemsFile);
//		signalSystems.getSignalSystemData().remove(new IdImpl("13")); //just to make sure we have consistent data
		
		e = new Extent();
		e.name = "signalized_links";
		e.network = new DgSignalizedLinksNetwork().createSmallNetwork(scSignalsBoundingBox.getNetwork(), signalSystems);
//		l.add(e);	

		e = new Extent();
		e.name = "signals_bb";
//		e.envelope = env;
		e.network = scSignalsBoundingBox.getNetwork();
//		l.add(e);
		
		
		String cityNetwork = DgPaths.REPOS  + "shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/cottbus_city_network/network_city_wgs84_utm33n.xml.gz";
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader2= new MatsimNetworkReader(sc2);
		netReader2.readFile(cityNetwork);
		e = new Extent();
		e.name = "city";
		e.network = sc2.getNetwork();
//		l.add(e);

		e = new Extent();
		e.name = "city_w_hole";
		for (Link link : scSignalsBoundingBox.getNetwork().getLinks().values()) {
			sc2.getNetwork().removeLink(link.getId()); 
		}
		e.network = sc2.getNetwork();
//		l.add(e);
		
		e = new Extent();
		e.name = "all";
		e.createPersonDiff = false;
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
	
	private static String createTimesString(List<TimeConfig> l){
		String result = "";
		for (TimeConfig c : l) {
			result += c.name + "_";
		}
		return result;
	}

	private static String createExtentString(List<Extent> l){
		String result = "";
		for (Extent e : l) {
			result += e.name + "_";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}

	
	public static List<RunInfo> createRunsIdList(){
		List<RunInfo> l = new ArrayList<RunInfo>();
		
//		CottbusRuns.addReduceFlowCapacityRunsNoIterations(l);
//		CottbusRuns.addReduceFlowCapacityRunsIterationsFromScratch(l);
//		CottbusRuns.addReduceFlowCapacityRunsIterationsRoutesOnlyFromBaseCase(l);
//		CottbusRuns.addStorageCapacityRuns(l);
//		CottbusRuns.addFlowStorageCapacityRuns(l);
		
//		CottbusRuns.add1740vs1745BaseCaseAnalysis(l);
		
//		CottbusRuns.add1712BaseCaseAnalysis(l);
//		CottbusRuns.add1712BaseCaseNoChoice(l);
//		CottbusRuns.add1712BaseCaseRoutesOnlyRuns(l);
//		CottbusRuns.add1712BaseCaseRoutesOnlyRandomRuns(l);
//		CottbusRuns.add1712BaseCaseRoutesOnlyHighStorageCapRuns(l);
		
//		CottbusRuns.add1933BaseCaseRoutesOnlyRandomRuns(l);
		
//		CottbusRuns.add1712BaseCaseRoutesOnlyHighStorageCapRunsOnSimplifiedNetwork(l);
//		CottbusRuns.add1712BaseCaseRoutesTimesHighStorageCapRunsOnSimplifiedNetwork(l);
		
//		CottbusRuns.add1930BaseCase(l);
//		CottbusRuns.add1712BaseCaseRoutesOnlyRuns5Percent(l);

//		CottbusRuns.add1712BaseCaseRoutesOnlyRunsBeta20(l);
//		CottbusRuns.add1712BaseCaseRoutesTimesRuns(l);
		
//		CottbusRuns.add1722BaseCaseAnalysis(l);
//		CottbusRuns.add1722BaseCaseRoutesOnlyRuns(l);
//		CottbusRuns.add1722BaseCaseRoutesTimesRuns(l);
//		CottbusRuns.add1726BaseCaseLongRerouteRuns(l);
//		CottbusRuns.add1722BaseCaseButKsModelBasedOn1712Runs(l);
		
//		CottbusRuns.add1972BaseCaseRoutesOnlyRandomRuns(l);
//		CottbusRuns.add1973BaseCaseRoutesTimesRandomRuns(l);
		
//		CottbusRuns.addBaseCaseOptAndRandomIt1400Runs(l, 2026, 2027, 2028, 2029, 2030, 2031);
//		CottbusRuns.addBaseCaseOptAndRandomIt1400Runs(l, 2032, 2033, 2034, 2035, 2036, 2037);
//		CottbusRuns.addBaseCaseOptAndRandomAndAnotherIt1400Runs(l, 2032, 2033, 2034, 2035, 2036, 2037, 2038);
		
//		CottbusRuns.addBaseCaseAndOptIt1400Runs(l, 2042, 2039);
//		CottbusRuns.addBaseCaseOptAndOptFixed(l, 2042, 2039, 2040, 2041);
//		CottbusRuns.addBaseCaseOptAndOptFixedAndOptChoice(l, 2042, 2039, 2040, 2041, 2043, 2044);
		
		// without time choice
//		CottbusRuns.addBaseCaseOptAndRouteChoice1400(l, 2042, 2039, 2047);
		// with time choice
//		CottbusRuns.addBaseCaseOptAndRouteChoice1400(l, 2046, 2045, 2048);
		// parameter test
		CottbusRuns.addOptAndRouteChoice1400(l, 2054, 2055);
		
		return l;
	}
	

	public static void main(String[] args) {
		List<RunInfo> runIds = createRunsIdList();
		String runIdsString = createRunIdIterationString(runIds);
		String outputDirectory = DgPaths.SHAREDSVN + "projects/cottbus/data/optimization/cb2ks2010/" 
				+ "2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/results/";
		List<TimeConfig> times = createTimeConfig();
		String timesString = createTimesString(times);
		List<Extent> extents = createExtentList();
		String extentString = createExtentString(extents);
		String outputFilename = outputDirectory + "2015-03-12_analysis" + runIdsString + "_" +  timesString;
		System.out.println(outputFilename);
		DgAnalyseCottbusKS2010 ana = new DgAnalyseCottbusKS2010();
		ana.setUseInMemoryEvents(false);
		ana.calculateResults(runIds, times, extents);
		ana.analyseResults();
		new ResultsWriter().writeResultsTable(ana.results, outputFilename + extentString +".txt");
		Map<Extent, List<Result>> resultsByExtent = ana.results.getResultsByExtent();
		for (Entry<Extent, List<Result>> extent : resultsByExtent.entrySet()) {
			new LatexResultsWriter().writeResultsTable(extent.getValue(), outputFilename + extent.getKey().name + ".tex");
		}
		log.info("Output written to " + outputFilename);

	}



}
