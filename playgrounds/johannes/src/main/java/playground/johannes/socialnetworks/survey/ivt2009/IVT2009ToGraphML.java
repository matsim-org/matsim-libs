/* *********************************************************************** *
 * project: org.matsim.*
 * IVT2009ToGraphML.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.survey.ivt2009;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.johannes.socialnetworks.graph.GraphAnalyser;
import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.io.KMLObjectStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLWriter;
import playground.johannes.socialnetworks.ivtsurveys.SNKMLEgoAlterSytle;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class IVT2009ToGraphML {

	private static final Logger logger = Logger.getLogger(IVT2009ToGraphML.class);
	
	private static final CoordinateTransformation transform = new WGS84toCH1903LV03();
	
	private static final String SEMICOLON = ";";
	
	private static final String EMPTY = "";
	
	private static final int USER_ID_COL = 3;
	
	private static final int PARAM_COL = 1;
	
	private static final int VALUE_COL = 2;
	
	private static final String AGE_PARAM_KEY = "84967X53X106";
	
	private static final String HOME_LOC_KEY = "84967X53X128Loc";
	
	private static final String HOME_LOC_YEAR_KEY = "84967X53X128von";
	
	private static final String ALTER_1_KEY = "84967X55X143A";
	
	private static final String ALTER_2_KEY = "84967X55X144B";
	
	private static final String ALTER_AGE_KEY = "84967X54X137";
	
	private static final String ALTER_LOC_KEY = "84967X54X338Loc";
	
	private static final int NUM_HOMELOCS = 20;
	
	private static final int NUM_ALTERS_1 = 29;
	
	private static final int NUM_ALTERS_2 = 11;
	
	public static void main(String[] args) throws IOException {
		String snowballfile = args[0];
		String output = args[1];
//		String popfilename = args[2];
		/*
		 * load raw data
		 */
		Map<String, Map<String, String>> egos = readTable(snowballfile);
		/*
		 * create an empty population and social network
		 */
		Population population = new PopulationImpl();
		SocialNetwork<Person> socialnet = new SocialNetwork<Person>();
		SocialNetworkBuilder<Person> builder = new SocialNetworkBuilder<Person>();
		
		Set<Ego<Person>> alters = new HashSet<Ego<Person>>();
		Set egoSet = new HashSet<Ego<Person>>();
		/*
		 * go through all user ids
		 */
		int numEgos = 0;
		int numAlters = 0;
		int numEgosInvalid = 0;
		int numAltersInvalid = 0;
		
		for(String id : egos.keySet()) {
			Map<String, String> egoData = egos.get(id);
			/*
			 * age
			 */
			String age = getValue(egoData, AGE_PARAM_KEY);
			if(age == null)
				logger.warn(String.format("Missing age for userId %1$s. Ignoring.", id));
			/*
			 * get the current (last) home location
			 */
			SortedMap<Integer, String> homeLocs = new TreeMap<Integer, String>();
			for(int i = NUM_HOMELOCS; i > 0; i--) {
				String year = getValue(egoData, HOME_LOC_YEAR_KEY + i);
				if(year != null) {
					String homeLoc = getValue(egoData, HOME_LOC_KEY + i);
					if(homeLoc != null)
						homeLocs.put(new Integer(year), homeLoc);
					else
						logger.warn(String.format("Missing home location string. userId = %1$s, entry = %2$s", id, i));
				}
			}
			if(homeLocs.isEmpty()) {
				logger.warn(String.format(
										"Missing home location for userId %1$s. Dropping person.",
										id));
				numEgosInvalid++;
			} else {
				Coord coord = decodeCoordinate(homeLocs.get(homeLocs.lastKey()));
				if (coord == null) {
					logger.warn(String.format("Cannot decode location string. userId = %1$s", id));
					numEgosInvalid++;
				} else {
					/*
					 * create a person and an ego
					 */
					Person egoPerson = createPerson(id, coord, age, population);
					population.addPerson(egoPerson);
					Ego<Person> ego = builder.addVertex(socialnet, egoPerson);
					numEgos++;
					egoSet.add(ego);
					/*
					 * create the alters
					 */
					for (int i = NUM_ALTERS_1; i > 0; i--) {
						Person alterPerson = createAlter(ALTER_1_KEY, i, egoData, id, population);
						if(alterPerson != null) {
							population.addPerson(alterPerson);
							Ego<Person> alter = builder.addVertex(socialnet, alterPerson);
							builder.addEdge(socialnet, ego, alter);
							numAlters++;
							alters.add(alter);
						}// else
//							numAltersInvalid++;
					}

					for (int i = NUM_ALTERS_2; i > 0; i--) {
						Person alterPerson = createAlter(ALTER_2_KEY, i, egoData, id, population);
						if(alterPerson != null) {
							population.addPerson(alterPerson);
							Ego<Person> alter = builder.addVertex(socialnet, alterPerson);
							builder.addEdge(socialnet, ego, alter);
							numAlters++;
							alters.add(alter);
						}// else
//							numAltersInvalid++;
					}
				}
			}
		}
		
		logger.info(String.format("Loaded %1$s egos and %2$s alters. %3$s egos and %4$s alters dropped", numEgos, numAlters, numEgosInvalid, numAltersInvalid));
		/*
		 * Write population and social network...
		 */
