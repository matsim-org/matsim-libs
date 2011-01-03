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

package playground.anhorni.LEGO.miniscenario.run.scoring;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.LEGO.miniscenario.ConfigReader;

public class DestinationChoiceScoring {

	private Random rnd;
	private ActivityFacilities facilities;
	private ConfigReader configReader;
	private Config config;
	
	public DestinationChoiceScoring(Random rnd, ActivityFacilities facilities , ConfigReader configReader, Config config) {
		this.rnd = rnd;
		this.facilities = facilities;
		this.configReader = configReader;
		this.config = config;
	}
				
	public double getDestinationScore(PlanImpl plan, ActivityImpl act, boolean distance) {
		if (!(act.getType().startsWith("s") || act.getType().startsWith("l"))) return 0.0;
		double score = 0.0;
		
		if (distance) {
			score += this.getDistanceUtility(plan.getPerson(), act, 
					plan.getPreviousActivity(plan.getPreviousLeg(act)),
					plan.getNextActivity(plan.getNextLeg(act)));
		}
		if (configReader.getScoreElementEpsilons() > 0.000001) {
			score += this.getEpsilonAlternative(act.getFacilityId(), plan.getPerson());
		}		
		return score;
	}
	
	private double getDistanceUtility(PersonImpl person, ActivityImpl act, Activity actPre, Activity actPost) {
		
		double distanceDirect = ((CoordImpl)actPre.getCoord()).calcDistance(actPost.getCoord());
		
		double distance = ((CoordImpl)actPre.getCoord()).calcDistance(act.getCoord()) + 
		((CoordImpl)act.getCoord()).calcDistance(actPost.getCoord()) - distanceDirect;
		
		double beta = Double.parseDouble(this.config.locationchoice().getSearchSpaceBeta());
		
		double utilityDistanceObserved = 0.0;
		if (configReader.isLinearDistanceUtility()) {
			utilityDistanceObserved = beta * distance;
		}
		else {
			utilityDistanceObserved = (-2.0) * Math.log(1.0 + distance * beta * (-1.0));
		}
		double utilityDistanceUnobserved = 0.0;		
		if (configReader.getScoreElementTastes() > 0.000001) {
			utilityDistanceObserved = 0;
			utilityDistanceUnobserved = Double.parseDouble(person.getDesires().getDesc().split("_")[0]) * distance;
		}
		return utilityDistanceObserved + utilityDistanceUnobserved;
	}
	
	private double getEpsilonAlternative(Id facilityId, PersonImpl person) {		
		ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
		double kf = Double.parseDouble(((ActivityFacilityImpl)facility).getDesc());
		double kp = Double.parseDouble(person.getDesires().getDesc().split("_")[1]);
		
		long seed = (long) ((kp + kf) * Math.pow(2.0, 40));
		rnd.setSeed(seed);
				
		for (int i = 0; i < 5; i++) {
			rnd.nextGaussian();
		}
		if (configReader.isGumbel()) {
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
			return rnd.nextGaussian();	
		}
	}
}
