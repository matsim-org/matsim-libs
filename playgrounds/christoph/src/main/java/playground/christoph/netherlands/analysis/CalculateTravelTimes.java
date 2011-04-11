/* *********************************************************************** *
 * project: org.matsim.*
 * CreateODDistanceMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.netherlands.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

import playground.christoph.netherlands.zones.SpecialZones;

public class CalculateTravelTimes {

	final private static Logger log = Logger.getLogger(CalculateTravelTimes.class);

	private static String networkFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.xml.gz";
	private static String shapeFile = "../../matsim/mysimulations/netherlands/zones/postcode4_org.shp";
	private static String eventsFile = "../../matsim/mysimulations/netherlands/output/ITERS/it.100/100.events.txt.gz";
	private static String outFileEmpty = "../../matsim/mysimulations/netherlands/TravelTimesMatrixEmptyNetwork.csv";
	private static String outFileMorning = "../../matsim/mysimulations/netherlands/TravelTimesMatrixMorningPeak.csv";
	private static String outFileMidday = "../../matsim/mysimulations/netherlands/TravelTimesMatrixMidday.csv";
	private static String outFileEvening = "../../matsim/mysimulations/netherlands/TravelTimesMatrixEveningPeak.csv";
	
	// morning peak, mid day, evening peak
	private static double morningPeak = 8 * 3600;
	private static double midDay = 12 * 3600;
	private static double eveningPeak = 18 * 3600;
	
	// mean trip travel time
	private static double meanTripTravelTime = 19 * 60;
	
	private String outFile = "";
	private boolean useFreeSpeedTravelTime = true;
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");
	private int numOfThreads = 6;
	private double startTime = 0.0;
	
	private double[][] travelTimes;
	private Set<Id> nodeIds;
	
	private Scenario scenario;
	private TravelTime travelTime;
	private TravelCost travelCost;
	
	public static void main(String[] args) throws Exception {
		
		CalculateTravelTimes calculateTravelTimes = null;
		
		// empty network
		calculateTravelTimes = new CalculateTravelTimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		calculateTravelTimes.outFile = outFileEmpty;
		calculateTravelTimes.calculateTravelTimes();
		
		// morning peak
		calculateTravelTimes = new CalculateTravelTimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		calculateTravelTimes.outFile = outFileMorning;
		calculateTravelTimes.startTime = morningPeak - meanTripTravelTime / 2;
		calculateTravelTimes.useFreeSpeedTravelTime = false;
		calculateTravelTimes.calculateTravelTimes();

		// midday peak
		calculateTravelTimes = new CalculateTravelTimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		calculateTravelTimes.outFile = outFileMidday;
		calculateTravelTimes.startTime = midDay - meanTripTravelTime / 2;
		calculateTravelTimes.useFreeSpeedTravelTime = false;
		calculateTravelTimes.calculateTravelTimes();

		// evening peak
		calculateTravelTimes = new CalculateTravelTimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		calculateTravelTimes.outFile = outFileEvening;
		calculateTravelTimes.startTime = eveningPeak - meanTripTravelTime / 2;
		calculateTravelTimes.useFreeSpeedTravelTime = false;
		calculateTravelTimes.calculateTravelTimes();

	}
	
	public CalculateTravelTimes(Scenario scenario) throws Exception {
		this.scenario = scenario;
	}
	
	private void calculateTravelTimes() throws Exception {
		
		log.info("Read Network File...");
		readNetworkFile(scenario);
		log.info("done.");

		log.info("Initialize Calculators...");
		initCalulators();
		log.info("done.");
		
		log.info("Get Connector Nodes...");
		getConnectorNodes();
		log.info("done.");
				
		log.info("Calculate TravelTimes...");
		doCalculation();
		log.info("done.");
		
		log.info("Write TravelTimes...");
		writeFile();
		log.info("done.");
	}
	
	private void initCalulators() throws Exception {
		if (useFreeSpeedTravelTime) {
			travelTime = new FreeSpeedTravelTimeCalculator();
		}
		else {
			travelTime = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
			EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
			eventsManager.addHandler((TravelTimeCalculator)travelTime);
			
			EventsReaderTXTv1 reader = new EventsReaderTXTv1(eventsManager);
			reader.readFile(eventsFile);			
		}
		
		travelCost = new FreeSpeedTravelCost(travelTime);
	}
	
	private void readNetworkFile(Scenario scenario) {
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}
	
	private void doCalculation() throws Exception {
		travelTimes = new double[nodeIds.size()][];
		Counter counter = new Counter("Calculated Travel Times: ");
		
		Thread[] threads = new Thread[numOfThreads];
		for (int j = 0; j < numOfThreads; j++) {
			Thread thread = new ParallelThread();
			thread.setDaemon(true);
			thread.setName("ParallelCreateODTravelTimeMatrixThread" + j);
			((ParallelThread) thread).scenario = scenario;
			((ParallelThread) thread).counter = counter;
			((ParallelThread) thread).leastCostPathCalculator = new FullNetworkDijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
			((ParallelThread) thread).nodeIds = this.nodeIds;
			((ParallelThread) thread).startTime = this.startTime;
			((ParallelThread) thread).fromIds = new ArrayList<Id>();
			((ParallelThread) thread).travelTimesList = new ArrayList<double[]>();
			threads[j] = thread;
		}        
		
		int i = 0;
		for (Id fromId : nodeIds) {
			double[] newArray = new double[nodeIds.size()];
			travelTimes[i] = newArray;
			
			((ParallelThread) threads[i % numOfThreads]).fromIds.add(fromId);
			((ParallelThread) threads[i % numOfThreads]).travelTimesList.add(newArray);
						
			i++;
		}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		// wait until each thread is finished
		for (Thread thread : threads) {
			thread.join();
		}
	}
	
	/*
	 * Find Connector Nodes. Each Zone Connector Node has the same Id as the TAZ of its zone!
	 */
	private void getConnectorNodes() throws Exception {

		nodeIds = new TreeSet<Id>();
		
		FeatureSource featureSource = ShapeFileReader.readDataFile(shapeFile);
		for (Object o : featureSource.getFeatures()) {
			Feature zone = (Feature) o;
			int zoneId = ((Long)zone.getAttribute(3)).intValue();	// PostCode
			if (SpecialZones.skipZone(zoneId)) continue;	// skip zone if it is invalid
			Id nodeId = scenario.createId(String.valueOf(zoneId));
			nodeIds.add(nodeId);
		}
	}
	
	public void writeFile() throws Exception {
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
	    
		fos = new FileOutputStream(outFile);
		osw = new OutputStreamWriter(fos, charset);
		bw = new BufferedWriter(osw);
		
		// write Header
		String header = "";
		for (Id id : nodeIds) {
			header = header + separator + id.toString(); 
		}
		bw.write(header);
		bw.write("\n");
		
		Iterator<Id> rows = nodeIds.iterator();
		// write Values
		for (double[] array : travelTimes) {	
			bw.write(String.valueOf(rows.next()));
			for (double value : array) {
				bw.write(separator);
				bw.write(String.valueOf(value));
			}
			bw.write("\n");
		}
		
		bw.close();
		osw.close();
		fos.close();
	}
	
	private static class ParallelThread extends Thread {
		
		Scenario scenario;
		Counter counter;
		LeastCostPathCalculator leastCostPathCalculator;
		Set<Id> nodeIds;
		List<Id> fromIds;
		List<double[]> travelTimesList;
		double startTime;
		
		@Override
		public void run() {
			for (int index = 0; index < fromIds.size(); index++) {
				Id fromId = fromIds.get(index);
				Node fromNode = scenario.getNetwork().getNodes().get(fromId);
				double[] travelTimes = travelTimesList.get(index);
				
				if (leastCostPathCalculator instanceof FullNetworkDijkstra) {
					((FullNetworkDijkstra)leastCostPathCalculator).calcLeastCostTree(fromNode, startTime);
				}
				
				int i = 0;
				for (Id toId : nodeIds) {
					Node toNode = scenario.getNetwork().getNodes().get(toId);
					Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, startTime);
					travelTimes[i] = path.travelCost;
					counter.incCounter();
					i++;
				}
			}
		}
	}
		
	public class FreeSpeedTravelCost implements PersonalizableTravelCost, TravelMinCost {
		private TravelTime travelTime;
		
		public FreeSpeedTravelCost(TravelTime travelTime) {
			this.travelTime = travelTime;
		}
		
		public void setPerson(Person person) {
			// nothing to do here
		}

		public double getLinkGeneralizedTravelCost(Link link, double time) {
			return travelTime.getLinkTravelTime(link, time);
		}

		public double getLinkMinimumTravelCost(Link link) {
			return travelTime.getLinkTravelTime(link, 0.0);
		}
	}
}