/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.LEGO.miniscenario.samplingDCM;

import java.util.Random;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.LEGO.miniscenario.run.scoring.DestinationChoiceScoring;
import playground.anhorni.analysis.Bins;

public class UtilitySampler {

	private final static Logger log = Logger.getLogger(UtilitySampler.class);
	private ScenarioImpl scenario = new ScenarioImpl();	
	private ConfigReader configReader = new ConfigReader();
	private Config config;
	private Random rnd;
		
	public static void main(final String[] args) {
		UtilitySampler sampler = new UtilitySampler();
		sampler.init();
		sampler.run();	
		sampler.analyze();
		log.info("Sampling finished -----------------------------------------");
	}
	
	private void init() {
		configReader.read();	
		
		ScenarioImpl scenario = new ScenarioLoaderImpl(configReader.getPath() + "input/config.xml").getScenario();
		this.config = scenario.getConfig();
		
		this.rnd = new Random(4711);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(this.configReader.getPath() + "input/plans.xml");
		new MatsimNetworkReader(this.scenario).readFile(this.configReader.getPath() + "input/network.xml");
		new MatsimFacilitiesReader(this.scenario).readFile(this.configReader.getPath() + "input/facilities.xml");
	}

	private void run() {	
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.rnd, this.scenario.getActivityFacilities(), this.configReader, config);
		
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			// show counter
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			// if person is not in the analysis population
			if (Integer.parseInt(p.getId().toString()) > configReader.getAnalysisPopulationOffset()) continue;
			
			double maxScore = -999.0;
			Id bestFacilityId = null;
			for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilitiesForActivityType("shop").values()) {
				// shopping activity
				ActivityImpl act = (ActivityImpl)p.getSelectedPlan().getPlanElements().get(2);
				act.setFacilityId(facility.getId());
				act.setCoord(facility.getCoord());
				act.setLinkId(facility.getLinkId());
				
				boolean distance = false;
				if (configReader.getScoreElementDistance() > 0.000001) distance = true;
				
				double score = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act, distance);
				if (score > maxScore) {
					maxScore = score;
					bestFacilityId = facility.getId();
				}
			}
			ActivityImpl act = (ActivityImpl)p.getSelectedPlan().getPlanElements().get(2);
			ActivityFacility bestFacility = this.scenario.getActivityFacilities().getFacilities().get(bestFacilityId);
			act.setFacilityId(bestFacility.getId());
			act.setCoord(bestFacility.getCoord());
			act.setLinkId(bestFacility.getLinkId());
			p.getSelectedPlan().setScore(maxScore);
		}	
	}
	
	private void analyze() {
		double maxDistance = configReader.getSideLengt();
		Bins shopBins = new Bins(configReader.getSpacing() * 2, maxDistance, "shop_distance");
		Bins scoreBins = new Bins(0.25, 15, "scores");
			
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			
			// if person is not in the analysis population
			if (Integer.parseInt(p.getId().toString()) > configReader.getAnalysisPopulationOffset()) continue;
			
			PlanImpl plan = (PlanImpl)p.getSelectedPlan(); 
			
			double shopDistance = ((CoordImpl)(plan.getFirstActivity().getCoord())).calcDistance(
					((ActivityImpl)plan.getPlanElements().get(2)).getCoord());
			
			shopBins.addVal(shopDistance, 1.0);	
			scoreBins.addVal(plan.getScore(), 1.0);
		}
		shopBins.plotBinnedDistribution(configReader.getPath() + "output/dcm_sampling_", "#", "m");
		scoreBins.plotBinnedDistribution(configReader.getPath() + "output/dcm_sampling_", "#", "[utils]");
	}
}
