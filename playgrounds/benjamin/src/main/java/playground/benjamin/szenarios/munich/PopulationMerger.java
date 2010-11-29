/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationMerger.java
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
package playground.benjamin.szenarios.munich;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * @author benjamin
 *
 */
public class PopulationMerger {

	private String networkFile = "../../detailedEval/Net/network.xml";
	private String midDemandFile = "../../detailedEval/pop/140k-synthetische-personen/plans.xml";
	private String prognose2025FreightDemandFile = "../../detailedEval/pop/gueterVerkehr/population_gv_bavaria_10pct_wgs84.xml.gz";
	private String prognose2025CommuterDemandFile = "../../detailedEval/pop/pendlerVerkehr/population_pv_bavaria_10pct_wgs84.xml.gz";
	private String outputPath = "../../detailedEval/pop/merged/";
	private String outputFileName = "mergedPopulation_10pct_gk4.xml.gz";

	private PopulationWriter populationWriter;
	
	protected static CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


	public static void main(String[] args) {
		PopulationMerger pm = new PopulationMerger();
		pm.run(args);
	}

	private void run(String[] args) {
		Scenario scenario = new ScenarioFactoryImpl().createScenario();
		PopulationImpl mergedPopulation = (PopulationImpl) scenario.getPopulation();

		mergedPopulation.setIsStreaming(true);
		populationWriter = new PopulationWriter(mergedPopulation, scenario.getNetwork());
		mergedPopulation.addAlgorithm(populationWriter);
		populationWriter.startStreaming(outputPath + outputFileName);

		addMidDemand(midDemandFile, networkFile, mergedPopulation);
		addPrognose2025FreightDemand(prognose2025FreightDemandFile, networkFile, mergedPopulation);
		addPrognose2025CommuterDemand(prognose2025CommuterDemandFile, networkFile, mergedPopulation);

		populationWriter.closeStreaming();
		System.out.println("Population merged!");
	}

	private static void addMidDemand(String midDemandFile, String networkFile, Population mergedPopulation) {
		Population midPopulation = PopulationMerger.getPopulation(midDemandFile, networkFile);
		
		for(Person person : midPopulation.getPersons().values()){
			mergedPopulation.addPerson(person);
		}
	}

	private static void addPrognose2025FreightDemand(String prognose2025FreightDemandFile, String networkFile, Population mergedPopulation) {
		Population freightPopulation = PopulationMerger.getPopulation(prognose2025FreightDemandFile, networkFile);

		for(Person person : freightPopulation.getPersons().values()){
			transformCoordinatesWGS84toGK4(person);
			addFreightPrefix(person);
			mergedPopulation.addPerson(person);
		}
	}

	private static void addPrognose2025CommuterDemand(String prognose2025CommuterDemandFile, String networkFile, Population mergedPopulation) {
		Population commuterPopulation = PopulationMerger.getPopulation(prognose2025CommuterDemandFile, networkFile);

		for(Person person : commuterPopulation.getPersons().values()){
			transformCoordinatesWGS84toGK4(person);
			addCommuterPrefix(person);
			mergedPopulation.addPerson(person);
		}
	}

	private static Population getPopulation(String demandFile, String networkFile) {
		Scenario sc = new ScenarioFactoryImpl().createScenario();
		Config config = sc.getConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(demandFile);

		//loading scenario and getting the population
		ScenarioLoader sl = new ScenarioLoaderImpl(sc) ;
		sl.loadScenario() ;
		Population population = sc.getPopulation();
		return population;
	}

	private static void transformCoordinatesWGS84toGK4(Person person) {
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if(pe instanceof Activity){
				
				Activity act = (Activity) pe;
				Coord wgs84Coord = act.getCoord();
				Coord gk4Coord = wgs84ToDhdnGk4.transform(wgs84Coord);
				act.getCoord().setXY(gk4Coord.getX(), gk4Coord.getY());
			}
		}
	}

	private static void addFreightPrefix(Person person) {
		Id id = person.getId();
		Id newId = new IdImpl("gv_" + id.toString());
		person.setId(newId);
		
	}

	private static void addCommuterPrefix(Person person) {
		Id id = person.getId();
		Id newId = new IdImpl("pv_" + id.toString());
		person.setId(newId);
		
	}

}
