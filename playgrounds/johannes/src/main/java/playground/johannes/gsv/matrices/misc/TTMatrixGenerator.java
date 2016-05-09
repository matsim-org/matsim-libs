/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.misc;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import playground.johannes.gsv.matrices.misc.SpanningTree.NodeData;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.HashMatrix;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author johannes
 *
 */
public class TTMatrixGenerator {
	
	private static final Logger logger = Logger.getLogger(TTMatrixGenerator.class);

	private Network network;
	
	private Map<String, Collection<Node>> zone2Node;
	
	private Map<Id<Node>, String> node2Zone;
	
	private class Worker implements Runnable {

		private final String originId;
		
		private final SpanningTree sTree;
		
		private final HashMatrix<String, CellData> ttMatrix;
		
		private int errors;
		
		public Worker(String originId) {
			this.originId = originId;
			
			FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
			sTree = new SpanningTree(tt, tt);
			
			ttMatrix = new HashMatrix<>();
		}

		public HashMatrix<String, CellData> getMatrix() {
			return ttMatrix;
		}
		
		public int getErrors() {
			return errors;
		}
		
		@Override
		public void run() {
			Collection<Node> nodes = zone2Node.get(originId);
			
			for(Node node : nodes) {
				sTree.setOrigin(node);
				sTree.setDepartureTime(0);
				sTree.run(network);
				
				for(Entry<Node, NodeData> entry : sTree.getTree().entrySet()) {
					String targetId = node2Zone.get(entry.getKey().getId());
					if(targetId != null) {
						CellData cData = ttMatrix.get(originId, targetId);
						
						if(cData == null) {
							cData = new CellData();
							ttMatrix.set(originId, targetId, cData);
						}
						
						cData.count++;
						cData.ttSum += entry.getValue().getCost();
					} else {
						errors++;
					}
				}
				logger.info("Processed node...");
			}
		}
	}
	
	private static class CellData {
		
		private double ttSum;
		
		private double count;
	}
	
	public NumericMatrix generate(Network network, ZoneCollection zones, String zoneIdKey, int nThreads) {
		this.network = network;
		
		logger.info("Initializing node mappings...");
		initMappings(zones, zoneIdKey);
		
		logger.info("Calculation travel times...");
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		Set<Future<Worker>> futures = new HashSet<>();
		
		for(Zone zone : zones.getZones()) {
			Worker worker = new Worker(zone.getAttribute(zoneIdKey));
			futures.add(executor.submit(worker, worker));
		}
		
		ProgressLogger.init(futures.size(), 1, 10);
		
		HashMatrix<String, CellData> sumMatrix = new HashMatrix<>();
		int errors = 0;
		
		for(Future<Worker> future : futures) {
			try {
				Worker worker = future.get();
				HashMatrix<String, CellData> m = worker.getMatrix();
				Set<String> keys = m.keys();
		
				for(String i : keys) {
					for(String j : keys) {
				
						CellData cData = m.get(i, j);
						if(cData != null) {
						
							CellData cDataSum = sumMatrix.get(i, j);
							if(cDataSum == null) {
								cDataSum = new CellData();
								sumMatrix.set(i, j, cDataSum);
							}
							
							cDataSum.count += cData.count;
							cDataSum.ttSum += cData.ttSum;
						}
					}
				}
				
				errors += worker.getErrors();
				
				ProgressLogger.step();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		ProgressLogger.terminate();
		
		if(errors > 0) {
			logger.info(String.format("%s errors occured.", errors));
		}
		
		logger.info("Collection results...");
		
		NumericMatrix ttMatrix = new NumericMatrix();
		Set<String> keys = sumMatrix.keys();
		for(String i : keys) {
			for(String j : keys) {
				CellData cData = sumMatrix.get(i, j);
				double avr = cData.ttSum / cData.count;
				ttMatrix.set(i, j, avr);
			}
		}
		
		
		return ttMatrix;
	}
	
	private void initMappings(ZoneCollection zones, String zoneIdKey) {
		node2Zone = new ConcurrentHashMap<>();
		zone2Node = new ConcurrentHashMap<>();
		
		for(Node node : network.getNodes().values()) {
			Coordinate c = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
			Zone zone = zones.get(c);
			if(zone != null) {
				String id = zone.getAttribute(zoneIdKey);
				node2Zone.put(node.getId(), id);
				
				Collection<Node> nodes = zone2Node.get(id);
				if(nodes == null) {
					nodes = new HashSet<>();
					zone2Node.put(id, nodes);
				}
				nodes.add(node);
			}
		}
	}
	
	public static void main(String args[]) throws IOException {
		String netFile = args[0];
		String zoneFile = args[1];
		String zoneIdKey = args[2];
		int nThreads = Integer.parseInt(args[3]);
		String out = args[4];
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		logger.info("Loading network...");
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
		reader.parse(netFile);
		
		logger.info("Loading zones...");
		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, zoneIdKey, null);
		
		TTMatrixGenerator generator = new TTMatrixGenerator();
		NumericMatrix m = generator.generate(scenario.getNetwork(), zones, zoneIdKey, nThreads);
		
		logger.info("Writing zones...");
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, out);
		logger.info("Done.");
	}
}
