/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceProbabilityCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.zones.ZoneMapping;

/*
 * Use fixed probabilities over the day which are read from a text file.
 */
public class LocationChoiceProbabilityCreator {

	private static final Logger log = Logger.getLogger(LocationChoiceProbabilityCreator.class);
	
	private String locationChoiceFile = "../../matsim/mysimulations/telaviv/locationchoice/destination_choice_prob.txt";
	
	private ZoneMapping zoneMapping;
	
	private Map<Integer, Integer> zoneToMatrixMapping;	// <TAZ, Index in the probability matrix>
	private Map<Integer, Integer> matrixToZoneMapping;	// <Index in the probability matrix, TAZ>
	private double[][] probabilityMatrix;	// from Zone Id (NOT TAZ), to Zone (NOT TAZ), Probability
	
	private Map<Integer, Double>[] fromZoneProbabilities;	// <toZoneId, Probability>[fromZoneId from zoneToMatrixMapping]
	
	public static void main(String[] args) {
		Map<Integer, Double> probabs = new LocationChoiceProbabilityCreator(new ScenarioImpl()).getFromZoneProbabilities(1525);
		
		double sum = 0.0;
		for (double value : probabs.values()) {
			sum = sum + value;
		}
		log.info("Sum Probabilities = " + sum);
	}
	
	public LocationChoiceProbabilityCreator(Scenario scenario) {		
		log.info("Creating zone mapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");
		
		log.info("Creating zone to probability matrix mapping...");
		createZoneToMatrixMapping();
		log.info("done.");
		
		log.info("Creating probability maps<toZone, probability>...");
		createProbabilityMaps();
		log.info("done.");
	}
	
	private void createZoneToMatrixMapping() {
		List<Integer> TAZs = new ArrayList<Integer>(); 
		
		for (Integer TAZ : zoneMapping.getParsedZones().keySet()) {
			TAZs.add(TAZ);
		}
		
		Collections.sort(TAZs);
		
		zoneToMatrixMapping = new HashMap<Integer, Integer>();			
		matrixToZoneMapping = new HashMap<Integer, Integer>();
		
		int id = 0;
		for (int TAZ : TAZs) {
			zoneToMatrixMapping.put(TAZ, id);
			matrixToZoneMapping.put(id, TAZ);
			id++;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createProbabilityMaps() {	
		int zoneCount = zoneToMatrixMapping.size();
		probabilityMatrix = new double[zoneCount][zoneCount];
		
		List<LocationChoiceProbability> probabilities = new LocationChoiceFileParser(locationChoiceFile).readFile();
				
		for (LocationChoiceProbability probability : probabilities) {
			int fromZone = probability.fromZone;
			int toZone = probability.toZone;
			
			probabilityMatrix[zoneToMatrixMapping.get(fromZone)][zoneToMatrixMapping.get(toZone)] = probability.probability;
		}
		
		fromZoneProbabilities = new Map[probabilityMatrix.length];
		for (int fromZoneId = 0; fromZoneId < probabilityMatrix.length; fromZoneId++) {
			double[] fromZoneProbab = probabilityMatrix[fromZoneId];
			
			Map<Integer, Double> map = new TreeMap<Integer, Double>();
			
			for (int index = 0; index < fromZoneProbab.length; index++) {
				map.put(matrixToZoneMapping.get(index), fromZoneProbab[index]);
			}
			
			fromZoneProbabilities[fromZoneId] = map;
		}
	}
	
	/**
	 * @return Map<toZone TAZ, Probability of that Zone>
	 */
	public Map<Integer, Double> getFromZoneProbabilities(int fromZoneTAZ) {	
		return fromZoneProbabilities[zoneToMatrixMapping.get(fromZoneTAZ)];
	}
}