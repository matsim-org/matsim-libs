/* *********************************************************************** *
 * project: org.matsim.*
 * WalkTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data to calculate a person's travel time on a link is taken from:
 * Weidmann, Ulrich (1992) Transporttechnik der Fussgänger - Transporttechnische Eigenschaften des Fussgängerverkehrs, 
 * Literaturauswertung, Schriftenreihe des IVT (90), Institut für Verkehrsplanung und Transportsysteme, Zürich.
 * 
 * Age data from 0..3 and from 80..100 is extrapolated.
 */
public class WalkTravelTime implements TravelTime {

	private static final Logger log = Logger.getLogger(WalkTravelTime.class);
	
	/*
	 * Cache variables for each thread accessing the object separately.
	 */
	/*package*/ final ThreadLocal<Person> personCache;
	/*package*/ final ThreadLocal<Double> personFactorCache;
	private final ThreadLocal<Double> personWalkSpeedCache;
	
	// Map shared between all threads accessing this object.
	/*package*/ final Map<Id, Double> personFactors;
	
	private final double referenceWalkSpeed; 				// 1.34 according to Weidmann, [m/s]
	
	private final double maleScaleFactor = 1.41 / 1.34;		// according to Weidmann
	private final double femaleScaleFactor = 1.27 / 1.34;	// according to Weidmann
	
	private final double weidmannReferenceWalkSpeed = 1.34;			// 1.34 according to Weidmann, [m/s]
	private final double weidmannScatterStandardDeviation = 0.26;	// 0.26 according to Weidmann, [m/s]
	
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

	// from 80% to -40%
	private final double[] slopeFactors = {
			0.0014, 0.0069, 0.0131, 0.0198, 0.0269, 0.0345, 0.0425, 0.0509, 0.0595, 0.0685,
			0.0776, 0.0870, 0.0966, 0.1064, 0.1163, 0.1264, 0.1365, 0.1468, 0.1572, 0.1677,
			0.1782, 0.1888, 0.1996, 0.2104, 0.2212, 0.2322, 0.2432, 0.2544, 0.2656, 0.2770,
			0.2884, 0.3000, 0.3117, 0.3236, 0.3356, 0.3477, 0.3600, 0.3725, 0.3852, 0.3981,
			0.4112, 0.4245, 0.4380, 0.4518, 0.4658, 0.4800, 0.4944, 0.5091, 0.5241, 0.5392,
			0.5546, 0.5703, 0.5861, 0.6022, 0.6185, 0.6349, 0.6516, 0.6683, 0.6852, 0.7023,
			0.7194, 0.7365, 0.7537, 0.7708, 0.7879, 0.8048, 0.8216, 0.8382, 0.8546, 0.8706,
			0.8862, 0.9013, 0.9160, 0.9300, 0.9433, 0.9558, 0.9675, 0.9782, 0.9878, 0.9963,
			1.0000, 1.0055, 1.0108, 1.0163, 1.0219, 1.0273, 1.0325, 1.0372, 1.0413, 1.0448,
			1.0474, 1.0491, 1.0497, 1.0494, 1.0478, 1.0451, 1.0412, 1.0361, 1.0297, 1.0221,
			1.0133, 1.0033, 0.9922, 0.9801, 0.9670, 0.9530, 0.9382, 0.9227, 0.9067, 0.8903,
			0.8737, 0.8570, 0.8405, 0.8242, 0.8085, 0.7935, 0.7795, 0.7667, 0.7555, 0.7460,
			0.7386};
	
	private final AtomicBoolean warnGender = new AtomicBoolean(true);
	private final AtomicBoolean warnSlopeRange = new AtomicBoolean(true);
	private final AtomicBoolean warnSlopeNotFound = new AtomicBoolean(true);
	private final AtomicBoolean warnAge = new AtomicBoolean(true);
	
