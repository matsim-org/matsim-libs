/* *********************************************************************** *
 * project: org.matsim.*
 * DgPopulationAnalysisReader
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
package playground.dgrether.analysis.population;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.population.algorithms.PlanCalcType;


public class DgPopulationAnalysisReader {
	
	private static final Logger log = Logger.getLogger(DgPopulationAnalysisReader.class);

	private ScenarioImpl sc;
	
	public DgPopulationAnalysisReader(ScenarioImpl sc) {
		this.sc = sc;
	}
	
	/**
   * Creates the object and computes the resulting comparison
   *setActivity
   * @param firstPlanPath
   * @param secondPlanPath
   * @param outpath
   *          if null the output is written to the console
   */
	public DgAnalysisPopulation doPopulationAnalysis(final String networkPath, final String firstPlanPath,
			final String secondPlanPath) {
		PopulationImpl population;
		DgAnalysisPopulation analysisPopulation;
		
		ScenarioLoader sl = new ScenarioLoader(sc);
		sc.getConfig().network().setInputFile(networkPath);
		sl.loadNetwork();
		// load first plans file
		population = loadPlansFile(firstPlanPath, sc.getNetwork());
		new PlanCalcType().run(population);
		
		analysisPopulation = new DgAnalysisPopulation();
		PlanImpl plan;
		ActivityImpl act;
		for (Id id : population.getPersons().keySet()) {
			plan = population.getPersons().get(id).getSelectedPlan();
			act = plan.getFirstActivity();
			
			DgPersonData personData = new DgPersonData();
			personData.setFirstActivity(act);
			DgPlanData pd = new DgPlanData();
			pd.setScore(plan.getScore());
			pd.setPlan(plan);
			personData.getPlanData().put(DgAnalysisPopulation.RUNID1, pd);
			personData.setPersonId(id);
			analysisPopulation.getPersonData().put(id, personData);
		}
		// many people can be in one pop -> care about memory
		population = null;
		System.gc();
		// load second population
		population = loadPlansFile(secondPlanPath, sc.getNetwork());
		new PlanCalcType().run(population);
		for (Id id : population.getPersons().keySet()) {
			plan = population.getPersons().get(id).getSelectedPlan();
			DgPersonData personData = analysisPopulation.getPersonData().get(id);
			DgPlanData pd = new DgPlanData();
			pd.setScore(plan.getScore());
			pd.setPlan(plan);
			personData.getPlanData().put(DgAnalysisPopulation.RUNID2, pd);
		}
		return analysisPopulation;
	}

	/**
   * Load the plan file with the given path.
   *
   * @param filename
   *          the path to the filename
   * @return the Plans object containing the population
   */
	protected PopulationImpl loadPlansFile(final String filename, NetworkLayer network) {
		PopulationImpl plans = new PopulationImpl();

		log.info("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		plansReader.readFile(filename);
		log.info("  done");

		return plans;
	}
}
