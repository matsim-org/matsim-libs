/* *********************************************************************** *
 * project: org.matsim.*
 * WebdiaryDistCalc.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.ivt;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TIntIntHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.*;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import org.matsim.contrib.socnetgen.socialnetworks.statistics.Correlations;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class WebdiaryDistCalc {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		GeometryFactory factory = new GeometryFactory();
		DistanceCalculator distCalc = new WGS84DistanceCalculator();
		
		TDoubleDoubleHashMap correl = new TDoubleDoubleHashMap();
		TDoubleArrayList xvals = new TDoubleArrayList();
		TDoubleArrayList yvals = new TDoubleArrayList();
		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.
				read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");

		Map<String, SocialSparseVertex> map = new HashMap<String, SocialSparseVertex>();
		for(SocialSampledVertexDecorator<SocialSparseVertex> v : graph.getVertices()) {
			map.put(v.getPerson().getPerson().getId().toString(), v.getDelegate());
		}
		
		List<String> files = new ArrayList<String>();
		files.add("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/raw/alters1.txt");
		files.add("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/raw/alters2.txt");
		TIntIntHashMap idMapping = idMapping(files);
	
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/webdiary.xy.csv"));
		String line = reader.readLine();
		String[] tokens = line.split(";");
		
		int userIdx = getIndex("\"User_ID\"", tokens);
		int typeIdx = getIndex("\"Zweck_ID\"", tokens);
		int latIdx = getIndex("\"lat\"", tokens);
		int longIdx = getIndex("\"long\"", tokens);
		int membersIdx = getIndex("\"Aktivitaet_Andere_Mitglieder\"", tokens); 
		
		int notfound = 0;
		int valid = 0;
		int nomembers = 0;
		while((line = reader.readLine())!= null) {
			tokens = line.split(";");
			
			int type = Integer.parseInt(tokens[typeIdx]);
			if(type == 8) {
//				double lat = Double.parseDouble(tokens[latIdx]);
				double lat = Double.parseDouble(tokens[tokens.length - 2]);
				double lon = Double.parseDouble(tokens[tokens.length - 1]);
				
				int id = Integer.parseInt(tokens[userIdx]);
				if(idMapping.contains(id)) {
				int id2 = idMapping.get(id);
				
				SocialVertex v = map.get(String.valueOf(id2));
				Point p1 = v.getPoint();
				if(p1 != null) {
					Point p2 = factory.createPoint(new Coordinate(lon, lat));
					double d = distCalc.distance(p1, p2);
					
					int members = -1;
					if(!tokens[membersIdx].equals("\"NULL\"")) {
					try {
						members = Integer.parseInt(tokens[membersIdx]);
					} catch (Exception e) {
						e.printStackTrace();
					}
					} else {
						members=0;
						nomembers++;
					}
					
					if(members >= 0 && d < 300000) {
//						xvals.add(v.getNeighbours().size());
//						yvals.add(members);
						xvals.add(members);
						yvals.add(d);
						valid++;
					}
				}
				} else {
//					System.err.println("Ego not found.");
					notfound++;
				}
			}
		}
		System.out.println("Valid samples = " + valid);
		System.err.println("No members = " + nomembers);
		System.err.println("Not found = " + notfound);
		Discretizer disc = new FixedBordersDiscretizer(new double[]{0,1,2,3,4,5,6,7,8,9,10,60});
//		correl = Correlations.mean(xvals.toNativeArray(), yvals.toNativeArray(), FixedSampleSizeDiscretizer.create(xvals.toNativeArray(), 20, 20));
		correl = Correlations.mean(xvals.toNativeArray(), yvals.toNativeArray(), disc);
//		TXTWriter.writeHistogram(correl, "members", "disr", "/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/members_k.txt");
		StatsWriter.writeHistogram(correl, "members", "disr", "/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/d_members.txt");
		
		
		TDoubleObjectHashMap<DescriptiveStatistics> correl2 = Correlations.statistics(xvals.toNativeArray(), yvals.toNativeArray(), disc);
//		TXTWriter.writeBoxplotStats(correl2,"/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/d_members.stats.txt");
		StatsWriter.writeStatistics(correl2, "members", "/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/d_members.stats.txt");
		DescriptiveStatistics stats = new DescriptiveStatistics(xvals.toNativeArray());
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new DummyDiscretizer(), false);
		StatsWriter.writeHistogram(hist, "members", "n", "/Users/jillenberger/Work/socialnets/data/ivt2009/webdiary/members.txt");
		
		DescriptiveStatistics dists = new DescriptiveStatistics(yvals.toNativeArray());
		System.out.println("Mean dist = " + dists.getMean());
 	}

	private static int getIndex(String name, String tokens[]) {
		for(int i = 0; i < tokens.length; i++) {
			if(name.equals(tokens[i]))
				return i;
		}
		
		return -1;
	}
	
	private static TIntIntHashMap idMapping(List<String> files) throws NumberFormatException, IOException {
		TIntIntHashMap map = new TIntIntHashMap();
		for(String file : files) {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				int id1 = Integer.parseInt(tokens[0]);
				
				int idx = tokens[1].indexOf(" ");
				if(idx > 0) {
				int id2 = Integer.parseInt(tokens[1].substring(idx+1));
				map.put(id1, id2);
				}
			}
		}
		
		return map;
	}
}
