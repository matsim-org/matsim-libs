/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilder.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLWriter;

/**
 * @author illenberger
 *
 */
public class GraphBuilder {
	
	private Logger logger = Logger.getLogger(GraphBuilder.class);
	
	private CoordinateTransformation transform = new WGS84toCH1903LV03();
	
	private KeyGenerator keyGenerator;
	
	private Population population;
	
	private SampledSocialNetBuilder<Person> builder;
	
	private SampledSocialNet<Person> socialnet;
	
	private Map<Id, SampledEgo<Person>> idVertexMapping;
	
	private Map<String, SampledEgo<Person>> userIdVertexMapping;

	public SampledSocialNet<Person> buildSocialNet(String userDataFile, String snowballDataFile) {
		try {
			CSVReader csvReader = new CSVReader();
			Map<String, String[]> userData = csvReader.readUserData(userDataFile);
			Map<String, Map<String, String>> snowballData = csvReader.readSnowballData(snowballDataFile);
			
			population = new PopulationImpl();
			keyGenerator = new KeyGenerator();
			builder = new SampledSocialNetBuilder<Person>();
			socialnet = builder.createGraph();
			idVertexMapping = new HashMap<Id, SampledEgo<Person>>();
			userIdVertexMapping = new HashMap<String, SampledEgo<Person>>();
			
			buildEgoPersons(userData, snowballData);
			buildAlters(snowballData);
			
			return socialnet;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void buildEgoPersons(Map<String, String[]> userData,
			Map<String, Map<String, String>> snowballData) {
		logger.info("Building egos...");
			
		for (String userId : userData.keySet()) {
			String[] userAttributes = userData.get(userId);
			Map<String, String> egoData = snowballData.get(userId);
			if (egoData != null) {
				/*
				 * required: snowball iteration
				 */
				String[] tokens = userAttributes[2].split(" ");
				if (tokens.length == 2) {
					int num = Integer.parseInt(tokens[1]);
					int iteration = -1;
					if (num < 1000)
						iteration = 0;
					else if (num < 10000)
						iteration = 1;
					else if (num < 100000)
						iteration = 2;

					System.out.println("Num="+num+"; it="+iteration);
					/*
					 * required! last known home location
					 */
					Coord homeLoc = egoHomeLocation(userId, egoData);
					if (homeLoc != null) {
						/*
						 * person id is surname + name
						 */
						Id id = createPersonId(userAttributes[0], userAttributes[1]);
						/*
						 * create a person and add a home activity
						 */
						Person person = createPerson(id, homeLoc);
						/*
						 * add other person attributes...
						 */
						population.addPerson(person);
						SampledEgo<Person> ego = builder.addVertex(socialnet, person, iteration);
						ego.sample(iteration);
						idVertexMapping.put(person.getId(), ego);
						userIdVertexMapping.put(userId, ego);
						
					} else {
						logger.warn(String.format(
											"Missing home location for user %1$s. Dropping user!",
											userId));
					}
				} else {
					logger.warn("Cannot determin snwoball iteration. Droppging user!");
				}
			} else {
				logger.warn(String.format(
						"No snowball data found for user %1$s!", userId));
			}
		}
		
		logger.info(String.format("Built %1$s egos, dropped %2$s egos.", socialnet.getVertices().size(), userData.size()));
	}
	
	private Coord egoHomeLocation(String userId, Map<String, String> egoData) {
		SortedMap<Integer, String> homeLocs = new TreeMap<Integer, String>();
		for(int i = KeyGenerator.NUM_HOMELOCS; i > 0; i--) {
			String year = egoData.get(keyGenerator.egoHomeLocationYearKey(i));
			if(year != null) {
				String homeLoc = egoData.get(keyGenerator.egoHomeLocationCoordKey(i));
				if(homeLoc != null)
					homeLocs.put(new Integer(year), homeLoc);
				else
					logger.warn(String.format("Missing home location string. userId = %1$s, entry = %2$s", userId, i));
			}
		}
		
		if(homeLocs.isEmpty())
			return null;
		else
			return decodeCoordinate(homeLocs.get(homeLocs.lastKey()));
	}
	
	private void buildAlters(Map<String, Map<String, String>> snowballData) {
		logger.info("Building alters...");
		int valid = 0;
		int invalid = 0;
		
		for (String userId : snowballData.keySet()) {
			SampledEgo<Person> ego = userIdVertexMapping.get(userId);
			if (ego != null) {
				Map<String, String> egoData = snowballData.get(userId);

				for (int i = KeyGenerator.NUM_ALTERS_1; i > 0; i--) {
					/*
					 * get the alter's name
					 */
					String name = egoData.get(keyGenerator.alter1Key(i));
					if (name != null && name.length() > 0) {
						name = cleanName(name);
						if (buildAlter(KeyGenerator.ALTER_1_KEY, name, userId,
								ego, i, egoData))
							valid++;
						else
							invalid++;
					}
				}

				for (int i = KeyGenerator.NUM_ALTERS_2; i > 0; i--) {
					/*
					 * get the alter's name
					 */
					String name = egoData.get(keyGenerator.alter2Key(i));
					if (name != null && name.length() > 0) {
						name = cleanName(name);
						if (buildAlter(KeyGenerator.ALTER_2_KEY, name, userId,
								ego, i, egoData))
							valid++;
						else
							invalid++;
					}
				}
			}
		}

		logger.info(String.format("Built %1$s alter, dropped %2$s.", valid,
				invalid));
	}
	
	private boolean buildAlter(String alterKey, String name, String userId, SampledEgo<Person> ego, int counter, Map<String, String> egoData) {
		Id alterId = new IdImpl(name);
		/*
		 * check if we already sampled this vertex
		 */
		SampledEgo<Person> alter = idVertexMapping.get(alterId);
		
		if (alter == null) {
			/*
			 * get the coordinate string
			 */
			String coordStr = egoData.get(keyGenerator.alterLocationCoordKey(alterKey, counter));
			
			if(coordStr != null) {
				/*
				 * decode the coordinate string
				 */
				Coord homeLoc = decodeCoordinate(coordStr);
				
				if(homeLoc != null) {
					/*
					 * create a person and a vertex
					 */
					Person person = createPerson(alterId, homeLoc);
					population.addPerson(person);
					alter = builder.addVertex(socialnet, person, ego.getIterationSampled());
					idVertexMapping.put(person.getId(), alter);
					userIdVertexMapping.put(userId, alter);
					alter.detect(ego.getIterationSampled());
					alter.setRecruitedBy(ego);
				}
			} else {
				logger.warn(String.format("Missing home location for alter %1$s of user %2$s. Dropping alter!", counter, userId));
			}
		}
		
		if(alter != null) {
			builder.addEdge(socialnet, ego, alter);
			alter.detect(Math.min(ego.getIterationSampled(), alter.getIterationDetected()));
			return true;
		} else
			return false;
	
	}
	
	private Id createPersonId(String surname, String name) {
		StringBuilder builder = new StringBuilder(surname.length() + name.length());
		builder.append(surname);
		builder.append(" ");
		/*
		 * reduce Doppelnamen
		 */
		name = cleanName(name);
		
		builder.append(name);
		return new IdImpl(builder.toString());
	}
	
	private String cleanName(String name) {
//		int idx = name.indexOf("-"); 
//		if(idx > -1) {
//			name = name.substring(0, idx);
//		}
//		TODO: Need to think about that...
		return name;
	}
	
	private Person createPerson(Id id, Coord coord) {
		Person person = population.getFactory().createPerson(id);
		Plan plan = population.getFactory().createPlan();
		Activity activity = population.getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(activity);
		person.addPlan(plan);

		return person;
	}
	
	private Coord decodeCoordinate(String coordString) {
		String[] tokens = coordString.split("@");
		if(tokens.length >= 2) {
			String latitude = tokens[0];
			String longitude = tokens[1];
//			return new CoordImpl(Double.parseDouble(longitude), Double.parseDouble(latitude));
			return transform.transform(new CoordImpl(Double.parseDouble(longitude), Double.parseDouble(latitude)));
		} else {
			logger.warn("Invalid coordinate string!");
			return null;
		}
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		GraphBuilder builder = new GraphBuilder();
		SampledSocialNet<Person> socialnet = builder.buildSocialNet(args[0], args[1]);
		System.out.println(SampledGraphStatistics.degreeDistribution(socialnet).mean());
		System.out.println(SampledGraphStatistics.localClusteringDistribution(socialnet).mean());
		
//		SpatialGraphMLWriter writer2 = new SpatialGraphMLWriter();
//		writer2.write(socialnet, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/graph.graphml");
		
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(socialnet, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/graph.graphml");
//		Distribution.writeHistogram(SampledGraphStatistics.degreeDistribution(socialnet).absoluteDistribution(), "/Users/fearonni/Desktop/degree.hist.txt");
//		Distribution.writeHistogram(SampledGraphStatistics.edgeLenghtDistribution(socialnet).absoluteDistributionLog2(1000), "/Users/fearonni/Desktop/edgelength.hist.txt");
////		
//		Population2SpatialGraph pop2graph = new Population2SpatialGraph();
//		SpatialGraph g2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/plans/plans.10.xml");
//		Set<Coord> coords = new HashSet<Coord>();
//		for(SpatialVertex v : g2.getVertices()) {
//			coords.add(v.getCoordinate());
//		}
//		Distribution edgelength = SampledGraphStatistics.normalizedEdgeLengthDistribution(socialnet, g2, 1000);
//		Distribution.writeHistogram(edgelength.absoluteDistributionLog2(1000), "/Users/fearonni/Desktop/edgelength.norm.hist.txt");
//		
//		SpatialGrid<Double> grid = SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml");
//		TDoubleDoubleHashMap distr = SpatialGraphStatistics.degreeDensityCorrelation((Collection<? extends SpatialVertex>) SnowballPartitions.createSampledPartition(socialnet), grid);
//		Distribution.writeHistogram(distr, "/Users/fearonni/Desktop/k_rho.txt");
//		
//		Set<? extends SpatialVertex> set = (Set<? extends SpatialVertex>)SnowballPartitions.createSampledPartition(socialnet);
//		TObjectDoubleHashMap<? extends SpatialVertex> meanEdgeLengthDistr = SpatialGraphStatistics.meanEdgeLength(set);
//		Distribution.writeHistogram(SpatialGraphStatistics.densityCorrelation(meanEdgeLengthDistr, grid, 1000), "/Users/fearonni/Desktop/meanedgelength.txt");
////		
////		Distribution gt2000 = new Distribution();
////		Distribution lt2000 = new Distribution();
//		Set<? extends SampledVertex> sampled = SnowballPartitions.createSampledPartition(socialnet);
//		Set<SpatialVertex> gt2000 = new HashSet<SpatialVertex>();
//		Set<SpatialVertex> lt2000 = new HashSet<SpatialVertex>();
//		for(SampledVertex v : sampled) {
//			Double rho = grid.getValue(((SpatialVertex)v).getCoordinate());
//			if(rho != null) {
//			if(rho > 2000)
//				gt2000.add((SpatialVertex) v);
//			else
//				lt2000.add((SpatialVertex) v);
//			}
//		}
//		Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution(gt2000).absoluteDistribution(1000), "/Users/fearonni/Desktop/edgelength.gt2000.txt");
//		Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution(lt2000).absoluteDistribution(1000), "/Users/fearonni/Desktop/edgelength.lt2000.txt");
		
////		KMLEgoNetWriter writer = new KMLEgoNetWriter();
//		KMLWriter writer = new KMLWriter();
//		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
//		writer.setDrawEdges(true);
////		writer.setVertexStyle(new KMLSnowballVertexStyle(writer.getVertexIconLink()));
//		writer.setVertexStyle(new KMLDegreeStyle(writer.getVertexIconLink()));
////		writer.setVertexDescriptor(new KMLSnowballDescriptor());
//		writer.setVertexDescriptor(new KMLVertexDescriptor(socialnet));
////		writer.write(socialnet, (Set) SnowballPartitions.createSampledPartition(socialnet, 0), 10, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/egonets.kmz");
//		writer.write(socialnet, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/egonets.kmz");
//		
//		SpatialPajekWriter pwriter = new SpatialPajekWriter();
//		pwriter.write(socialnet, "/Users/fearonni/Desktop/egonet.net");
	}
}