//		logger.info("Writing population to " + output+"ivt2009.plans.xml");
//		new PopulationWriter(population).writeFile(output+"ivt2009.plans.xml");
		
		logger.info("Writing social network to " + output + "ivt2009.graphml");
//		SNGraphMLWriter graphWriter = new SNGraphMLWriter();
//		graphWriter.write(socialnet, output + "ivt2009.graphml");
		SpatialGraphMLWriter graphWriter = new SpatialGraphMLWriter();
		graphWriter.write(socialnet, output + "ivt2009.graphml");
		
		KMLWriter kmlwriter = new KMLWriter();
		KMLObjectStyle<SocialNetwork<Person>, Ego<Person>> vertexStyle = new SNKMLEgoAlterSytle<Person>(egoSet, kmlwriter.getVertexIconLink());
		kmlwriter.setVertexStyle(vertexStyle);
		kmlwriter.setCoordinateTransformation(new CH1903LV03toWGS84());
		kmlwriter.write(socialnet, output+"socialnet.kmz");
		/*
		 * statistics
		 */
		/*
		 * degree
		 */
		Distribution degreeStats = GraphStatistics.degreeDistribution(egoSet);
		double meanDegree = degreeStats.mean();
		logger.info(String.format("Mean degree is %1$s.", meanDegree));
		if(output != null)
			Distribution.writeHistogram(degreeStats.absoluteDistribution(), output + "degree.hist.txt");
		/*
		 * edge length distribution
		 */
		Distribution edgeLengthDistr = SpatialGraphStatistics.edgeLengthDistribution(egoSet);
		double d_mean = edgeLengthDistr.mean();
		logger.info("Mean edge length is " + d_mean);
	
