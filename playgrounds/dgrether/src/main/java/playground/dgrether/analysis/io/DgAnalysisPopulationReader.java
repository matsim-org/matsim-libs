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
package playground.dgrether.analysis.io;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlanCalcType;

import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.DgPlanData;


public class DgAnalysisPopulationReader {
	
	private static final Logger log = Logger.getLogger(DgAnalysisPopulationReader.class);

	private ScenarioImpl sc;

	private Map<String, NetworkLayer> loadedNetworks = new HashMap<String, NetworkLayer>();
	
	private boolean isExcludeTransit = false;
	
	public DgAnalysisPopulationReader(ScenarioImpl sc) {
		this.sc = sc;
	}
	
	public DgAnalysisPopulation readAnalysisPopulation(DgAnalysisPopulation analysisPopulation, final Id runId, final String networkPath, final String firstPlanPath) {
		PopulationImpl population;
		NetworkLayer net;
		if (this.loadedNetworks.containsKey(networkPath)){
			net = loadedNetworks.get(networkPath);
			this.sc.setNetwork(net);
		}
		else {
			ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc);
			sc.getConfig().network().setInputFile(networkPath);
			sl.loadNetwork();
			net = sc.getNetwork();
			this.loadedNetworks.put(networkPath, net);
		}
		// load first plans file
		population = loadPopulationFile(firstPlanPath, sc);
		new PlanCalcType().run(population);
		Plan plan;
		ActivityImpl act;
		for (Id id : population.getPersons().keySet()) {
			if (isExcludeTransit){
				if (isTransitPerson(id)){
					continue;
				}
			}
			plan = population.getPersons().get(id).getSelectedPlan();
			act = ((PlanImpl) plan).getFirstActivity();
			
			DgPersonData personData;
			personData = analysisPopulation.getPersonData().get(id);
			if (personData == null) {
				personData = new DgPersonData();
				analysisPopulation.getPersonData().put(id, personData);
				personData.setFirstActivity(act);
				personData.setPersonId(id);
			}
			DgPlanData pd = new DgPlanData();
			pd.setScore(plan.getScore());
			plan.setPerson(null);
//			pd.setPlan(plan);
			personData.getPlanData().put(runId, pd);
		}
		population = null;
		System.gc();
		return analysisPopulation;
	}

	
	
	private boolean isTransitPerson(Id id) {
		int idi = Integer.parseInt(id.toString());
		return (idi >= 1000000000);
	}

	/**
   * Creates the object and computes the resulting comparison
   *setActivity
   * @deprecated 
   * @param firstPlanPath
   * @param secondPlanPath
   * @param outpath
   *          if null the output is written to the console
   */
	@Deprecated
	public DgAnalysisPopulation doPopulationAnalysis(final String networkPath, final String firstPlanPath,
			final String secondPlanPath) {
		return doPopulationAnalysis( networkPath, firstPlanPath, secondPlanPath, "selected" ) ;
	}

	public DgAnalysisPopulation doPopulationAnalysis(final String networkPath, final String firstPlanPath,
			final String secondPlanPath, final String whichPlan ) {
		PopulationImpl population;
		DgAnalysisPopulation analysisPopulation;
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(sc);
		sc.getConfig().network().setInputFile(networkPath);
		sl.loadNetwork();
		// load first plans file
		population = loadPopulationFile(firstPlanPath, sc);
		new PlanCalcType().run(population);
		
		analysisPopulation = new DgAnalysisPopulation();
		Plan plan = null ;
		ActivityImpl act;
		for (Id id : population.getPersons().keySet()) {
			if ( whichPlan.equals( "selected" ) ) {
				plan = population.getPersons().get(id).getSelectedPlan();
			} else if ( whichPlan.equals( "best" ) ) {
				plan = ((PersonImpl) population.getPersons().get(id)).getBestPlan();
			} else {
				log.error( " whichPlan not recognized; aborting ... " ) ;
				System.exit( -1 ) ;
			}
			act = ((PlanImpl) plan).getFirstActivity();
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
		population = loadPopulationFile(secondPlanPath, sc);
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
	protected PopulationImpl loadPopulationFile(final String filename, ScenarioImpl sc) {
		PopulationImpl plans = new PopulationImpl();

		log.info("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(sc);
		plansReader.readFile(filename);
		log.info("  done");

		return plans;
	}

	public void setExcludeTransit(boolean b) {
		this.isExcludeTransit  = b;
	}
}
