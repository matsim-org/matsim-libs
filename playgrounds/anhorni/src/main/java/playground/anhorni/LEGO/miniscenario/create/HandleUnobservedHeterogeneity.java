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

import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.LEGO.miniscenario.run.scoring.DestinationChoiceScoring;
import playground.anhorni.random.RandomFromVarDistr;


public class HandleUnobservedHeterogeneity {
	private ScenarioImpl scenario;	
	private ConfigReader configReader;
	private RandomFromVarDistr rnd;
//	private double b;
	private final static Logger log = Logger.getLogger(HandleUnobservedHeterogeneity.class);
			
	public HandleUnobservedHeterogeneity(ScenarioImpl scenario, ConfigReader configReader, RandomFromVarDistr rnd) {		
		this.scenario = scenario;
		this.configReader = configReader;
		this.rnd = rnd;
//		b = this.configReader.getMaxDistance();
	}
	
	public void assign() {				
		double beta = configReader.getBeta();
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
			((PersonImpl)p).getDesires().setDesc(String.valueOf(rnd.getGaussian(configReader.getBeta(), Math.sqrt(var))));
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
			((PersonImpl)p).getDesires().setDesc(((PersonImpl)p).getDesires().getDesc() + "_" + getRandom(var));
		}
	}	
	private void assignKValuesAlternatives(double var) {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			((ActivityFacilityImpl)facility).setDesc(Double.toString(getRandom(var)));
		}
	}
	
	public void computeLargestEpsilon(ScenarioImpl scenario, String type) {	
		int counter = 0;
		int nextMsg = 1;
		
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(new Random(), this.scenario.getActivityFacilities(), configReader);	
		log.info("Computing max epsilons for type: " + type + "...");
		log.info(scenario.getActivityFacilities().getFacilitiesForActivityType(type).size() + " " + type + " facilities");
		
		TreeMap<Id, ActivityFacility> typedFacilities = scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			//ceck if plan contains activity of type
			boolean typeInPlan = false;
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().startsWith(type)) typeInPlan = true;
				}
			}
			
			double maxEpsilon = 0.0;
			if (typeInPlan) {
				for (Facility f : typedFacilities.values()) {
					ActivityImpl act = new ActivityImpl(type, new IdImpl(1));
					act.setFacilityId(f.getId());
					double epsilon = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act, false);
					
					if (epsilon > maxEpsilon) {
						maxEpsilon = epsilon;
					}
				}
			}
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			
			String desiresDesc = ((PersonImpl)p).getDesires().getDesc();
			((PersonImpl)p).getDesires().setDesc(desiresDesc + "_" + maxEpsilon);
		}
	}
}

// old: --------------------------------------------------------------------------------------------
	
//	private double getScaleDistance(double utVarDistanceSoll) {
//		return Math.sqrt(utVarDistanceSoll / (Math.pow(this.getBetaDistance(b), 2.0) * this.getVarianceDistance()));
//	}
	
//	private double getVarianceTot() {
//		double var = 1.0;
//		if (configReader.getScoreElementDistance() > 0.00000001) {
//			var += this.getVarShare(configReader.getScoreElementDistance());
//		}
//		return var;		
//	}
	
//	private double getVarShare(double f) {
//		return f / (1-f);
//	}
	
//	private double getUtilityVarianceDue2Tastes(double b, double f) {
//		if (f==0) return 0.0;
//		
//		double beta = this.getBetaDistance(b);
//		
//		return Math.pow(this.getExpectedValueDistance(), 2.0) * this.getVarianceOfTastes(b, f) +
//			Math.pow(beta, 2.0) * this.getVarianceDistance() + 
//			this.getVarianceDistance() * this.getVarianceOfTastes(b, f);
//	}
	
//	//ok
//	private double getExpectedValueDistance() {
//		return (1.0 * this.b / 3.0);
//	}
//	
//	//ok
//	private double getVarianceDistance() {
//		return (Math.pow(this.b, 2.0) / 18.0);
//	}
	
//	private double getBetaDistance(double b) {
//		return (- 2.0 / Math.pow(b, 2.0));
//	}
		
//	private double getVarianceOfTastes(double b, double f) {
//		if (f==0.0) return 0.0;
//		
//		double beta = this.getBetaDistance(b);
//		 
//		return (Math.pow(beta, 2.0) * this.getVarianceDistance() *(1 - f)) / 
//			(f * (Math.pow(this.getExpectedValueDistance(), 2.0) + this.getVarianceDistance()));
//	}	

