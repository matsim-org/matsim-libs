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

package org.matsim.locationchoice.bestresponse.scoring;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class DestinationChoiceScoring { 
	//As the random number generator is re-seeded here anyway, we do not need a rng given from outside!
	private Random rnd = new Random();
	private ActivityFacilities facilities;
	private Config config;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
		
	public DestinationChoiceScoring(ActivityFacilities facilities , Config config, 
			ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues) {
		this.facilities = facilities;
		this.config = config;
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
	}
				
	public double getDestinationScore(PlanImpl plan, ActivityImpl act) {
		if (!(act.getType().startsWith("s") || act.getType().startsWith("l"))) return 0.0;
		double score = 0.0;

// do not use distance scoring anymore
//		if (distance) {
//			score += this.getDistanceUtility(plan.getPerson(), act, 
//					plan.getPreviousActivity(plan.getPreviousLeg(act)),
//					plan.getNextActivity(plan.getNextLeg(act)));
//		}
		
// always use epsilons now
//		if (Double.parseDouble(config.findParam(LCEXP, "scoreElementEpsilons")) > 0.000001) {
			double fVar = 1.0;
			if (act.getType().startsWith("s")) {
				fVar = Double.parseDouble(config.locationchoice().getScaleEpsShopping());
			}
			else if (act.getType().startsWith("l")){
				fVar = Double.parseDouble(config.locationchoice().getScaleEpsLeisure());
			}
			else {
				fVar = 1.0;
			}
			score += (fVar * this.getEpsilonAlternative(act.getFacilityId(), plan.getPerson()));
//		}		
		return score;
	}

// do not use distance scoring anymore
//	private double getDistanceUtility(PersonImpl person, ActivityImpl act, Activity actPre, Activity actPost) {
//		
//		double distanceDirect = ((CoordImpl)actPre.getCoord()).calcDistance(actPost.getCoord());
//		
//		double distance = ((CoordImpl)actPre.getCoord()).calcDistance(act.getCoord()) + 
//		((CoordImpl)act.getCoord()).calcDistance(actPost.getCoord()) - distanceDirect;
//		
//		double beta = Double.parseDouble(this.config.locationchoice().getSearchSpaceBeta());
//		
//		double utilityDistanceObserved = 0.0;
//		if (Boolean.parseBoolean(config.findParam(LCEXP, "linearDistanceUtility"))) {
//			utilityDistanceObserved = beta * distance;
//		}
//		else {
//			utilityDistanceObserved = (-2.0) * Math.log(1.0 + distance * beta * (-1.0));
//		}
//		double utilityDistanceUnobserved = 0.0;		
//		if (Double.parseDouble(config.findParam(LCEXP, "varTastes")) > 0.000001) {
//			utilityDistanceObserved = 0;
//			utilityDistanceUnobserved = Double.parseDouble(person.getDesires().getDesc().split("_")[0]) * distance;
//		}
//		return utilityDistanceObserved + utilityDistanceUnobserved;
//	}
	
	private double getEpsilonAlternative(Id facilityId, PersonImpl person) {		
		ActivityFacility facility = this.facilities.getFacilities().get(facilityId);		
		double kf = (Double) this.facilitiesKValues.getAttribute(facility.getId().toString(), "k");
		double kp = (Double) this.personsKValues.getAttribute(person.getId().toString(), "k");
		
		/* long seed = (long) ((kp + kf) * Math.pow(2.0, 40)); 
		/* This was not a good solution.
		*/
				
		// I use now the uniform distribution for the generation of the k-values:
		// kp= [0..1] kf=[0..1]
		long seed = (long) ((kp * kf) * Long.MAX_VALUE);
		rnd.setSeed(seed);
				
		if (config.locationchoice().getEpsilonDistribution().equals("gumbel")) {
			// take a few draws to come to the "chaotic region"
			for (int i = 0; i < 5; i++) {
				rnd.nextDouble();
			}
			double uniform = rnd.nextDouble();
			// interval MUST be ]0,1[
			while (uniform == 0.0 || uniform == 1.0) {
				uniform = rnd.nextDouble();
			}
			double r = 0.0 - 1.0 * Math.log(-Math.log(1.0 * uniform));
			//scale to sigma^2 = 1.0: sigma_gumbel = PI / sqrt(6.0)
			return (r * Math.sqrt(6.0) / Math.PI);	
		}
		else {
			// take a few draws to come to the "chaotic region"
			for (int i = 0; i < 5; i++) {
				rnd.nextGaussian();
			}
			return rnd.nextGaussian();	
		}
	}
}
