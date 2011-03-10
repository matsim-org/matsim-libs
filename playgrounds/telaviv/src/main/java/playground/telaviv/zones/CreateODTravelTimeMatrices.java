/* *********************************************************************** *
 * project: org.matsim.*
 * CreateODTravelTimeMatrices
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

package playground.telaviv.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

import playground.telaviv.locationchoice.FullNetworkDijkstra;
import playground.telaviv.locationchoice.FullNetworkDijkstraFactory;

public class CreateODTravelTimeMatrices {

	private static final Logger log = Logger.getLogger(CreateODTravelTimeMatrices.class);
	private static String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	
	protected Scenario scenario;
	protected ZoneMapping zoneMapping;
	protected TravelTime travelTime;
	protected TravelCost travelCost;
	protected Map<Integer, Integer> tazMapping;	// index in Array, TAZ
	protected double travelTimes[][][];	// fromZone, toZone, timeBin
	protected Set<Id> nodeIds;
	protected int numOfThreads = 6;
	protected int binSize = 60 * 60;	// 60 Minutes Bins
	protected int numSlots = 30;	// 30 Hours
	
	public static void main(String[] args) {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		ZoneMapping zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		PersonalizableTravelTime travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());		
		new CreateODTravelTimeMatrices(scenario, zoneMapping, travelTime).calculateODMatrices();
	}
	
	public CreateODTravelTimeMatrices(Scenario scenario, ZoneMapping zoneMapping, PersonalizableTravelTime travelTime) {
		this.scenario = scenario;
		this.zoneMapping = zoneMapping;
		this.travelTime = travelTime;
		
		travelCost = (TravelTimeDistanceCostCalculator) new TravelCostCalculatorFactoryImpl().createTravelCostCalculator(travelTime, scenario.getConfig().planCalcScore());	
		getConnectorNodes();
	}
	
	public double[][] getODTravelTimeMatrix(double time) {
		return travelTimes[getTimeSlotIndex(time)];
	}
	
	public double[][][] getODTravelTimeMatrices() {
		return travelTimes;
	}
	
	public int getNumSlots() {
		return this.numSlots;
	}
	
	public int getBinSize() {
		return this.binSize;
	}
	
	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.binSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}
	
	public Map<Integer, Integer> getTAZMapping() {
		return tazMapping;
	}
	
	public void calculateODMatrices() {
		log.info("Calculate ZoneMatrix with size " + nodeIds.size() + "x" + nodeIds.size());
		Counter counter = new Counter("Calculated OD Travel Time Pairs: ");
		travelTimes = new double[nodeIds.size()][nodeIds.size()][this.numSlots];
		tazMapping = new HashMap<Integer, Integer>();	
				
		Thread[] threads = new Thread[numOfThreads];
		for (int j = 0; j < numOfThreads; j++) {
			Thread thread = new ParallelThread();
			thread.setDaemon(true);
			thread.setName("ParallelCreateODTravelTimeMatrixThread" + j);
			((ParallelThread) thread).scenario = scenario;
			((ParallelThread) thread).counter = counter;
			((ParallelThread) thread).binSize = this.binSize;
			((ParallelThread) thread).numSlots = this.numSlots;
			((ParallelThread) thread).leastCostPathCalculator = new FullNetworkDijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
			((ParallelThread) thread).nodeIds = this.nodeIds;
			((ParallelThread) thread).data = new ArrayList<Data>();
			threads[j] = thread;
		}
		
		int i = 0;
		for (Id fromId : nodeIds) {
//			distances[i] = new double[nodeIds.size()];
			Node fromNode = scenario.getNetwork().getNodes().get(fromId);
			
			tazMapping.put(Integer.valueOf(fromId.toString()), i);
			
			Data data = new Data();
			data.array = travelTimes[i];
			data.fromNode = fromNode;
			((ParallelThread) threads[i % numOfThreads]).data.add(data);
						
			i++;
		}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		// wait until each thread is finished
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		}
		
		/*
		 *  Now update trips within a single zone. We set the distance to
		 *  half of the smallest travelTime of the neighbors.
		 */
		for (int t = 0; t < numSlots; t++) {
			for (int m = 0; m < nodeIds.size(); m++) {
				
				double min = Double.MAX_VALUE;
				
				for (int n = 0; n < nodeIds.size(); n++) {
					if (n == m) continue;
					if (travelTimes[n][m][t] < min) min = travelTimes[n][m][t];
					if (travelTimes[m][n][t] < min) min = travelTimes[m][n][t];
				}
				
				travelTimes[m][m][t] = min/2;
			}			
		}
		
		log.info("done.");
	}
	
	/*
	 * Find Connector Nodes. Each Zone Connector Node has the same Id as the TAZ of its zone!
	 */
	private void getConnectorNodes () {
		nodeIds = new TreeSet<Id>();
		for (Integer zoneId : zoneMapping.getParsedZones().keySet()) {
			nodeIds.add(scenario.createId(zoneId.toString()));
		}

	}
	
	private static class Data {
		Node fromNode;
		double[][] array;	// toNode, time
	}
	
	private static class ParallelThread extends Thread {
		
		Scenario scenario;
		Counter counter;
		LeastCostPathCalculator leastCostPathCalculator;
		List<Data> data;
		Set<Id> nodeIds;
		int binSize;
		int numSlots;
		
		@Override
		public void run() {
			for (Data d :data) {
				double time = 0.0;
				for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
					time = timeSlot * binSize + binSize / 2;

					int i = 0;
					
					if (leastCostPathCalculator instanceof FullNetworkDijkstra) {
						((FullNetworkDijkstra)leastCostPathCalculator).calcLeastCostTree(d.fromNode, time);
					}
					
					for (Id toId : nodeIds) {
						Node toNode = scenario.getNetwork().getNodes().get(toId);
						Path path = leastCostPathCalculator.calcLeastCostPath(d.fromNode, toNode, time);
						d.array[i][timeSlot] = path.travelTime;
						counter.incCounter();
						i++;
					}
				}
			}
		}
	}
}
