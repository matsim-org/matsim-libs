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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.LegScoring;
import org.matsim.core.utils.misc.RouteUtils;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.Surprice;

/**
 * anhorni
 */
public class LaggedLegScoringFunction implements LegScoring, BasicScoring {

	protected double score;
	private double lastTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
    protected Network network;
    
    private String day;
    private AgentMemory memory;
    private double income;
    private Config config;
    
    private double constantCar;
    private double constantPt;
    private double constantBike;
    private double constantWalk;    

    public LaggedLegScoringFunction(final CharyparNagelScoringParameters params, Network network, final Config config, AgentMemory memory, String day, double income) {
		this.params = params;
        this.network = network;
        
        this.memory = memory;
        this.day = day;
        this.income = income;
        
        this.config = config;
        
		this.reset();		
		this.adaptCoefficients();
	}
    
    private void adaptCoefficients() {
    	this.constantCar = this.params.constantCar;
    	this.constantPt = this.params.constantPt;
    	this.constantBike = this.params.constantBike;
    	this.constantWalk = this.params.constantWalk;  
    	
    	double f = Double.parseDouble(this.config.findParam(Surprice.SURPRICE_RUN, "f"));
    	
		// adapt for Tue - Sun: 
		if (!this.day.equals("Mon")) {
			String mode = this.memory.getMainModePreviousDay(this.day);
			
			if (mode.equals("car")) {
				this.constantCar *= f;
			} else if (mode.equals("pt")) {
				this.constantPt *= f;
			} else if (mode.equals("bike")) {
				this.constantBike *= f;			
			} else if (mode.equals("walk")) {
				this.constantWalk *= f;
			}
			else {
				// do nothing
			}
		}
		this.adaptCoefficientsAccordingToIncome();
	}
	
	private void adaptCoefficientsAccordingToIncome() {		
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
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
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		if (TransportMode.car.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.constantCar;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistancePt_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT_s + this.params.marginalUtilityOfDistancePt_m * dist;
			tmpScore += this.constantPt;
		} else if (TransportMode.walk.equals(leg.getMode()) || TransportMode.transit_walk.equals(leg.getMode())) {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceWalk_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk_s + this.params.marginalUtilityOfDistanceWalk_m * dist;
			tmpScore +=  this.constantWalk;
		} else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingBike_s;
			tmpScore += this.constantBike ;
		} else {
			double dist = 0.0; // distance in meters
			if (this.params.marginalUtilityOfDistanceCar_m != 0.0) {
				Route route = leg.getRoute();
				dist = getDistance(route);
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistanceCar_m * dist;
			tmpScore += this.constantCar ;
		}
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
		return 0;
	}
}
