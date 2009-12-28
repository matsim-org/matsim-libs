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
package playground.johannes.socialnetworks.ivtsurveys;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class TravelTimeHistogram {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String networkfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/ivtch-osm.xml";
		String configfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/config.socialNetGenerator.xml";
		String egofile = "/Users/fearonni/vsp-work/socialnets/data-analysis/egos_wgs84.txt";
		String alterfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/alters_wgs84.txt";
		String outputfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/traveltimes.txt";
		String gridfile = "/Users/fearonni/vsp-work/socialnets/data-analysis/popdensity.1000.xml";
		/*
		 * Load network...
		 */
		System.out.println("Loading network...");
//		NetworkLayer network = new NetworkLayer();
//		new MatsimNetworkReader(network).readFile(networkfile);
		Config config = Gbl.createConfig(new String[]{configfile});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		ScenarioImpl data = loader.getScenario();
		PopulationImpl population = data.getPopulation();
		NetworkLayer network = (NetworkLayer) data.getNetwork();
		/*
		 * Make grid...
		 */
//		
		SpatialGrid<Double> grid = SpatialGrid.readFromFile(gridfile);		
		double maxX = grid.getXmax();
		double maxY = grid.getYmax();
		double minX = grid.getXmin();
		double minY = grid.getYmin();
		double resolution = grid.getResolution();
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
				if((ego.homeloc.getX() >= minX) && (ego.homeloc.getX() <= maxX) &&
						(ego.homeloc.getY() >= minY) && (ego.homeloc.getY() <= maxY))
					egos.put(ego.id, ego);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * Load alters...
		 */
		BufferedWriter writer3 = IOUtils.getBufferedWriter("/Users/fearonni/vsp-work/socialnets/data-analysis/altercoords.txt");
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
					coord = transform.transform(coord);
					writer3.write(String.valueOf(coord.getX() + Math.random()*100 - 50));
					writer3.write("\t");
					writer3.write(String.valueOf(coord.getY() + Math.random()*100 - 50));
					writer3.newLine();
					if((coord.getX() >= minX) && (coord.getX() <= maxX) &&
							(coord.getY() >= minY) && (coord.getY() <= maxY))
						ego.alters.add(coord);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer3.close();
		
		BufferedWriter writer2 = IOUtils.getBufferedWriter("/Users/fearonni/vsp-work/socialnets/data-analysis/egocoors.txt");
		for(Ego ego : egos.values()) {
			writer2.write(String.valueOf(ego.homeloc.getX() + Math.random()*100 - 50));
			writer2.write("\t");
			writer2.write(String.valueOf(ego.homeloc.getY() + Math.random()*100 - 50));
			writer2.write("\t");
			writer2.write(String.valueOf(ego.alters.size()));
			writer2.newLine();
		}
		writer2.close();
		/*
		 * Load dist distribution
		 */
//		BufferedReader reader = IOUtils.getBufferedReader("/Volumes/math-work/socialnets/distanceDistribution2.txt");
//		String line;
//		TDoubleDoubleHashMap distDistr = new TDoubleDoubleHashMap();
//		reader.readLine();
//		while((line = reader.readLine()) != null) {
//			String[] tokens = line.split("\t");
//			double bin = Double.parseDouble(tokens[0]);
//			double count = Double.parseDouble(tokens[1]);
//			distDistr.put(bin, count);
//		}
		HashMap<Ego, TDoubleDoubleHashMap> egoHist = new HashMap<Ego, TDoubleDoubleHashMap>();
		double binsize = 1000;
		int count = 0;
		for(Ego ego : egos.values()) {
			TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
			for(Person p2 : population.getPersons().values()) {
				Coord c1 = ego.homeloc;
				Coord c2 = ((PlanImpl) p2.getSelectedPlan()).getFirstActivity().getCoord();
				double d = CoordUtils.calcDistance(c1, c2);
				double bin = Math.floor(d/binsize);
				double val = hist.get(bin);
				val++;
				hist.put(bin, val);
			}
			egoHist.put(ego, hist);
			Distribution.writeHistogram(hist, "/Users/fearonni/vsp-work/socialnets/data-analysis/egohists/" + count + ".txt");
			count++;
			if(count % 10 == 0) {
				System.out.println(String.format(
						"Processed %1$s of %2$s persons. (%3$s )", count,
						egos.size(), count / (double) egos.size()));
			}
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
		Distribution stats = new Distribution();
		Distribution stats2 = new Distribution();
		int egocount = 0;
		for(Ego ego : egos.values()) {
			LinkImpl homelink = network.getNearestLink(ego.homeloc);
			TDoubleDoubleHashMap hist = egoHist.get(ego);
			
//			double pEgo = getPersons(ego.homeloc, 250, population);
			for(Coord coord : ego.alters) {
				Relation r = new Relation();
				LinkImpl alterlink = network.getNearestLink(coord);
				Path fastestRoute = fastestPathRouter.calcLeastCostPath(homelink.getToNode(), alterlink.getFromNode(), 0);
				Path shortesRoute = shortestPathRouer.calcLeastCostPath(homelink.getToNode(), alterlink.getFromNode(), 0);
				
//				double sumOpportunities = 0;
//				int sumCells = 0;
//				double d = coord.calcDistance(ego.homeloc);
//				boolean[][] selected = new boolean[grid.getNumRows()+1][grid.getNumCols(0)+2];
//				for (int alpha = 0; alpha < 360; alpha++) {
//					double x = Math.sin(alpha) * d + ego.homeloc.getX();
//					double y = Math.cos(alpha) * d + ego.homeloc.getY();
//					int row = grid.getRow(x);
//					int col = grid.getColumn(y);
//					if (row >= 0 && row < selected.length && col >= 0 && col < selected[row].length) {
//						if (selected[row][col] == false) {
//							Double v = grid.getValue(new CoordImpl(x, y));
//							if (v == null)
//								v = 0.0;
//							sumOpportunities += v;
//							sumCells++;
//							selected[row][col] = true;
//						}
//					}
//				}
//				Double value = grid.getValue(ego.homeloc);
//				if(value == null)
//					value = 0.0;
//				double pEgo = value;
				
				
//				value = grid.getValue(coord);
//				if(value == null)
//					value = 0.0;
//				double pAlter = value;// / (resolution * resolution);
//				double pAlter = sumOpportunities / (double)sumCells;
//				double pAlter = getPersons(coord, 250, population);
				
//				double w = 1 / ((pEgo + pAlter) - (pEgo * pAlter));
//				double w = 1 / (pEgo * pAlter);
//				double w = 1 / pAlter;
				double bin = Math.floor(CoordUtils.calcDistance(coord, ego.homeloc)/1000.0);
				double w = 1/hist.get(bin);
//				double w = 1/distDistr.get(bin);
				if(Double.isInfinite(w)) {
						w = 0.0;
						System.err.println("pAlter = 0");
				} else {
				
				r.ttFastesPath = fastestRoute.travelTime;
				r.distFastestPath = getPathLength(fastestRoute);
				r.ttShortestPath = shortesRoute.travelTime;
				r.distShortestPath = getPathLength(shortesRoute);
				r.geodesicDistance = CoordUtils.calcDistance(coord, ego.homeloc);
				if(r.geodesicDistance > 0 ) {
				relations.add(r);
				stats.add(r.ttFastesPath, w);
				stats2.add(r.geodesicDistance, w);
				}
				}
				
			}
			egocount++;
			System.out.println("Processed " +egocount +" egos...");
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
		
		writeHistogram(stats, "/Users/fearonni/vsp-work/socialnets/data-analysis/tt_fastest", 60);
		writeHistogram(stats2, "/Users/fearonni/vsp-work/socialnets/data-analysis/geodist", 1000);
		System.out.println("Done.");
	}
	
	private static void writeHistogram(Distribution stats, String outputfile, int binsize) {
		try {
			BufferedWriter aWriter = IOUtils.getBufferedWriter(String.format("%1$s.absolute.txt", outputfile));
			BufferedWriter nWriter = IOUtils.getBufferedWriter(String.format("%1$s.normalized.txt", outputfile));
			
			aWriter.write("bin\tcount");
			aWriter.newLine();
			
			nWriter.write("bin\tpercentage");
			nWriter.newLine();
			
			TDoubleDoubleHashMap aDistr = stats.absoluteDistribution(binsize);
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
	}
	private static double getPathLength(Path path) {
		double sum = 0;
		for(Link link : path.links)
			sum += link.getLength();
		return sum;
	}
	
	
	public static class Ego {
		
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

	public static class TravelDistanceCost implements TravelTime, TravelCost {

		public double getLinkTravelTime(Link link, double time) {
			return link.getLength() / link.getFreespeed(time);
		}

		public double getLinkTravelCost(Link link, double time) {
			return link.getLength();
		}
		
	}
	
	private static int getPersons(Coord ego, double radius, Population pop) {
		int count = 0;
		for(Person p : pop.getPersons().values()) {
			double r = CoordUtils.calcDistance(ego, ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord());
			if(r <= radius)
				count++;
		}
		return count;
	}
}
