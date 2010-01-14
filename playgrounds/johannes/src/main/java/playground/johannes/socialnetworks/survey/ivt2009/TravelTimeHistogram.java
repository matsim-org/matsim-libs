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

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseVertex;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.socialnetworks.snowball2.SnowballPartitions;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLegacy;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;

/**
 * @author illenberger
 *
 */
public class TravelTimeHistogram {

	public static void main(String args[]) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
//		ScenarioLoader loader = new ScenarioLoader(config);
//		loader.loadScenario();
//		ScenarioImpl data = loader.getScenario();
//		NetworkLayer network = (NetworkLayer) data.getNetwork();
//		
		String output = config.getParam("tthistogram", "output");
//		/*
//		 * load events
//		 */
//		int binSize = Integer.parseInt(config.getParam("tthistogram", "binsize")); // one day
//		int maxTime = 60*60*24;
//		
//		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, binSize, maxTime, new TravelTimeCalculatorConfigGroup());
//		EventsImpl events = new EventsImpl();
//		events.addHandler(ttCalculator);
//		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
//		eReader.readFile(config.getParam("tthistogram", "eventsfile"));
//		/*
//		 * init dijkstra
//		 */
//		Dijkstra router = new Dijkstra(network, new TravelCost() {
//			
//			public double getLinkTravelCost(Link link, double time) {
//				return ttCalculator.getLinkTravelTime(link, 28800);
//			}
//		}, ttCalculator);
		/*
		 * read graph
		 */
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialSparseGraph graph = reader.readGraph("/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/graph/graph.graphml");
		/*
		 * read zones
		 */
		ZoneLayerLegacy zoneLayer = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		TravelTimeMatrix matrix = TravelTimeMatrix.createFromFile(new HashSet<ZoneLegacy>(zoneLayer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.txt");
//		Population2SpatialGraph pop2graph = new Population2SpatialGraph();
//		SpatialGraph graph2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.02.xml");
//		double bounds[] = graph2.getBounds();
		/*
		 * read grid
		 */
//		SpatialGrid<Double> grid = SpatialGrid.readFromFile(config.getParam("tthistogram", "densityfile"));
//		ZoneLayer zones = ZoneLayer.createFromShapeFile("");
		ZoneLayerDouble densityZones = ZoneLayerDouble.createFromFile(new HashSet<ZoneLegacy>(zoneLayer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		/*
		 * get sampled partition
		 */
		Set<? extends SampledSpatialSparseVertex> sbPartition = SnowballPartitions.createSampledPartition(graph.getVertices());
//		TDoubleObjectHashMap<?> partitions = SpatialGraphStatistics.createDensityPartitions(sbPartition, densityZones, 2000);
		
		new File(output + "rhoPartitions").mkdirs();
		
		Distribution fastest = new Distribution();
		Distribution fastestNorm = new Distribution();
		Distribution fastestNorm2 = new Distribution();
		int counter=0;
//		int starttime = Integer.parseInt(config.getParam("tthistogram", "starttime"));
//		TDoubleObjectIterator<?> it = partitions.iterator();
//		for(int i = 0; i < partitions.size(); i++) {
//			it.advance();
//			Set<SampledSpatialVertex> partition = (Set<SampledSpatialVertex>) it.value();
//		Set<SampledSpatialSparseVertex> partition = (Set<SampledSpatialSparseVertex>) sbPartition;

//		Distribution rhoDistr = new Distribution();
		
		double binsize = 300;
		for (SpatialSparseVertex v : sbPartition) {
			ZoneLegacy z_i = zoneLayer.getZone(v.getCoordinate());
			if (z_i != null) {

				TIntDoubleHashMap areas = new TIntDoubleHashMap();
				TIntIntHashMap n_i = new TIntIntHashMap();
				for (ZoneLegacy z_j : densityZones.getZones()) {
					double tt = matrix.getTravelTime(z_i, z_j);
					double a = z_j.getBorder().getArea() / (1000 * 1000);
					int bin = (int)Math.ceil(tt/binsize);
					
					areas.adjustOrPutValue(bin, a, a);
					
					double rho = densityZones.getValue(z_j);
					int n = (int)(a * rho);
					n_i.adjustOrPutValue(bin, n, n);
				}

				
//				for (SpatialVertex v2 : graph2.getVertices()) {
//					Zone z_j = zoneLayer.getZone(v2.getCoordinate());
//					if (z_j != null) {
//						double tt = matrix.getTravelTime(z_i, z_j);
//						double bin = tt / 300;
//						n_i.adjustOrPutValue(bin, 1, 1);
//					}
//				}

				for (SpatialSparseVertex v2 : v.getNeighbours()) {
					ZoneLegacy z_j = zoneLayer.getZone(v2.getCoordinate());
					if (z_j == null)
						System.err.println("Zone is null");
					else {
						double tt = matrix.getTravelTime(z_i, z_j);
						int bin = (int)Math.ceil(tt/binsize);
//						rhoDistr.add(tt);
						fastest.add(tt);
						fastestNorm.add(tt, 1 / (double)n_i.get(bin));
						double a = areas.get(bin);
						fastestNorm2.add(tt, a / (double)n_i.get(bin));
					}
				}
			} else {
				System.err.println("Zone is null!");
			}
			counter++;
			System.out.println(String.format(
					"Processed %1$s of %2$s vertices.", counter, graph
							.getVertices().size()));
		}

		// Distribution.writeHistogram(rhoDistr.absoluteDistribution(60), output
		// + "rhoPartitions/traveltime." + it.key() + ".txt");
		// Distribution.writeHistogram(rhoDistr.absoluteDistributionLog2(60),
		// output + "rhoPartitions/traveltime.log2." + it.key() + ".txt");

		// }

		Distribution.writeHistogram(fastest.absoluteDistribution(60), output + "traveltime.txt");
		Distribution.writeHistogram(fastest.absoluteDistributionLog2(60), output + "traveltime.log2.txt");
		Distribution.writeHistogram(fastestNorm.absoluteDistribution(60), output + "traveltime.norm.txt");
		Distribution.writeHistogram(fastestNorm.absoluteDistributionLog2(60), output + "traveltime.norm.log2.txt");
		Distribution.writeHistogram(fastestNorm2.absoluteDistribution(60), output + "traveltime.norm2.txt");
		Distribution.writeHistogram(fastestNorm2.absoluteDistributionLog2(60), output + "traveltime.norm2.log2.txt");
	}
}
