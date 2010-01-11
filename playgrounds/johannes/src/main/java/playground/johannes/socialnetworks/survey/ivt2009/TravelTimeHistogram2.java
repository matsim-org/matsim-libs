/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.SnowballPartitions;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseGraph;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseVertex;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLReader;
import playground.johannes.socialnetworks.spatial.Zone;
import playground.johannes.socialnetworks.spatial.ZoneLayer;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class TravelTimeHistogram2 {

	public static void main(String args[]) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		ScenarioImpl data = loader.getScenario();
		NetworkLayer network = (NetworkLayer) data.getNetwork();
		
		String output = config.getParam("tthistogram", "output");
		/*
		 * load events
		 */
		int binSize = Integer.parseInt(config.getParam("tthistogram", "binsize")); // one day
		int maxTime = 60*60*24;
		
		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, binSize, maxTime, new TravelTimeCalculatorConfigGroup());
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(ttCalculator);
		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
		eReader.readFile(config.getParam("tthistogram", "eventsfile"));
		/*
		 * init dijkstra
		 */
		Dijkstra router = new Dijkstra(network, new TravelCost() {
			
			public double getLinkTravelCost(Link link, double time) {
				return ttCalculator.getLinkTravelTime(link, 28800);
			}
		}, ttCalculator);
		/*
		 * read graph
		 */
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader(21781);
		SampledSpatialSparseGraph graph = reader.readGraph(config.getParam("tthistogram", "graph"));
		
		
		Population2SpatialGraph pop2graph = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.04.xml");
		double bounds[] = graph2.getBounds();
		/*
		 * read grid
		 */
		SpatialGrid<Double> grid = SpatialGrid.readFromFile(config.getParam("tthistogram", "densityfile"));
		ZoneLayer zoneLayer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerDouble densityZones = ZoneLayerDouble.createFromFile(new HashSet<Zone>(zoneLayer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		/*
		 * get sampled partition
		 */
		Set<? extends SampledSpatialSparseVertex> sbPartition = SnowballPartitions.createSampledPartition(graph.getVertices());
		TDoubleObjectHashMap<?> partitions = SpatialGraphStatistics.createDensityPartitions(sbPartition, densityZones, 2000);
		
		new File(output + "rhoPartitions").mkdirs();
		
		Distribution fastest = new Distribution();
		Distribution fastestNorm = new Distribution();
		Distribution fastestNorm2 = new Distribution();
		int counter=0;
//		int starttime = Integer.parseInt(config.getParam("tthistogram", "starttime"));
		TDoubleObjectIterator<?> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			Set<SampledSpatialSparseVertex> partition = (Set<SampledSpatialSparseVertex>) it.value();
			
			Distribution rhoDistr = new Distribution();

			for(SpatialSparseVertex v : partition) {
//				Zone z_i = zoneLayer.getZone(v.getCoordinate());
//				if(z_i != null) {
//					
//					TIntDoubleHashMap areas = new TIntDoubleHashMap();
//					for(Zone z_j : zoneLayer.getZones()) {
//						double tt = matrix.getTravelTime(z_i, z_j);
//						double a = z_j.getBorder().getArea()/(1000 * 1000);
//						
//						areas.adjustOrPutValue((int)tt/300, a, a);
//					}
//					
//					
//				TDoubleIntHashMap n_i = new TDoubleIntHashMap();
//				for(SpatialVertex v2 : v.getNeighbours()) {
//					Zone z_j = zoneLayer.getZone(v2.getCoordinate());
//					if(z_j != null) {
//					double tt = matrix.getTravelTime(z_i, z_j);
//					double bin = tt/300;
//					n_i.adjustOrPutValue(bin, 1, 1);
//					}
//				}

				Node n_i = network.getNearestNode(v.getCoordinate());			
				for(SpatialSparseVertex v2 : v.getNeighbours()) {
					Node n_j = network.getNearestNode(v2.getCoordinate());
					double tt = router.calcLeastCostPath(n_i, n_j, 6*60*60).travelTime;
					rhoDistr.add(tt);
					fastest.add(tt);
//					fastestNorm.add(tt, 1/n_i.get(tt/300));
//					double a = areas.get((int)tt/300);
//					fastestNorm2.add(tt, a/n_i.get(tt/300));

				}
//				} else {
//					System.err.println("Zone is null!");
//				}
				counter++;
				System.out.println(String.format("Processed %1$s of %2$s vertices.", counter, graph.getVertices().size()));
			}
			
			Distribution.writeHistogram(rhoDistr.absoluteDistribution(60), output + "rhoPartitions/traveltime." + it.key() + ".txt");
			Distribution.writeHistogram(rhoDistr.absoluteDistributionLog2(60), output + "rhoPartitions/traveltime.log2." + it.key() + ".txt");
			
		}
				
		Distribution.writeHistogram(fastest.absoluteDistribution(60), output + "traveltime.txt");
		Distribution.writeHistogram(fastest.absoluteDistributionLog2(60), output + "traveltime.log2.txt");
		Distribution.writeHistogram(fastestNorm.absoluteDistribution(60), output + "traveltime.norm.txt");
		Distribution.writeHistogram(fastestNorm.absoluteDistributionLog2(60), output + "traveltime.norm.log2.txt");
		Distribution.writeHistogram(fastestNorm2.absoluteDistribution(60), output + "traveltime.norm2.txt");
		Distribution.writeHistogram(fastestNorm2.absoluteDistributionLog2(60), output + "traveltime.norm2.log2.txt");
	}
}
