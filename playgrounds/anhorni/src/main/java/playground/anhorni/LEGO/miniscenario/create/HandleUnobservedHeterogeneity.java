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

package playground.anhorni.LEGO.miniscenario.create;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.PersonImpl;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.random.RandomFromVarDistr;


public class HandleUnobservedHeterogeneity {
	private ScenarioImpl scenario;	
	private ConfigReader configReader;
	private RandomFromVarDistr rnd;
	private Config config;
			
	public HandleUnobservedHeterogeneity(ScenarioImpl scenario, ConfigReader configReader, RandomFromVarDistr rnd, Config config) {		
		this.scenario = scenario;
		this.configReader = configReader;
		this.rnd = rnd;
		this.config = config;
	}
	
	public void assign() {				
		double beta = Double.parseDouble(config.locationchoice().getSearchSpaceBeta());
		double utVarEpsilon = 1.0;	// utMeanEpsilon = 0.0;
		this.assignPersonsTastes(beta, configReader.getVarTastes());
		this.assignKValuesPersons(0.5 * utVarEpsilon);
		this.assignKValuesAlternatives(0.5 * utVarEpsilon);	
	}
	
	private void assignPersonsTastes(double mean, double var) {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			
			if (((PersonImpl)p).getDesires() == null) {
				((PersonImpl)p).createDesires("");
			}
			((PersonImpl)p).getDesires().setDesc(String.valueOf(rnd.getGaussian(
					Double.parseDouble(config.locationchoice().getSearchSpaceBeta()), Math.sqrt(var))));
		}
	}
	
	private double getRandom(double var) {
		if (configReader.isGumbel()) {
			double uniform = rnd.getUniform(1.0);
			// interval MUST be ]0,1[
			while (uniform == 0.0 || uniform == 1.0) {
				uniform = rnd.getUniform(1.0);
			}
			double r = 0.0 - 1.0 * Math.log(-Math.log(1.0 * uniform));
			//scale to sigma^2 = var: sigma_gumbel = PI / sqrt(6.0)
			return (r * Math.sqrt(6.0) * Math.sqrt(var) / Math.PI);	
		}
		else {	
			return rnd.getGaussian(0.0, Math.sqrt(var));	
		}
	}
	
	
	// does not matter which distribution is chosen here
	private void assignKValuesPersons(double var) {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			((PersonImpl)p).getDesires().setDesc(((PersonImpl)p).getDesires().getDesc() + "_" + rnd.getUniform(1.0));
		}
	}	
	private void assignKValuesAlternatives(double var) {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			((ActivityFacilityImpl)facility).setDesc(Double.toString(rnd.getUniform(1.0)));
		}
	}
	
//	public void computeLargestEpsilon(ScenarioImpl scenario, String type) {	
//		int counter = 0;
//		int nextMsg = 1;
//		
//		DestinationChoiceScoring scorer = new DestinationChoiceScoring(new Random(), this.scenario.getActivityFacilities(), configReader, this.config);	
//		log.info("Computing max epsilons for type: " + type + "...");
//		log.info(scenario.getActivityFacilities().getFacilitiesForActivityType(type).size() + " " + type + " facilities");
//		
//		TreeMap<Id, ActivityFacility> typedFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(type);
//		
//		for (Person p : scenario.getPopulation().getPersons().values()) {
//			//ceck if plan contains activity of type
//			boolean typeInPlan = false;
//			for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
//				if (pe instanceof Activity) {
//					if (((Activity) pe).getType().startsWith(type)) typeInPlan = true;
//				}
//			}
//			
//			double maxEpsilon = 0.0;
//			if (typeInPlan) {
//				for (Facility f : typedFacilities.values()) {
//					ActivityImpl act = new ActivityImpl(type, new IdImpl(1));
//					act.setFacilityId(f.getId());
//					double epsilon = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act, false);
//					
//					if (epsilon > maxEpsilon) {
//						maxEpsilon = epsilon;
//					}
//				}
//			}
//			counter++;
//			if (counter % nextMsg == 0) {
//				nextMsg *= 2;
//				log.info(" person # " + counter);
//			}
//			
//			String desiresDesc = ((PersonImpl)p).getDesires().getDesc();
//			((PersonImpl)p).getDesires().setDesc(desiresDesc + "_" + maxEpsilon);
//		}
//	}
}	

