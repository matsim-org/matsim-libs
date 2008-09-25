/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnets;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.router.Dijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.utils.io.IOUtils;

import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class TravelTimeHistogram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String networkfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/ivtch-osm.xml";
		String configfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/config.socialNetGenerator.xml";
		String egofile = "/Users/fearonni/vsp-work/socialnets/data-analysis/egos_wgs84.txt";
		String alterfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/alters_wgs84.txt";
		String outputfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/traveltimes.txt";
		/*
		 * Load network...
		 */
//		System.out.println("Loading network...");
//		NetworkLayer network = new NetworkLayer();
//		new MatsimNetworkReader(network).readFile(networkfile);
		Config config = Gbl.createConfig(new String[]{configfile});
		ScenarioData data = new ScenarioData(config);
		NetworkLayer network = data.getNetwork();
		/*
		 * Load egos...
		 */
		System.out.println("Loading egos...");
		HashMap<String, Ego> egos = new HashMap<String, Ego>();
		WGS84toCH1903LV03 transform = new WGS84toCH1903LV03();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(egofile);
			String line;
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				Ego ego = new Ego();
				ego.id = tokens[0];
				Coord coord = new CoordImpl(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
				ego.homeloc = transform.transform(coord);
				egos.put(ego.id, ego);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * Load alters...
		 */
		System.out.println("Loading alters...");
		try {
			BufferedReader reader = IOUtils.getBufferedReader(alterfile);
			String line;
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				Ego ego = egos.get(tokens[0]);
				if(ego == null) {
					System.err.println("Ego not found!");
				} else {
					Coord coord = new CoordImpl(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
					ego.alters.add(transform.transform(coord));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * Make grid...
		 */
		Population population = data.getPopulation();
				
		double maxX = 0;
		double maxY = 0;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for(Person person : population) {
			Coord homeLoc = person.getSelectedPlan().getFirstActivity().getCoord();
			maxX = Math.max(maxX, homeLoc.getX());
			maxY = Math.max(maxY, homeLoc.getY());
			minX = Math.min(minX, homeLoc.getX());
			minY = Math.min(minY, homeLoc.getY());
		}
		double resolution = 500;
		SpatialGrid<Double> grid = new SpatialGrid<Double>(new CoordImpl(minX, minY), new CoordImpl(maxX, maxY), resolution);
		for(Person person : population) {
			Coord homeLoc = person.getSelectedPlan().getFirstActivity().getCoord();
			Double count = grid.getValue(homeLoc);
			if(count == null)
				count = 0.0;
			count++;
			grid.setValue(count, homeLoc);
		}
		
		/*
		 * Map coordinates to links and route between links...
		 */
		System.out.println("Calculating routes...");
		
		FreespeedTravelTime freett = new FreespeedTravelTime();
		TravelDistanceCost disttt = new TravelDistanceCost();
		
		Dijkstra fastestPathRouter = new Dijkstra(network, freett, freett);
		Dijkstra shortestPathRouer = new Dijkstra(network, disttt, disttt);
		Set<Relation> relations = new HashSet<Relation>();
		WeightedStatistics stats = new WeightedStatistics();
		for(Ego ego : egos.values()) {
			Link homelink = network.getNearestLink(ego.homeloc);
			for(Coord coord : ego.alters) {
				Relation r = new Relation();
				Link alterlink = network.getNearestLink(coord);
				Route fastestRoute = fastestPathRouter.calcLeastCostPath(homelink.getToNode(), alterlink.getFromNode(), 0);
				Route shortesRoute = shortestPathRouer.calcLeastCostPath(homelink.getToNode(), alterlink.getFromNode(), 0);
				
				Double value = grid.getValue(homelink.getCenter());
				if(value == null)
					value = 0.0;
				double pEgo = value / (resolution * resolution);
				
				value = grid.getValue(alterlink.getCenter());
				if(value == null)
					value = 0.0;
				double pAlter = value / (resolution * resolution);
				
				double w = 1 / ((pEgo + pAlter) - (pEgo * pAlter));
				if(Double.isInfinite(w))
						w = 1.0;
				
				r.ttFastesPath = fastestRoute.getTravTime();
				r.distFastestPath = getPathLength(fastestRoute);
				r.ttShortestPath = shortesRoute.getTravTime();
				r.distShortestPath = getPathLength(shortesRoute);
				r.geodesicDistance = coord.calcDistance(ego.homeloc);
				relations.add(r);
				stats.add(r.ttFastesPath, w);
			}
		}
		/*
		 * Dump distances...
		 */
		System.out.println("Dumping results...");
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputfile);
			writer.write("geodesic_dist\ttt_fastest\tdist_fastest\ttt_shortest\tdist_shortest");
			writer.newLine();
			for(Relation r : relations) {
				writer.write(String.valueOf(r.geodesicDistance));
				writer.write("\t");
				writer.write(String.valueOf(r.ttFastesPath));
				writer.write("\t");
				writer.write(String.valueOf(r.distFastestPath));
				writer.write("\t");
				writer.write(String.valueOf(r.ttShortestPath));
				writer.write("\t");
				writer.write(String.valueOf(r.distShortestPath));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedWriter aWriter = IOUtils.getBufferedWriter(String.format("%1$s.absolute.txt", outputfile));
			BufferedWriter nWriter = IOUtils.getBufferedWriter(String.format("%1$s.normalized.txt", outputfile));
			
			aWriter.write("bin\tcount");
			aWriter.newLine();
			
			nWriter.write("bin\tpercentage");
			nWriter.newLine();
			
			TDoubleDoubleHashMap aDistr = stats.absoluteDistribution(60);
			TDoubleDoubleHashMap nDistr = stats.normalizedDistribution();
			double[] keys = aDistr.keys();
			Arrays.sort(keys);
			
			for(double key : keys) {
				aWriter.write(String.format(Locale.US, "%1.14f", key));
				aWriter.write("\t");
				aWriter.write(String.format(Locale.US, "%1.14f", aDistr.get(key)));
				aWriter.newLine();
				
				nWriter.write(String.format(Locale.US, "%1.14f", key));
				nWriter.write("\t");
				nWriter.write(String.format(Locale.US, "%1.14f", nDistr.get(key)));
				nWriter.newLine();
			}
			
			aWriter.close();
			nWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
	
	private static double getPathLength(Route route) {
		double sum = 0;
		for(Link link : route.getLinkRoute())
			sum += link.getLength();
		return sum;
	}
	
	
	private static class Ego {
		
		String id;
		
		Coord homeloc;
		
		Set<Coord> alters = new HashSet<Coord>();
		
	}
	
	private static class Relation {
		
		double geodesicDistance;
		
		double distFastestPath;
		
		double ttFastesPath;
		
		double distShortestPath;
		
		double ttShortestPath;
	}

	private static class TravelDistanceCost implements TravelTime, TravelCost {

		public double getLinkTravelTime(Link link, double time) {
			return link.getLength() / link.getFreespeed(time);
		}

		public double getLinkTravelCost(Link link, double time) {
			return link.getLength();
		}
		
	}
}
