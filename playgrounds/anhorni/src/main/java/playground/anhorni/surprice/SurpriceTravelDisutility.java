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
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
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
	private final TollRouterBehaviour tollCostHandler;
	private final RoadPricingScheme scheme;
	private boolean doRoadPricing = false;
	
	private final static Logger log = Logger.getLogger(SurpriceTravelDisutility.class);
	
	public SurpriceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, 
			String day, AgentMemories memories, ObjectAttributes preferences, final RoadPricingScheme scheme, boolean doRoadPricing) {
		this.day = day;
		this.memories = memories;
		this.timeCalculator = timeCalculator;
		this.preferences = preferences;
		this.scheme = scheme;
		this.doRoadPricing = doRoadPricing;
		
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
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
		double distanceCostFactor = params.getDistanceCostFactor(); // [EUR / m]
		
		double tmpScore = beta_TT * travelTime + beta_TD * distance;
		tmpScore += dudm * (distanceCostFactor * distance);
		
		if (doRoadPricing) tmpScore += - dudm * this.tollCostHandler.getTollCost(link, time, person, vehicle); // toll disutility		
		return (tmpScore * -1.0); // disutility needs to be positive!
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {		
		log.error("this one should not be used :( ");
		System.exit(99);
		return 0.0;
	}
	
	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time, Person person, Vehicle vehicle);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			Cost cost_per_m = SurpriceTravelDisutility.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount * link.getLength();
		}
	}

	private static int wrnCnt2 = 0 ;
	
	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			RoadPricingSchemeImpl.Cost cost = SurpriceTravelDisutility.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility.
			 */
			if ( wrnCnt2 < 1 ) {
				wrnCnt2 ++ ;
				Logger.getLogger(this.getClass()).warn("at least here, the area toll does not use the true toll value. " +
						"This may work anyways, but without more explanation it is not obvious to me.  kai, mar'11") ;
			}
			return 1000;
		}
	}

	/*package*/ class CordonTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			RoadPricingSchemeImpl.Cost cost = SurpriceTravelDisutility.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}
	
	class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			Cost cost_per_m = SurpriceTravelDisutility.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount;
		}
	}

}