	private final AtomicInteger genderWarnCount = new AtomicInteger(0);
	private final AtomicInteger slopeRangeWarnCount = new AtomicInteger(0);
	private final AtomicInteger slopeNotFoundWarnCount = new AtomicInteger(0);
	private final AtomicInteger ageWarnCount = new AtomicInteger(0);
	
	private final Map<Id<Link>, Double> linkSlopes;
	
	public WalkTravelTime(PlansCalcRouteConfigGroup plansCalcGroup, Map<Id<Link>, Double> linkSlopes) {
		this.referenceWalkSpeed = plansCalcGroup.getTeleportedModeSpeeds().get(TransportMode.walk);
		this.personCache = new ThreadLocal<>();
		this.personFactorCache = new ThreadLocal<>();
		this.personWalkSpeedCache = new ThreadLocal<>();
		
		this.personFactors = new ConcurrentHashMap<>();
		this.linkSlopes = linkSlopes;
	}
	
	// when used for transit_walk
	/*package*/ WalkTravelTime(double referenceWalkSpeed, Map<Id<Link>, Double> linkSlopes) {
		this.referenceWalkSpeed = referenceWalkSpeed;
		this.personCache = new ThreadLocal<>();
		this.personFactorCache = new ThreadLocal<>();
		this.personWalkSpeedCache = new ThreadLocal<>();
		
		this.personFactors = new ConcurrentHashMap<>();
		this.linkSlopes = linkSlopes;
	}


