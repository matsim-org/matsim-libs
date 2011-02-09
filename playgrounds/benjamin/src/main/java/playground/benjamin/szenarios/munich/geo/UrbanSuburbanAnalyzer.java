/* *********************************************************************** *
 * project: org.matsim.*
 * UrbanSuburbanAnalyzer.java
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
package playground.benjamin.szenarios.munich.geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author benjamin
 *
 */
public class UrbanSuburbanAnalyzer {

	private static final Logger logger = Logger.getLogger(UrbanSuburbanAnalyzer.class);

	// INPUT
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run7/";
	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String plansFile = runDirectory + "output_plans.xml.gz";
	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";

	// OUTPUT
	private static String outputPath = runDirectory + "urbanSuburban/";

	//===
	private final Scenario scenario;


	public UrbanSuburbanAnalyzer(){
		this.scenario = new ScenarioFactoryImpl().createScenario();
	}

	//	public UrbanSuburbanAnalyzer(Scenario scenario){
	//		this.scenario = scenario;
	//	}

	public static void main(String[] args) {
		UrbanSuburbanAnalyzer usa = new UrbanSuburbanAnalyzer();
		usa.run(args);
	}

	private void run(String[] args) {
		loadScenario();
		Set<Feature> urbanShape = readShape(urbanShapeFile);
		Set<Feature> suburbanShape = readShape(suburbanShapeFile);

		Population population = scenario.getPopulation();
		Population urbanPop = getRelevantPopulation(population, urbanShape);
		Population suburbanPop = getRelevantPopulation(population, suburbanShape);

		Map<String, Integer> urbanModalSplit = calculateModalSplitForArea(urbanPop);
		Map<String, Integer> suburbanModalSplit = calculateModalSplitForArea(suburbanPop);
		
		writeModalSplit(urbanModalSplit);
		writeModalSplit(suburbanModalSplit);
	}

	private void writeModalSplit(Map<String, Integer> mode2share) {
		System.out.println(mode2share);
	}

	private Map<String, Integer> calculateModalSplitForArea(Population population) {
		Map<String, Integer> mode2share = new HashMap<String, Integer>();
		List<String> transportModes = new ArrayList<String>();

		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.walk);
		transportModes.add("undefined");

		for(String transportMode : transportModes){
			Integer numberOfLegs = 0;
			numberOfLegs = calculateNoOfLegs(transportMode, population);
			mode2share.put(transportMode, numberOfLegs);
		}
		return mode2share;
	}

	private Integer calculateNoOfLegs(String transportMode, Population population) {
		Integer noOfLegs = 0;
		for(Person person : population.getPersons().values()){
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();
					if(transportMode.equals(mode)){
						noOfLegs = noOfLegs + 1;
					}
				}
			}
		}
		return noOfLegs;
	}

		private Population getRelevantPopulation(Population population,	Set<Feature> featuresInShape) {
			ScenarioImpl emptyScenario = new ScenarioImpl();
			Population filteredPopulation = new PopulationImpl(emptyScenario);
			for(Person person : population.getPersons().values()){
				if(isPersonFromMID(person)){
					if(isPersonInShape(person, featuresInShape)){
						filteredPopulation.addPerson(person);
					}
				}
			}
			return filteredPopulation;
		}

		private boolean isPersonFromMID(Person person) {
			boolean isFromMID = false;
			if(!person.getId().toString().contains("gv_") && !person.getId().toString().contains("pv_")){
				isFromMID = true;
			}
			return isFromMID;
		}

		private boolean isPersonInShape(Person person, Set<Feature> featuresInShape) {
			boolean isInShape = false;
			Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Coord homeCoord = homeAct.getCoord();
			GeometryFactory factory = new GeometryFactory();
			Geometry geo = factory.createPoint(new Coordinate(homeCoord.getX(), homeCoord.getY()));
			for(Feature feature : featuresInShape){
				if(feature.getDefaultGeometry().contains(geo)){
//					logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
					isInShape = true;
					break;
				}
			}
			return isInShape;
		}

		private Set<Feature> readShape(String shapeFile) {
			final Set<Feature> featuresInShape;
			try {
				featuresInShape = new ShapeFileReader().readFileAndInitialize(shapeFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return featuresInShape;
		}

		private void loadScenario() {
			Config config = scenario.getConfig();
			config.network().setInputFile(netFile);
			config.plans().setInputFile(plansFile);
			ScenarioLoader scenarioLoader = new ScenarioLoaderImpl(scenario) ;
			scenarioLoader.loadScenario() ;
		}
	}
