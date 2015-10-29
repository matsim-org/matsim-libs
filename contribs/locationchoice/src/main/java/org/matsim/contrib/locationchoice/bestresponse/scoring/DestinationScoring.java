/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.bestresponse.scoring;

import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.EpsilonDistributionTypes;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.config.Config;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributesUtils;

public class DestinationScoring { 
	//As the random number generator is re-seeded here anyway, we do not need a rng given from outside!
	private Random rnd = new Random();
	private Config config;
	private DestinationChoiceConfigGroup dccg;
	private double[] facilitiesKValuesArray;
	private double[] personsKValuesArray;
	private ScaleEpsilon scaleEpsilon;
	private DestinationChoiceBestResponseContext lcContext;
		
	public DestinationScoring(DestinationChoiceBestResponseContext lcContext) {
		this.config = lcContext.getScenario().getConfig();
		this.dccg = (DestinationChoiceConfigGroup) this.config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.facilitiesKValuesArray = lcContext.getFacilitiesKValuesArray();
		this.personsKValuesArray = lcContext.getPersonsKValuesArray();
		this.scaleEpsilon = lcContext.getScaleEpsilon();
		this.lcContext = lcContext;
	}
	
	public double getDestinationScore(Activity act, double fVar, int activityIndex, Id<Person> personId) {
		double score = 0.0;
		if (this.scaleEpsilon.isFlexibleType(act.getType())) {
			/*
			 * The following will not work if activities are inserted or removed during replanning (e.g. ptInteractionActivities).  kai/mz, oct'14
			 */
			int actIndex =  2*activityIndex ; // "2": retrofitting; this used to be the PlanElementIndex.  One could probably remove the "2",
			// but then the test fail because this is equivalent to a different random seed.  So more diligent checking would be needed.
			
			if (fVar < 0.0) fVar = this.scaleEpsilon.getEpsilonFactor(act.getType());
			score += (fVar * this.getEpsilonAlternative(act.getFacilityId(), personId, actIndex));
			score += this.getAttributesScore(act.getFacilityId(), personId );
		}
		return score;
	}
	
	/*
	 * linear at the moment
	 */
	private double getAttributesScore(Id<ActivityFacility> facilityId, Id<Person> personId) {
		double accumulatedScore = 0.0;
		
		if (this.lcContext.getPersonsBetas() != null && this.lcContext.getFacilitiesAttributes() != null) {
			
			// Maybe this is too dangerous and we should specify the available attributes and corresponding betas in the config as well
			// let us see!
			Collection<String> betas = ObjectAttributesUtils.getAllAttributeNames(this.lcContext.getPersonsBetas(), personId.toString());
			for (String name:betas) {
				double beta = (Double) this.lcContext.getPersonsBetas().getAttribute(personId.toString(), name);
				double attribute = (Double) this.lcContext.getFacilitiesAttributes().getAttribute(facilityId.toString(), name);
				accumulatedScore += beta * attribute;
			}
		}
		
		return accumulatedScore;
	}
	
	private double getEpsilonAlternative(Id<ActivityFacility> facilityId, Id<Person> personId, int actIndex) {
		/*
		 * k values are uniform in [0..1[, see class ReadOrCreateKVals.
		 */		
		double kf = this.facilitiesKValuesArray[this.lcContext.getFacilityIndex(facilityId)];
		double kp = this.personsKValuesArray[this.lcContext.getPersonIndex(personId)]; 
		
		/* generate another stable random number for the activity
		 * TODO: check if there is enough randomness with this seed
		 */
		rnd.setSeed(actIndex);
		double ka = rnd.nextDouble();
		
		/*
		 * generates a uniform rnd seed in [0,1[ 
		 */
		long seed = (long) (((kp + kf + ka) % 1.0) * Long.MAX_VALUE);
		rnd.setSeed(seed);
		
		/*
		 * generate the epsilons according to standard Gumbel or standard Gaussian distribution
		 */
		if (this.dccg.getEpsilonDistribution() == EpsilonDistributionTypes.gumbel) {
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
