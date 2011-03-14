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
package playground.demandde.munich;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;


/**
 * @author benjamin
 *
 */
public class PopulationMerger {

	private String networkFile = "../../detailedEval/Net/network-86-85-87-84.xml";
	// the network file is necessary here since pop_v4 needs the network.  Should not be needed with
	// pop_v5 

	private String midDemandFile = "../../detailedEval/pop/140k-synthetische-personen/plans.xml";
	// comes from somewhere else based on detailed data for the city (berlin = srv, muc = mic).
	// might need some plug-in for approximate internal traffic if this is not available.
	
	private String prognose2025FreightDemandFile = "../../detailedEval/pop/gueterVerkehr/population_gv_bavaria_10pct_wgs84.xml.gz";
	// this "falls from the sky" (ruby script)
	
//	private String prognose2025CommuterDemandFile = "../../detailedEval/pop/pendlerVerkehr/population_pv_bavaria_10pct_wgs84.xml.gz";
//	private String pendlerstatistikInCommutingDemandFile = "../../detailedEval/pop/pendlerVerkehr/pendlermatrizen/onlyIn/pendlerverkehr_1pct_dhdn_gk4.xml.gz";

	private String pendlerstatistikCommutingDemandFile = "../../detailedEval/pop/pendlerVerkehr/pendlermatrizen/inAndOut/pendlerverkehr_10pct_scaledAndMode_workStartingTimePeak0800Var2h_dhdn_gk4.xml.gz";
	// this comes from other classes in this playground module
	
	private String outputPath = "../../detailedEval/pop/merged/";
	private String outputFileName = "mergedPopulation_All_10pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";

	private PopulationWriter populationWriter;
	
	protected static CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


	public static void main(String[] args) {
		PopulationMerger pm = new PopulationMerger();
		pm.run(args);
	}

	private void run(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		PopulationImpl mergedPopulation = (PopulationImpl) scenario.getPopulation();

		mergedPopulation.setIsStreaming(true);
		populationWriter = new PopulationWriter(mergedPopulation, scenario.getNetwork());
		mergedPopulation.addAlgorithm(populationWriter);
		populationWriter.startStreaming(outputPath + outputFileName);

		addMidDemand(midDemandFile, networkFile, mergedPopulation);
		addPendlerstatistikInCommuterDemand(pendlerstatistikCommutingDemandFile, networkFile, mergedPopulation);
		addPrognose2025FreightDemand(prognose2025FreightDemandFile, networkFile, mergedPopulation);
//		addPrognose2025CommuterDemand(prognose2025CommuterDemandFile, networkFile, mergedPopulation);

		populationWriter.closeStreaming();
		System.out.println("Population merged!");
	}

	private void addPendlerstatistikInCommuterDemand(String pendlerstatistikInCommutingDemandFile, String networkFile, PopulationImpl mergedPopulation) {
		Population inCommuterPopulation = PopulationMerger.getPopulation(pendlerstatistikInCommutingDemandFile, networkFile);
		
		for(Person person : inCommuterPopulation.getPersons().values()){
			addCommuterPrefix(person);
			mergedPopulation.addPerson(person);
		}
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
		Config config1 = ConfigUtils.createConfig();
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(config1);
		Config config = sc.getConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(demandFile);

		//loading scenario and getting the population
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc) ;
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
