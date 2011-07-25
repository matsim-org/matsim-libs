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
package playground.benjamin.szenarios.munich.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class UrbanSuburbanAnalyzer {

	private static final Logger logger = Logger.getLogger(UrbanSuburbanAnalyzer.class);

	// INPUT
	//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run30/";
	private static String runNumber = "970";
	private static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	private static String shapeDirectory = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/";

	//	private static String netFile = runDirectory + "output_network.xml.gz";
	//	private static String plansFile = runDirectory + "output_plans.xml.gz";
	private static String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private static String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";

	private static String urbanShapeFile = shapeDirectory + "urbanAreas.shp";
	private static String suburbanShapeFile = shapeDirectory + "suburbanAreas.shp";
	private static String cityShapeFile = shapeDirectory + "cityArea.shp";

	// OUTPUT
	private static String outputPath = runDirectory + "urbanSuburban/";

	//===
	private Scenario scenario;


	public UrbanSuburbanAnalyzer(){
		Config config = ConfigUtils.createConfig();
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
	}

	public UrbanSuburbanAnalyzer(Scenario scenario){
		this.scenario = scenario;
	}

	public static void main(String[] args) {
		UrbanSuburbanAnalyzer usa = new UrbanSuburbanAnalyzer();
		usa.loadScenario();
		usa.run();
	}

	public void run() {
		Set<Feature> urbanShape = readShape(urbanShapeFile);
		Set<Feature> suburbanShape = readShape(suburbanShapeFile);
		Set<Feature> cityShape = readShape(cityShapeFile);

		Population population = scenario.getPopulation();
		Population mucPop = getRelevantPopulation(population, cityShape);
		Population urbanMucPop = getRelevantPopulation(population, urbanShape);
		Population suburbanMucPop = getRelevantPopulation(population, suburbanShape);

		mucPop.setName("mucPop");
		urbanMucPop.setName("urbanMucPop");
		suburbanMucPop.setName("suburbanMucPop");

		Map<String, Integer> mucMode2NoOfLegs = getMode2NoOfLegs(mucPop);
		Map<String, Integer> urbanMode2NoOfLegs = getMode2NoOfLegs(urbanMucPop);
		Map<String, Integer> suburbanMode2NoOfLegs = getMode2NoOfLegs(suburbanMucPop);
		Integer mucTotalLegs = calculateTotalLegs(mucPop);
		Integer urbanTotalLegs = calculateTotalLegs(urbanMucPop);
		Integer suburbanTotalLegs = calculateTotalLegs(suburbanMucPop);

		writeInformation(mucPop, mucMode2NoOfLegs, mucTotalLegs);
		writeInformation(urbanMucPop, urbanMode2NoOfLegs, urbanTotalLegs);
		writeInformation(suburbanMucPop, suburbanMode2NoOfLegs, suburbanTotalLegs);
	}

	private void writeInformation(Population population, Map<String, Integer> mode2NoOfLegs, Integer totalLegs) {

		String name = population.getName();
		int size = population.getPersons().size();

		System.out.println("##################################################################");
		System.out.println(name + " consists of " + size + " persons that execute " + totalLegs + " Legs.");
		System.out.println("##################################################################");
		for(Entry<String, Integer> entry : mode2NoOfLegs.entrySet()){
			Double modeShare = (double) entry.getValue() / (double) totalLegs;
			System.out.println(entry.getKey() + "\t" + "noOfLegs: " + entry.getValue() + "\t" + "modeShare: " + modeShare * 100 + " %");
		}
		System.out.println("##################################################################" + "\n");
	}

	private Integer calculateTotalLegs(Population population) {
		Integer totalLegs = 0;
		for(Person person : population.getPersons().values()){
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					totalLegs++;
				}
			}
		}
		return totalLegs;
	}

	private Map<String, Integer> getMode2NoOfLegs(Population population) {
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<String, Integer>();
		List<String> transportModes = new ArrayList<String>();

		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.walk);
		transportModes.add(TransportMode.bike);
		transportModes.add("undefined");

		for(String transportMode : transportModes){
			Integer noOfLegs = null;
			noOfLegs = calculateNoOfLegs(transportMode, population);
			mode2NoOfLegs.put(transportMode, noOfLegs);
		}
		return mode2NoOfLegs;
	}

	private Integer calculateNoOfLegs(String transportMode, Population population) {
		Integer noOfLegs = 0;
		for(Person person : population.getPersons().values()){
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();
					if(transportMode.equals(mode)){
						noOfLegs++;
					}
				}
			}
		}
		return noOfLegs;
	}

	public Population getMiDPopulation(Population population) {
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public Population getRelevantPopulation(Population population,	Set<Feature> featuresInShape) {
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonNonFreight(person)){
				if(isPersonsHomeInShape(person, featuresInShape)){
					filteredPopulation.addPerson(person);
				}
			}
		}
		return filteredPopulation;
	}

	private boolean isPersonNonFreight(Person person) {
		boolean isNonFreight = false;
		if(!person.getId().toString().contains("gv_")){
			isNonFreight = true;
		}
		return isNonFreight;
	}

	private boolean isPersonFromMID(Person person) {
		boolean isFromMID = false;
		if(!person.getId().toString().contains("gv_") && !person.getId().toString().contains("pv_")){
			isFromMID = true;
		}
		return isFromMID;
	}

	private boolean isPersonsHomeInShape(Person person, Set<Feature> featuresInShape) {
		boolean isInShape = false;
		Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		Coord homeCoord = homeAct.getCoord();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(homeCoord.getX(), homeCoord.getY()));
		for(Feature feature : featuresInShape){
			if(feature.getDefaultGeometry().contains(geo)){
				//logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}

	public Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInShape;
		featuresInShape = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInShape;
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}
}
