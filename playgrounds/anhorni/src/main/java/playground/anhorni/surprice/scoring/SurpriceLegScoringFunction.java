/* *********************************************************************** *
 * project: org.matsim.*
 * LaggedLegScoringFunction.java
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

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.RouteUtils;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.Surprice;

public class SurpriceLegScoringFunction implements LegScoring, BasicScoring {

	protected double score;
	private PersonImpl person;
	private double lastTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
    protected Network network;
    
    private String day;
    private AgentMemory memory;
    private Config config;
        
    private double constantCar;
    private double constantPt;
    private double constantBike;
    private double constantWalk;  
    
    public SurpriceLegScoringFunction(final CharyparNagelScoringParameters params, Network network, final Config config, AgentMemory memory, 
    		String day, PersonImpl person) {
		this.params = params;
        this.network = network;
        this.config = config;
        
        this.memory = memory;
        this.day = day;
        
        this.constantCar = this.params.constantCar;
    	this.constantPt = this.params.constantPt;
    	this.constantBike = this.params.constantBike;
    	this.constantWalk = this.params.constantWalk; 
    	
    	this.person = person;
        
		this.reset();		
	}
        
    private void adaptCoefficientsLagged() {    	
    	double fLagged = (Double) this.person.getCustomAttributes().get(day + ".fLagged");
    	
		// adapt for tue - sun: 
		if (!this.day.equals("mon")) {
			String mode = this.memory.getMainModePreviousDay(this.day);
			
			if (mode.equals("car")) {
				this.constantCar = this.params.constantCar * fLagged + 2 * (1.0 - fLagged);
			} else if (mode.equals("pt")) {
				this.constantPt = this.params.constantPt * fLagged + 2 * (1.0 - fLagged);
			} else if (mode.equals("bike")) {
				this.constantBike = this.params.constantBike * fLagged + 2 * (1.0 - fLagged);			
			} else if (mode.equals("walk")) {
				this.constantWalk = this.params.constantWalk * fLagged + 2 * (1.0 - fLagged);
			}
			else {
				// do nothing
			}
			this.person.getCustomAttributes().put(day + ".constantCar", this.constantCar);
			this.person.getCustomAttributes().put(day + ".constantPt", this.constantPt);
			this.person.getCustomAttributes().put(day + ".constantBike", this.constantBike);
			this.person.getCustomAttributes().put(day + ".constantWalk", this.constantWalk);
		}
	}
	
	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
		this.person.getCustomAttributes().put(day + ".legScore", null);
		this.person.getCustomAttributes().put(day + ".legMonetaryCosts", null);
		
		this.person.getCustomAttributes().put(day + ".constantCar", null);
		this.person.getCustomAttributes().put(day + ".constantPt", null);
		this.person.getCustomAttributes().put(day + ".constantBike", null);
		this.person.getCustomAttributes().put(day + ".constantWalk", null);
		
		this.constantCar = this.params.constantCar;
    	this.constantPt = this.params.constantPt;
    	this.constantBike = this.params.constantBike;
    	this.constantWalk = this.params.constantWalk;
	}

	@Override
	public void startLeg(final double time, final Leg leg) {
		assert leg != null;
		this.lastTime = time;
		this.currentLeg = leg;
	}

	@Override
	public void endLeg(final double time) {
		handleLeg(this.currentLeg, time);
		this.lastTime = time;
	}

	private void handleLeg(Leg leg, final double time) {
		this.score += calcLegScore(this.lastTime, time, leg);
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {	
		if (Boolean.parseBoolean(this.config.findParam(Surprice.SURPRICE_RUN, "useLaggedVars"))) {
			this.adaptCoefficientsLagged();			
		}		
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		double tmpMonetaryCosts = 0.0;
		
		// ============= CAR =======================================================
		if (TransportMode.car.equals(leg.getMode())) {			
			Route route = leg.getRoute();
			double dist = 0.0; // distance in meters
			dist = getDistance(route);
						
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + 
			this.params.monetaryDistanceCostRateCar * this.params.marginalUtilityOfMoney * dist;
			tmpScore += this.constantCar;
			
			tmpMonetaryCosts += this.params.monetaryDistanceCostRateCar * this.params.marginalUtilityOfMoney * dist;
						
		// ============= CAR =======================================================
		} else if (TransportMode.pt.equals(leg.getMode())) {			
			Route route = leg.getRoute();
			double dist = 0.0; // distance in meters
			dist = getDistance(route);
			
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT_s + 
			this.params.monetaryDistanceCostRatePt * this.params.marginalUtilityOfMoney * dist;
			tmpScore += this.constantPt;	
			
			tmpMonetaryCosts += this.params.monetaryDistanceCostRatePt * this.params.marginalUtilityOfMoney * dist;
			
		} else if (TransportMode.walk.equals(leg.getMode()) || TransportMode.transit_walk.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s;
			tmpScore +=  this.constantWalk;
		} else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
			tmpScore += this.constantBike;
		} else {
			double dist = 0.0; // distance in meters
			Route route = leg.getRoute();
			dist = getDistance(route);
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + 
			this.params.monetaryDistanceCostRateCar * this.params.marginalUtilityOfMoney * dist;
			tmpScore += this.constantCar;
			
			tmpMonetaryCosts += this.params.monetaryDistanceCostRateCar * this.params.marginalUtilityOfMoney * dist;
		}
		double prevVal = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legScore") != null) {
			prevVal = (Double)this.person.getCustomAttributes().get(day + ".legScore");
		}
		person.getCustomAttributes().put(day + ".legScore", prevVal + tmpScore);
		
		prevVal = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legMonetaryCosts") != null) {
			prevVal = (Double) this.person.getCustomAttributes().get(day + ".legMonetaryCosts");
		}
		person.getCustomAttributes().put(day + ".legMonetaryCosts" , prevVal + tmpMonetaryCosts);
		return tmpScore;
	}

	private double getDistance(Route route) {
		double dist;
		if (route instanceof NetworkRoute) {
			dist =  RouteUtils.calcDistance((NetworkRoute) route, network);
		} else {
			dist = route.getDistance();
		}
		return dist;
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		return this.score;
	}
}
