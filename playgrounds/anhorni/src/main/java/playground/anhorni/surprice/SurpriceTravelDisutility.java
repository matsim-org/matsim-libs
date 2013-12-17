/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

import playground.anhorni.surprice.scoring.Params;


/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author anhorni
 */
public class SurpriceTravelDisutility implements TravelDisutility {

	protected final TravelTime timeCalculator;
	private String day;
	private AgentMemories memories;
	private ObjectAttributes preferences;
	
	private final static Logger log = Logger.getLogger(SurpriceTravelDisutility.class);
	
	public SurpriceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, 
			String day, AgentMemories memories, ObjectAttributes preferences) {
		this.day = day;
		this.memories = memories;
		this.timeCalculator = timeCalculator;
		this.preferences = preferences;
	}
	
	/*
	 * link travel disutility is only used for routing and not mode choice (!) in a toll scenario. 
	 * For routing only, the trip constants can (luckily) be neglected. They are added by SurpriceTravelDisutilityIncludingToll
	 */

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double dudm = (Double)this.preferences.getAttribute(person.getId().toString(), "dudm");
		String mode = null;;
		String purpose = "undef";
		LegImpl leg = null;
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				leg = (LegImpl)pe;
				if (time < leg.getArrivalTime() && time > leg.getDepartureTime()) {
					mode = leg.getMode();
					purpose = plan.getNextActivity(leg).getType();
					break;
				}
			}
		}			
		Params params = new Params();
		params.setParams(purpose, mode, this.memories.getMemory(plan.getPerson().getId()), this.day, leg.getDepartureTime());
		
		double beta_TD = params.getBeta_TD();
		double beta_TT = params.getBeta_TT();
			
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);		
		double distance = link.getLength();
		
		double tmpScore = beta_TT * travelTime + beta_TD * distance;
		double distanceCostFactor = params.getDistanceCostFactor(); // [EUR / m]

		tmpScore += dudm * (distanceCostFactor * distance);
		return (tmpScore * -1.0); // disutility needs to be positive!
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {		
		log.error("this one should not be used :( ");
		System.exit(99);
		return 0.0;
	}
}
