/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityChoiceSetGenerator.java
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
package playground.johannes.studies.coopsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author illenberger
 *
 */
public class FacilityChoiceSetGenerator {

	private final Logger logger = Logger.getLogger(FacilityChoiceSetGenerator.class);
	
	private final DistanceCalculator calculator;
	
	private final int numChoices;
	
	private final double gamma;
	
	private final Random random;
	
	public FacilityChoiceSetGenerator(double gamma, int numChoices, Random random) {
		this(gamma, numChoices, random, OrthodromicDistanceCalculator.getInstance());
	}
	
	public FacilityChoiceSetGenerator(double gamma, int numChoices, Random random, DistanceCalculator calculator) {
		this.gamma = gamma;
		this.numChoices = numChoices;
		this.random = random;
		this.calculator = calculator;
	}
	
	public Map<SocialVertex, List<Id<ActivityFacility>>> generate(SocialGraph graph, ActivityFacilities facilities, String type) {
		logger.info("Generating facility choice set...");
		
		List<ActivityFacility> facList = filterFacilities(facilities, type);
		logger.info(String.format("%1$s facilities of type %2$s.", facList.size(), type));
		Map<SocialVertex, List<Id<ActivityFacility>>> choiceSets = new HashMap<>(graph.getVertices().size());
		
		ProgressLogger.init(graph.getVertices().size(), 1, 5);
		for(SocialVertex ego : graph.getVertices()) {
			/*
			 * calculate normalization
			 */
			double norm = 0;
			for(ActivityFacility facility : facList) {
				double d = calculator.distance(ego.getPoint(), MatsimCoordUtils.coordToPoint(facility.getCoord()));
				norm += Math.pow(d, gamma);
			}
			/*
			 * draw facilities
			 */
			List<Id<ActivityFacility>> choiceSet = new ArrayList<>((int) (numChoices * 1.5));
			for(ActivityFacility facility : facList) {
				double d = calculator.distance(ego.getPoint(), MatsimCoordUtils.coordToPoint(facility.getCoord()));
				double p = numChoices/norm * Math.pow(d, gamma);
				if(random.nextDouble() < p)
					choiceSet.add(facility.getId());
			}
			/*
			 * each ego needs at least one facility
			 */
			if(choiceSet.isEmpty())
				choiceSet.add(facList.get(random.nextInt(facList.size())).getId());
			
			choiceSets.put(ego, choiceSet);
			
			ProgressLogger.step();
		}
		
		return choiceSets;
	}
	
	private List<ActivityFacility> filterFacilities(ActivityFacilities facilities, String type) {
		List<ActivityFacility> facList = new ArrayList<ActivityFacility>(facilities.getFacilities().size());
		
		for(Entry<Id<ActivityFacility>, ? extends ActivityFacility> entry : facilities.getFacilities().entrySet()) {
			ActivityFacility facility = entry.getValue();
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(type == null || option.getType().equals(type)) {
					facList.add(facility);
					break;
				}
			}
		}
		
		return facList;
	}
	
	public static void write(Map<SocialVertex, List<Id<ActivityFacility>>> choiceSets, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for(Entry<SocialVertex, List<Id<ActivityFacility>>> entry : choiceSets.entrySet()) {
			SocialVertex ego = entry.getKey();
			writer.write(ego.getPerson().getPerson().getId().toString());
			
			List<Id<ActivityFacility>> choiceSet = entry.getValue();
			for(Id<ActivityFacility> id : choiceSet) {
				writer.write("\t");
				writer.write(id.toString());
			}
			writer.newLine();
		}
		
		writer.close();
	}
	
	public static Map<SocialVertex, List<Id<ActivityFacility>>> read(String file, SocialGraph graph) throws IOException {
		Map<String, SocialVertex> idVertexMap = new HashMap<String, SocialVertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices()) {
			idVertexMap.put(v.getPerson().getPerson().getId().toString(), v);
			
		}
		
		Map<SocialVertex, List<Id<ActivityFacility>>> choiceSets = new HashMap<SocialVertex, List<Id<ActivityFacility>>>(graph.getVertices().size());
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			SocialVertex ego = idVertexMap.get(tokens[0]);
			
			List<Id<ActivityFacility>> choiceSet = new ArrayList<>();
			for(int i = 1; i < tokens.length; i++) {
				Id<ActivityFacility> id = Id.create(tokens[i], ActivityFacility.class);
				choiceSet.add(id);
			}
			
			choiceSets.put(ego, choiceSet);
		}
		reader.close();
		return choiceSets;
	}

}
