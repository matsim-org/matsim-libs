/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.PC2.scoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
/**
 * This class is based on work by Andreas Horni related to location choice, located at:
 * org.matsim.contrib.locationchoice.bestresponse.DestinationScoring
 * 
 * @author wrashid, ahorni
 *
 */
public class RandomErrorTermManager {
	
	private Random rnd = new Random();
	HashMap<Id,Double> parkingKValue=new HashMap<Id,Double>();
	HashMap<Id,Double> personKValue=new HashMap<Id,Double>();
	String epsilonDistribution;
	
	public RandomErrorTermManager(String epsilonDistribution,
			LinkedList<Id> parkingIds, Collection<? extends Person> persons, int seed) {
				this.epsilonDistribution = epsilonDistribution;
		
		Random random = new Random();
		random.setSeed(seed);
		for (Id parkingId:parkingIds){
			parkingKValue.put(parkingId, random.nextDouble());
		}
		
		for (Person person: persons){
			personKValue.put(person.getId(), random.nextDouble());
		}
	}

	public double getEpsilonAlternative(Id parkingId, Id personId, int actIndex) {
		/*
		 * k values are uniform in [0..1[, see class ReadOrCreateKVals.
		 */		
		double kparking = parkingKValue.get(parkingId);
		double kperson = personKValue.get(personId); 
		
		/* generate another stable random number for the activity
		 */
		rnd.setSeed(actIndex);
		double kactIndex = rnd.nextDouble();
		
		/*
		 * generates a uniform rnd seed in [0,1[ 
		 */
		long seed = (long) (((kperson + kparking + kactIndex) % 1.0) * Long.MAX_VALUE);
		rnd.setSeed(seed);
		
		/*
		 * generate the epsilons according to standard Gumbel or standard Gaussian distribution
		 */
		if (epsilonDistribution.equalsIgnoreCase("gumbel")) {
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
		else if (epsilonDistribution.equalsIgnoreCase("gaussian")) {
			// take a few draws to come to the "chaotic region"
			for (int i = 0; i < 5; i++) {
				rnd.nextGaussian();
			}
			return rnd.nextGaussian();	
		} else {
			DebugLib.stopSystemAndReportInconsistency("unknown epsilonDistribution: " + epsilonDistribution);
			return 0;
		}
	}

}
