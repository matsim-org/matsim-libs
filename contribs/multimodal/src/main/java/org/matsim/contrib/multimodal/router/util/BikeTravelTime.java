/* *********************************************************************** *
 * project: org.matsim.*
 * BikeTravelTime.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * Data to calculate a person's travel time on a link is taken from:
 * John Parkin and Jonathon Rotheram (2010): Design speeds and acceleration characteristics
 * of bicycle traffic for use in planning and appraisal.
 * 
 * The default downhill speed is limited to 35 km/h. The value is scaled for each person
 * based on age, gender and scatter.
 * 
 * On steep links, a person is faster by walking than by biking. For such links, the
 * walk travel time is returned.
 */
public class BikeTravelTime extends WalkTravelTime {
	
	/*
	 * Cache variables for each thread accessing the object separately.
	 */
	/*package*/ private final ThreadLocal<Double> personBikeSpeedCache;
	/*package*/ private final ThreadLocal<Double> maxPersonBikeSpeedCache;
	/*package*/ private final ThreadLocal<Double> personUphillFactorCache;
	/*package*/ private final ThreadLocal<Double> personDownhillFactorCache;
	
	/*
	 * If the set reference speed does not match the default reference speed,
	 * the up- and downhill factors are scaled accordingly. 
	 */
	private final double defaultReferenceSpeed = 6.01;	// [m/s]
	
	private final double referenceBikeSpeed;		// 6.01 according to Prakin and Rotheram
	private final double maxBikeSpeed = 35.0 / 3.6;	// according to Parkin and Rotheram
	
	private final double downhillFactor = 0.2379;	// 0%..-15%
	private final double uphillFactor = -0.4002;	// 0%..12%
	
	public BikeTravelTime(PlansCalcRouteConfigGroup plansCalcGroup, Map<Id<Link>, Double> linkSlopes) {
		super(plansCalcGroup, linkSlopes);
		
		this.referenceBikeSpeed = plansCalcGroup.getTeleportedModeSpeeds().get(TransportMode.bike);
		
		this.personBikeSpeedCache = new ThreadLocal<>();
		this.maxPersonBikeSpeedCache = new ThreadLocal<>();
		this.personUphillFactorCache = new ThreadLocal<>();
		this.personDownhillFactorCache = new ThreadLocal<>();
	}
	
	public BikeTravelTime(PlansCalcRouteConfigGroup plansCalcGroup) {
		this(plansCalcGroup, null);
	}


	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		setPerson(person);
		double walkTravelTime = super.getLinkTravelTime(link, time, person, vehicle);
		
		double slope = super.getSlope(link);
		double slopeShift = getSlopeShift(slope);
		
//		double linkSpeed = this.personBikeSpeed + slopeShift;
		double linkSpeed = this.personBikeSpeedCache.get() + slopeShift;
		
		// limit min and max speed
//		if (linkSpeed > maxPersonBikeSpeed) linkSpeed = maxPersonBikeSpeed;
		if (linkSpeed > this.maxPersonBikeSpeedCache.get()) linkSpeed = this.maxPersonBikeSpeedCache.get();
		else if (linkSpeed < 0.0) linkSpeed = Double.MIN_VALUE;
		
		double bikeTravelTime = link.getLength() / linkSpeed;
		return Math.min(walkTravelTime, bikeTravelTime);
	}
	
	/*
	 * It is assumed, that there is a linear relation between speed and slope.
	 * The returned values shifts the speed on a flat area.
	 * E.g. speed(10%) = speed(0%) + slopeShift(10%)
	 */
	/*package*/ double getSlopeShift(double slope) {
		if (slope > 0.0) return this.personUphillFactorCache.get() * slope;
		else return this.personDownhillFactorCache.get() * slope;
	}
	
	@Override
    void setPerson(Person person) {
		/* 
		 * Only recalculate the person's speed factor if the person has 
		 * changed. This check has to be performed before super.setPerson(...)
		 * is called because there the personId is already updated!
		 */
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
//			this.personCache.set(person);	// set in the super-class
//			this.personFactorCache.set(personFactor);	// set in the super-class
			this.personBikeSpeedCache.set(this.referenceBikeSpeed * personFactor);
			this.maxPersonBikeSpeedCache.set(maxBikeSpeed * personFactor);
			this.personUphillFactorCache.set(uphillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed);
			this.personDownhillFactorCache.set(downhillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed);
			super.setPerson(person);
			return;
		}
		
		super.setPerson(person);
		
//		this.personBikeSpeed = this.referenceBikeSpeed * this.personFactor;
//		this.maxPersonBikeSpeed = maxBikeSpeed * personFactor;
//		this.personUphillFactor = uphillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed;
//		this.personDownhillFactor = downhillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed;

		double personFactor = this.personFactorCache.get();
		this.personBikeSpeedCache.set(this.referenceBikeSpeed * personFactor);
		this.maxPersonBikeSpeedCache.set(maxBikeSpeed * personFactor);
		this.personUphillFactorCache.set(uphillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed);
		this.personDownhillFactorCache.set(downhillFactor * personFactor * referenceBikeSpeed / defaultReferenceSpeed);
	}
}
