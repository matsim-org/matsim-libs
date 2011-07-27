/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionPerAreaAnalysis.java
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
package playground.benjamin.szenarios.munich.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * @author benjamin
 *
 */
public class EmissionPerAreaAnalysis {
	private static final Logger logger = Logger.getLogger(EmissionPerAreaAnalysis.class);

	private static Scenario scenario;
	private static Population population;
	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";

	public static void main(String[] args) {
		UrbanSuburbanAnalyzer usa = new UrbanSuburbanAnalyzer(scenario);
		Set<Feature> urbanShape = usa.readShape(urbanShapeFile);
		Population urbanPop = usa.getRelevantPopulation(population, urbanShape);
		Set<Feature> suburbanShape = usa.readShape(suburbanShapeFile);
		Population suburbanPop = usa.getRelevantPopulation(population, suburbanShape);
		
//		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2TotalEmissionsInGrammPerType);
//		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2TotalEmissionsInGrammPerType);
		
//		List<Double> emissionType2AvgEmissionsUrbanArea = calculateAvgEmissionsPerTypeAndArea(urbanPop, personId2WarmEmissionsInGrammPerType);
//		List<Double> emissionType2AvgEmissionsSuburbanArea = calculateAvgEmissionsPerTypeAndArea(suburbanPop, personId2WarmEmissionsInGrammPerType);

//		System.out.println("urbanArea: " + emissionType2AvgEmissionsUrbanArea);
//		System.out.println("suburbanArea: " + emissionType2AvgEmissionsSuburbanArea);

	}
	private List<Double> calculateAvgEmissionsPerTypeAndArea(Population population, Map<Id, double[]> personId2emissionsInGrammPerType) {
		List<Double> avgEmissionsPerTypeandArea = new ArrayList<Double>();
		double totalFc = 0.0;
		double totalNox = 0.0;
		double totalCo2 = 0.0;
		double totalNo2 = 0.0;
		double totalPM = 0.0;

		Integer populationSize = population.getPersons().size();
//		Integer populationSize = 1;
		logger.warn(populationSize.toString());

		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			if(personId2emissionsInGrammPerType.containsKey(personId)){
				double fc =  personId2emissionsInGrammPerType.get(personId)[0];
				double nox = personId2emissionsInGrammPerType.get(personId)[1];
				double co2 = personId2emissionsInGrammPerType.get(personId)[2];
				double no2 = personId2emissionsInGrammPerType.get(personId)[3];
				double pm = personId2emissionsInGrammPerType.get(personId)[4];
				
				totalFc = totalFc + fc;
				totalNox = totalNox + nox;
				totalCo2 = totalCo2 + co2;
				totalNo2 = totalNo2 + no2;
				totalPM = totalPM + pm;
			}
		}
		avgEmissionsPerTypeandArea.add(totalFc / populationSize);
		avgEmissionsPerTypeandArea.add(totalNox / populationSize);
		avgEmissionsPerTypeandArea.add(totalCo2 / populationSize);
		avgEmissionsPerTypeandArea.add(totalNo2 / populationSize);
		avgEmissionsPerTypeandArea.add(totalPM / populationSize);
		return avgEmissionsPerTypeandArea;
	}

}