	public WalkTravelTime(PlansCalcRouteConfigGroup plansCalcGroup) {
		this(plansCalcGroup, null);
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {		
		setPerson(person);
		double slope = getSlope(link);
		double slopeFactor = getSlopeFactor(slope);
//		double personWalkSpeed = this.referenceWalkSpeed * this.personFactorCache.get();
		double personWalkSpeed = this.personWalkSpeedCache.get();
		return link.getLength() / (personWalkSpeed * slopeFactor);
	}

	/*package*/ double getSlopeFactor(double slope) {
		double slopeFactor;
		
		if (slope > 80.0) {
			if (slopeRangeWarnCount.get() < 10) {
				incSlopeRangeWarnCount("Slope is out of expected range (-40% .. -80%). Found slope of " + slope + ". Use 80.0 instead.");
			}
			slope = 80.0;
		} else if (slope < -40) {
			if (slopeRangeWarnCount.get() < 10) {
				incSlopeRangeWarnCount("Slope is out of expected range (-40% .. -80%). Found slope of " + slope + ". Use -40.0 instead.");
			}
			slope = -40.0;
		}
		slopeFactor = slopeFactors[-(int)Math.round(slope) + 80];			
		return slopeFactor;
	}
			
	/*
	 * Returns the slope of a link in %.
	 */
	/*package*/ final double getSlope(Link link) {
		
		// if no slope information is available
		if (this.linkSlopes == null) return 0.0;
		
		Double slope = this.linkSlopes.get(link.getId());
		if (slope == null) {
			incSlopeNotFoundWarnCount("No slope information for link " + link.getId().toString() +
					" was found. Assuming slope of 0%.");
			return 0.0;
		} else return slope;
	}

	private void incGenderWarnCount(String text) {
		genderWarnCount.incrementAndGet();
		if (warnGender.get()) {
			printWarning(text, genderWarnCount.get());
			if (genderWarnCount.get() >= 10) warnGender.set(false);
		}
	}
	
	private void incSlopeRangeWarnCount(String text) {
		slopeRangeWarnCount.incrementAndGet();
		if (warnSlopeRange.get()) {
			printWarning(text, slopeRangeWarnCount.get());
			if (slopeRangeWarnCount.get() >= 10) warnSlopeRange.set(false);
		}
	}
	
	private void incSlopeNotFoundWarnCount(String text) {
		slopeNotFoundWarnCount.incrementAndGet();
		if (warnSlopeNotFound.get()) {
			printWarning(text, slopeNotFoundWarnCount.get());
			if (slopeNotFoundWarnCount.get() >= 10) warnSlopeNotFound.set(false);
		}
	}
	
	private void incAgeWarnCount(String text) {
		ageWarnCount.incrementAndGet();
		if (warnAge.get()) {
			printWarning(text, ageWarnCount.get());
			if (ageWarnCount.get() >= 10) warnAge.set(false);
		}
	}
	
	private void printWarning(String text, int count) {
		if (count >= 10) log.warn(text + " No further warnings from this type will be given!");
		else log.warn(text);
	}
	
	/*package*/ void setPerson(Person person) {
		/* 
		 * Only recalculate the person's walk speed factor if
		 * the person has changed and is not in the map.
		 */
		if (this.personCache.get() != null && person.getId().equals(this.personCache.get().getId())) return;
		
		/*
		 * If the person's walk speed is already in the map, use that value.
		 * Otherwise calculate it and add it to the map.
		 */
		Double value = this.personFactors.get(person.getId());
		if (value != null) {
			double personFactor = value;
			this.personCache.set(person);
			this.personFactorCache.set(personFactor);
			this.personWalkSpeedCache.set(this.referenceWalkSpeed * personFactor);
			return;
		}
		
		double scatterFactor;
		double ageFactor = 1.0;
		double genderFactor = 1.0;

		// calculate scatter factor
		Random random = new Random();
		random.setSeed(person.getId().toString().hashCode());
		for (int i = 0; i < 5; i++) random.nextDouble();
		
		/*
		 * Limit scatter factor to +/- 4 times the standard deviation.
		 * Therefore, the scatter factor is 0.224 ... 1.777
		 */
		double scatterLimit = 4 * weidmannScatterStandardDeviation;
		double scatterSpeed = random.nextGaussian() * weidmannScatterStandardDeviation + weidmannReferenceWalkSpeed;
		if (scatterSpeed < weidmannReferenceWalkSpeed - scatterLimit) {
			scatterSpeed = weidmannReferenceWalkSpeed - scatterLimit;
		} else if (scatterSpeed > weidmannReferenceWalkSpeed + scatterLimit) {
			scatterSpeed = weidmannReferenceWalkSpeed + scatterLimit;
		}
		scatterFactor = scatterSpeed / weidmannReferenceWalkSpeed;
		
		if (person instanceof Person) {
			Person p = person;
			
			// get gender factor
			if (PersonUtils.getSex(p) == null) {
				if (genderWarnCount.get() < 10) {
					incGenderWarnCount("Person's gender is not defined. Ignoring gender dependent walk speed factor.");
				}
			} else if (PersonUtils.getSex(p).equals("m")) genderFactor = maleScaleFactor;
			else if (PersonUtils.getSex(p).equals("f")) genderFactor = femaleScaleFactor;
			else {
				if (genderWarnCount.get() < 10) {
					incGenderWarnCount("Person's gender is not defined. Ignoring gender dependent walk speed factor.");
				}
			}
			
			Integer age = PersonUtils.getAge(p);
			
			if (age == null) {
				if (ageWarnCount.get() < 10) {
					incAgeWarnCount("Person's age is not defined. Ignoring age dependent walk speed factor.");
				}
			}
			else if (age < 0) {
				if (ageWarnCount.get() < 10) {
					incAgeWarnCount("Person's age is out of expected range (0 .. 100). Founde age of " + age + ". Use 0 instead.");
				}
				ageFactor = ageFactors[0];
			} else if (age > 100) {
				if (ageWarnCount.get() < 10) {
					incAgeWarnCount("Person's age is out of expected range (0 .. 100). Founde age of " + age + ". Use 100 instead.");
				}
				ageFactor = ageFactors[100];
			} else {
				ageFactor = ageFactors[PersonUtils.getAge(p)];
			}
		}
		
		double personFactor = scatterFactor * ageFactor * genderFactor;
		this.personCache.set(person);
		this.personFactorCache.set(personFactor);
		this.personWalkSpeedCache.set(this.referenceWalkSpeed * personFactor);
		
		this.personFactors.put(person.getId(), personFactor);
	}

}
