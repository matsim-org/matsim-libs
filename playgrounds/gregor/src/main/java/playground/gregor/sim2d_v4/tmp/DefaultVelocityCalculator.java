/* *********************************************************************** *
 * project: org.matsim.*
 * VelocityCalculatorImpl.java
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

package playground.gregor.sim2d_v4.tmp;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;

/**
 * Data to calculate a person's velocity on a link is taken from:
 * Weidmann, Ulrich (1992) Transporttechnik der Fussgänger - Transporttechnische Eigenschaften des Fussgängerverkehrs, 
 * Literaturauswertung, Schriftenreihe des IVT (90), Institut für Verkehrsplanung und Transportsysteme, Zürich.
 * 
 * Age data from 0..3 and from 80..100 is extrapolated.
 */
public class DefaultVelocityCalculator implements VelocityCalculator {

	private static final Logger log = Logger.getLogger(DefaultVelocityCalculator.class);
	
	private final double referenceWalkSpeed; 				// 1.34 according to Weidmann, [m/s]
	
	private final double maleScaleFactor = 1.41 / 1.34;		// according to Weidmann
	private final double femaleScaleFactor = 1.27 / 1.34;	// according to Weidmann
	
	private final double scatterStandardDeviation = 0.26;	// according to Weidmann, [m/s]
	
	// age from 0 .. 100
	private final double[] ageFactors = {
			0.1940, 0.2776, 0.3582, 0.4328, 0.5075, 0.5821, 0.6567, 0.7090, 0.7687, 0.8284,
			0.8806, 0.9328, 0.9776, 1.0261, 1.0672, 1.1082, 1.1418, 1.1716, 1.1940, 1.2090,
			1.2127, 1.2127, 1.2090, 1.1978, 1.1866, 1.1791, 1.1754, 1.1716, 1.1642, 1.1604,
			1.1567, 1.1493, 1.1455, 1.1418, 1.1381, 1.1343, 1.1306, 1.1269, 1.1231, 1.1194,
			1.1157, 1.1119, 1.1082, 1.1045, 1.0970, 1.0896, 1.0858, 1.0821, 1.0746, 1.0672,
			1.0597, 1.0522, 1.0410, 1.0299, 1.0224, 1.0112, 1.0000, 0.9888, 0.9776, 0.9627,
			0.9478, 0.9328, 0.9216, 0.9067, 0.8918, 0.8769, 0.8582, 0.8433, 0.8284, 0.8097,
			0.7873, 0.7687, 0.7500, 0.7276, 0.7052, 0.6791, 0.6493, 0.6194, 0.5896, 0.5560,
			0.5149, 0.4813, 0.4478, 0.4104, 0.3843, 0.3582, 0.3358, 0.3172, 0.2985, 0.2799,
			0.2612, 0.2463, 0.2351, 0.2239, 0.2164, 0.2090, 0.2015, 0.1978, 0.1940, 0.1903,
			0.1866};

	private final Random random;
	
	private boolean warnGender;
	private boolean warnAge;
	
	private int genderWarnCount = 0;
	private int ageWarnCount = 0;

	
	public DefaultVelocityCalculator(PlansCalcRouteConfigGroup plansCalcGroup) {
		this.referenceWalkSpeed = plansCalcGroup.getTeleportedModeSpeeds().get(TransportMode.walk);
		this.random = MatsimRandom.getLocalInstance();
	}
	
	@Override
	public double getVelocity(Person person, Link link) {
		double personFactor = calculatePersonFactors(person);
		double linkFactor = calculateLinkFactor(link);
		
		double personWalkVelocity = this.referenceWalkSpeed * personFactor * linkFactor;
		return personWalkVelocity;
	}
	
	private void incGenderWarnCount(String text) {
		this.genderWarnCount++;
		if (this.warnGender) {
			printWarning(text, this.genderWarnCount);
			if (this.genderWarnCount >= 10) this.warnGender = false;
		}
	}

	private void incAgeWarnCount(String text) {
		this.ageWarnCount++;
		if (this.warnAge) {
			printWarning(text, this.ageWarnCount);
			if (this.ageWarnCount >= 10) this.warnAge = false;
		}
	}
	
	private void printWarning(String text, int count) {
		if (count >= 10) log.warn(text + " No further warnings from this type will be given!");
		else log.warn(text);
	}
	
	private double calculatePersonFactors(Person person) {
				
		double scatterFactor = 1.0;
		double ageFactor = 1.0;
		double genderFactor = 1.0;

		// calculate scatter factor
		this.random.setSeed(person.getId().toString().hashCode());
		for (int i = 0; i < 5; i++) this.random.nextDouble();
		
		// limit scatter factor to +/- 4 times the standard deviation
		double scatterSpeed = this.random.nextGaussian() * this.scatterStandardDeviation + this.referenceWalkSpeed;
		if (scatterSpeed < this.referenceWalkSpeed - 4 * this.scatterStandardDeviation) {
			scatterSpeed = this.referenceWalkSpeed - 4 * this.scatterStandardDeviation;
		} else if (scatterSpeed > this.referenceWalkSpeed + 4 * this.scatterStandardDeviation) {
			scatterSpeed = this.referenceWalkSpeed + 4 * this.scatterStandardDeviation;
		}
		scatterFactor = this.referenceWalkSpeed / scatterSpeed;
		
		if (person instanceof PersonImpl) {
			Person p = person;
			
			// get gender factor
			if (PersonImpl.getSex(p) == null) {
				if (this.genderWarnCount < 10) {
					incGenderWarnCount("Person's gender is not defined. Ignoring gender dependent walk speed factor.");
				}
			} else if (PersonImpl.getSex(p).equalsIgnoreCase("m")) genderFactor = this.maleScaleFactor;
			else if (PersonImpl.getSex(p).equalsIgnoreCase("f")) genderFactor = this.femaleScaleFactor;
			else {
				if (this.genderWarnCount < 10) {
					incGenderWarnCount("Person's gender is not defined correct - expected 'm' or 'f' but found " +
							PersonImpl.getSex(p) + ". Ignoring gender dependent walk speed factor.");
				}
			}
			
			int age = PersonImpl.getAge(p);
			
			// by default, age is set to Integer.MIN_VALUE in PersonImpl  
			if (age == Integer.MIN_VALUE) {
				if (this.ageWarnCount < 10) {
					incAgeWarnCount("Person's age is not defined. Ignoring age dependent walk speed factor.");
				}
			}
			else if (age < 0) {
				if (this.ageWarnCount < 10) {
					incAgeWarnCount("Person's age is out of expected range (0 .. 100). Founde age of " + age + ". Use 0 instead.");
				}
				ageFactor = this.ageFactors[0];
			} else if (age > 100) {
				if (this.ageWarnCount < 10) {
					incAgeWarnCount("Person's age is out of expected range (0 .. 100). Founde age of " + age + ". Use 100 instead.");
				}
				ageFactor = this.ageFactors[100];
			} else {
				ageFactor = this.ageFactors[PersonImpl.getAge(p)];
			}
		}
		
		double personFactor = scatterFactor * ageFactor * genderFactor;
		return personFactor;
	}
	
	/*
	 * TODO: take a links slope into account when 3d coordinates are implemented in MATSim.
	 */
	private double calculateLinkFactor(Link link) {
		return 1.0;
	}
}