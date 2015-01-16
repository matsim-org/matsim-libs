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

package playground.johannes.gsv.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class LeiDis2Trajectory {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String leidisfile = args[0];
		String outfile = args[1];
		final String targetId = "1082M";
		final int defaultTimeRange = 60*20;
		int timeRange = defaultTimeRange;

		BufferedReader reader = new BufferedReader(new FileReader(leidisfile));
		String line;
		/*
		 * go through file and get target trajectory
		 */
		Trajectory targetT = new Trajectory();
//		List<String> nodes = readNodes();
		List<String> nodes = new ArrayList<>(loadCoordinates().keySet());
		
		while ((line = reader.readLine()) != null) {
			String trainId = parseTrainId(line);
			if (trainId.equalsIgnoreCase(targetId)) {
				String nodeId = parseNodeId(line);

				if (nodes.contains(nodeId)) {
					int type = parseType(line);

					// int arr = parsePlanedTime(line);
					int time = parseActualTime(line);

					targetT.id = trainId;
					if (type == 3)
						targetT.arrs.put(nodeId, time);
					else if (type == 4)
						targetT.deps.put(nodeId, time);
					else {
						targetT.arrs.put(nodeId, time);
						targetT.deps.put(nodeId, time);
					}

				}
			}
		}
		reader.close();

		reader = new BufferedReader(new FileReader(leidisfile));
		/*
		 * go through file and get target trajectory
		 */
		Map<String, Trajectory> trajectories = new LinkedHashMap<>();
		trajectories.put(targetId, targetT);
		
		while ((line = reader.readLine()) != null) {

			String trainId = parseTrainId(line);
			String nodeId = parseNodeId(line);

			if (nodes.contains(nodeId)) {
				int type = parseType(line);
				// int arr = parsePlanedTime(line);
				int time = parseActualTime(line);
				/*
				 * get times of target train
				 */
				Integer targetTime;
				if (type == 3) {
					targetTime = targetT.arrs.get(nodeId);
				} else
					targetTime = targetT.deps.get(nodeId);

				if(targetTime == null) {
					targetTime = targetT.deps.get("MP");
					timeRange = 60*60;
				} else {
					timeRange = defaultTimeRange;
				}
				
				if (targetTime != null) {
					int lower = targetTime - timeRange;
					int upper = targetTime + timeRange;
					if (time >= lower && time <= upper) {
						Trajectory t = trajectories.get(trainId);
						if (t == null) {
							t = new Trajectory();
							t.id = trainId;
							trajectories.put(trainId, t);
						}

						if (type == 3)
							t.arrs.put(nodeId, time);
						else if (type == 4)
							t.deps.put(nodeId, time);
						else {
							t.arrs.put(nodeId, time);
							t.deps.put(nodeId, time);
						}
					}
				}

			}

		}
		reader.close();
		/*
		 * remove trains that do not stop in MP and MA
		 */
		Set<String> removeTrains = new HashSet<>();
		for (Trajectory t : trajectories.values()) {
			String start = "MP";//nodes.get(0);
			String end = "MA";//nodes.get(nodes.size() - 1);
			
			boolean remove = false;
			if(!t.arrs.containsKey(start) && !t.deps.containsKey(start)) {
				remove = true;
			}
			if(!t.arrs.containsKey(end) && !t.deps.containsKey(end)) {
				remove = true;
			}
			if(remove) {
				removeTrains.add(t.id);
			}
		}
		for(String id : removeTrains) {
			trajectories.remove(id);
		}
		/*
		 * remove Leerfahrt
		 */
		trajectories.remove("71439M");
		
		Map<String, Point> coords = loadCoordinates();
		/*
		 * write
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
		writer.write("node");
		for (Trajectory t : trajectories.values()) {
			writer.write("\t" + t.id);
		}
		writer.newLine();
		/*
		 * 
		 */
		double dist = 0;
		for (int i = 0; i < nodes.size(); i++) {
			String node = nodes.get(i);
			if(i > 0) {
				Point prev = coords.get(nodes.get(i-1));
				Point next = coords.get(nodes.get(i));
				dist += WGS84DistanceCalculator.getInstance().distance(prev, next);
			}
			writer.write(String.valueOf(dist));

			for (Trajectory t : trajectories.values()) {
				writer.write("\t");
				Integer val = t.arrs.get(node);
				if (val != null) {
					writer.write(String.valueOf(val));
				} else {
					writer.write("");
				}
			}
			writer.newLine();
			writer.write(String.valueOf(dist));
			for (Trajectory t : trajectories.values()) {
				writer.write("\t");
				Integer val = t.deps.get(node);
				if (val != null) {
					writer.write(String.valueOf(val));
				} else {
					writer.write("");
				}
			}
			writer.newLine();
		}
		writer.close();

	}

	private static List<String> readNodes() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/fpd/trajectories/nodes.txt"));
		String line = null;
		List<String> list = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			list.add(line);

		}
		reader.close();
		return list;
		
		
	}

	static private int parseTime(String str) {
		String hh = str.substring(0, 2);
		String mm = str.substring(2, 4);
		String ss = str.substring(4, 6);

		int h = Integer.parseInt(hh);
		int m = Integer.parseInt(mm);
		int s = Integer.parseInt(ss);

		return s + (m * 60) + (h * 60 * 60);
	}

	private static String parseTrainId(String line) {
		String trainId = line.substring(9, 16);
		return trainId.trim();
	}

	private static String parseNodeId(String line) {
		return line.substring(17, 22).trim();
	}

	private static int parsePlanedTime(String line) {
		String str = line.substring(32, 38).trim();
		if (str.isEmpty())
			return 0;
		else
			return parseTime(str);
	}

	private static int parseActualTime(String line) {
		String str = line.substring(46, 53).trim();
		if (str.isEmpty())
			return 0;
		else
			return parseTime(str);
	}

	private static int parseType(String line) {
		return Integer.parseInt(line.substring(23, 24));
	}

	private static class Trajectory {

		private String id;

		private SortedMap<String, Integer> arrs = new TreeMap<>();

		private SortedMap<String, Integer> deps = new TreeMap<>();

	}
	
	private static <T> Map<String, Point> loadCoordinates() throws IOException {
		Map<String, Point> coords = new LinkedHashMap<>();
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/fpd/trajectories/nodes.shp");
		for(SimpleFeature feature : features) {
			String code = (String) feature.getAttribute("CODE");
			Point p = ((Geometry)feature.getDefaultGeometry()).getCentroid();
			coords.put(code, p);
		}
		
		SortedMap<Point, String> sortedCoords = new TreeMap<>(new Comparator<Point>() {

			@Override
			public int compare(Point o1, Point o2) {
				int r = Double.compare(o2.getCoordinate().x, o1.getCoordinate().x);
				if(r == 0) {
					return o2.hashCode() - o1.hashCode();
				} else {
					return r;
				}
			}
		});
		
		for(Entry<String, Point> coord : coords.entrySet()) {
			sortedCoords.put(coord.getValue(), coord.getKey());
		}
		
		coords.clear();
		for(Entry<Point, String> e : sortedCoords.entrySet()) {
			coords.put(e.getValue(), e.getKey());
		}
		
		return coords;
	}
}