//		Distribution edgeLengthDistrNorm = SpatialGraphStatistics.normalizedEdgeLengthDistribution(egoSet, 1000);
//		double d_mean_norm = edgeLengthDistrNorm.mean();
//		logger.info("Normalized mean edge length is " + d_mean_norm);
		
		SpatialGrid<Double> densityGrid = null;
		if(args[2] != null)
			densityGrid = SpatialGrid.readFromFile(args[2]);

		if(output != null) {
			Distribution.writeHistogram(edgeLengthDistr.absoluteDistribution(1000), output + "edgelength.hist.txt");
//			Distribution.writeHistogram(edgeLengthDistrNorm.absoluteDistribution(1000), output + "edgelength.norm.hist.txt");
			Correlations.writeToFile(SpatialGraphStatistics.edgeLengthDegreeCorrelation(egoSet), output + "edgelength_k.txt", "k", "edge length");
			
			if(densityGrid != null) {
				Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(egoSet, densityGrid), output + "k_rho.txt", "density", "k");
				Correlations.writeToFile(SpatialGraphStatistics.clusteringDensityCorrelation(egoSet, densityGrid), output + "c_rho.txt", "density", "c");
			}
		}
		
		if (output != null) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output + GraphAnalyser.SUMMARY_FILE, true));
			writer.write("meanEdgeLength=");
			writer.write(Double.toString(d_mean));
			writer.newLine();
			writer.write("meanDegree=");
			writer.write(Double.toString(meanDegree));
			writer.newLine();
			writer.close();
		}
		
		
		

	}

	private static Map<String, Map<String, String>> readTable(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		
		Map<String, Map<String, String>> egos = new HashMap<String, Map<String,String>>();
		
		String line;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(SEMICOLON);
			String userId = trimString(tokens[USER_ID_COL]);
			if (!EMPTY.equalsIgnoreCase(userId)) {

				Map<String, String> egoData = egos.get(userId);
				if (egoData == null) {
					egoData = new HashMap<String, String>();
					egos.put(userId, egoData);
				}

				String param = trimString(tokens[PARAM_COL]);
				if (!EMPTY.equalsIgnoreCase(param)) {
					String value = trimString(tokens[VALUE_COL]);

					if(!EMPTY.equalsIgnoreCase(value)) {
						String oldValue = egoData.put(param, value);
						if (oldValue != null)
							logger.warn(String.format(
												"Overwriting value for parameter %1$s (%2$s -> %3$s)! UserId = %4$s",
												param, oldValue, value, userId));
					}
				}
			}
		}
		
		return egos;
	}
	
	private static String trimString(String str) {
		if(str.length() > 1)
			return str.substring(1, str.length() - 1).trim();
		return str;
	}
	
	private static Person createPerson(String id, Coord coord, String age, Population population) {
		PersonImpl person = new PersonImpl(new IdImpl(id));
		if(age != null)
				person.setAge(Integer.parseInt(age));
		Plan plan = new PlanImpl(person);
		if(coord == null)
			throw new NullPointerException("Null coordinates are not allowed.");
//		BasicActivityImpl act = new BasicActivityImpl("home");
//		act.setCoord(coord);
		Activity act = population.getFactory().createActivityFromCoord("home", coord);
		
		plan.addActivity(act);
		person.addPlan(plan);
		
		return person;
	}
	
	private static Person createAlter(String alterKey, int counter, Map<String, String> egoData, String egoId, Population population) {
		String name = getValue(egoData, alterKey+counter);
		System.out.println(name);
		String alterAge = getValue(egoData, makeKey(alterKey, ALTER_AGE_KEY, counter));
		if(alterAge == null) {
			logger.warn(String.format("Missing age for alter %1$s of userId %2$s. Ignoring.", counter, egoId));
			return null;
		}
		
		String alterCoord = getValue(egoData, makeKey(alterKey, ALTER_LOC_KEY, counter));
		if(alterCoord == null) {
//			logger.warn(String.format(
//					"Missing home location for alter %1$s of userId %2$s. Dropping alter.",
//					counter, egoId));
			return null;
		} else {
			Coord coord = decodeCoordinate(alterCoord);
			if(coord == null) {
				logger.warn(String.format("Cannot decode location string. userId = %1$s, alter = %2$s", egoId, counter));
				return null;
			} else
				return createPerson(String.format("%1$s.%2$s", egoId, counter), coord, alterAge, population);
		}
		
	}
	
	private static Coord decodeCoordinate(String coordString) {
		String[] tokens = coordString.split("@");
		if(tokens.length == 7) {
			String latitude = tokens[0];
			String longitude = tokens[1];
			return transform.transform(new CoordImpl(Double.parseDouble(longitude), Double.parseDouble(latitude)));
		} else {
			logger.warn("Invalid coordinate string!");
			return null;
		}
	}
	
	private static String getValue(Map<String, String> egoData, String key) {
		String value = egoData.get(key);
		if((value != null) && EMPTY.equalsIgnoreCase(value.trim()))
			return null;
		return value;
	}
	
	private static String makeKey(String key1, String key2, int counter) {
		StringBuffer buffer = new StringBuffer(key1.length() + key2.length() + 3);
		buffer.append(key1);
		buffer.append(Integer.toString(counter));
		buffer.append("_");
		buffer.append(key2);
		return buffer.toString();
	}
}
