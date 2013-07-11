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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
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
    
    private double income;
    private double avgIncome;
        
    
    public SurpriceLegScoringFunction(final CharyparNagelScoringParameters params, Network network, AgentMemory memory, 
    		String day, PersonImpl person, double income, double avgIncome) {
		this.params = params;
        this.network = network;
        this.memory = memory;
        this.day = day;    	
    	this.person = person;
    	
    	this.income = income;
    	this.avgIncome = avgIncome;
        
		this.reset();		
	}
        	
	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
		this.person.getCustomAttributes().put(day + ".legScore", null);
		this.person.getCustomAttributes().put(day + ".legMonetaryCosts", null);
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
		
		String purpose = ((PlanImpl)this.person.getSelectedPlan()).getNextActivity(leg).getType();	
		
		boolean mLag_purpose = false;
		boolean mLag_time = false;		
		// lag effects for tue - sun: 
		if (!this.day.equals("mon")) {				
			mLag_purpose = this.memory.getLagPurpose(departureTime, purpose, day, leg.getMode());
			mLag_time = this.memory.getLagTime(departureTime, purpose, day, leg.getMode());
		}	
		
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
				
		double beta_TC = 0.0;
		double beta_TT = 0.0;
		double lambda_I_TC = 0.0;
		double lambda_D_TT;
		double lambda_D_TC;
		
		double lagP = 0.0;
		double lagT = 0.0;
		
		double asc = 0.0;
		double distance = getDistance(leg.getRoute());
		
		// ============= car, other, unkown, pax, mtb ================================	
		if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home")) {
			beta_TC = Surprice.beta_TC_car_com;
			beta_TT = Surprice.beta_TT_car_com;
			lambda_I_TC = Surprice.lambda_I_TC_car_com;
//			lambda_D_TT = Surprice.lambda_D_TT_car_com;
//			lambda_D_TC = Surprice.lambda_D_TC_car_com;
		} else if (purpose.equals("shop")) {
			beta_TC = Surprice.beta_TC_car_shp;
			beta_TT = Surprice.beta_TT_car_shp;
			lambda_I_TC = Surprice.lambda_I_TC_car_shp;
//			lambda_D_TT = Surprice.lambda_D_TT_car_shp;
//			lambda_D_TC = Surprice.lambda_D_TC_car_shp;
		} else if (purpose.equals("leisure")) {
			beta_TC = Surprice.beta_TC_car_lei;
			beta_TT = Surprice.beta_TT_car_lei;
			lambda_I_TC = Surprice.lambda_I_TC_car_lei;
//			lambda_D_TT = Surprice.lambda_D_TT_car_lei;
//			lambda_D_TC = Surprice.lambda_D_TC_car_lei;
		} 
		lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_car;
		lagT = (mLag_time? 1 : 0) * Surprice.lag_time_car;
		asc = Surprice.constantCar;	
							
		// ============= PT =======================================================
		if (TransportMode.pt.equals(leg.getMode())) {			
			if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home")) {
				beta_TC = Surprice.beta_TC_pt_com;
				beta_TT = Surprice.beta_TT_pt_com;
				lambda_I_TC = Surprice.lambda_I_TC_pt_com;
//				lambda_D_TT = Surprice.lambda_D_TT_pt_com;
//				lambda_D_TC = Surprice.lambda_D_TC_pt_com;
			} else if (purpose.equals("shop")) {
				beta_TC = Surprice.beta_TC_pt_shp;
				beta_TT = Surprice.beta_TT_pt_shp;
				lambda_I_TC = Surprice.lambda_I_TC_pt_shp;
//				lambda_D_TT = Surprice.lambda_D_TT_pt_shp;
//				lambda_D_TC = Surprice.lambda_D_TC_pt_shp;
			} else if (purpose.equals("leisure")) {
				beta_TC = Surprice.beta_TC_pt_lei;
				beta_TT = Surprice.beta_TT_pt_lei;
				lambda_I_TC = Surprice.lambda_I_TC_pt_lei;
//				lambda_D_TT = Surprice.lambda_D_TT_pt_lei;
//				lambda_D_TC = Surprice.lambda_D_TC_pt_lei;
			} 
			lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_pt;
			lagT = (mLag_time? 1 : 0) * Surprice.lag_time_pt;
			asc = Surprice.constantPt;
		
		// ============= slm =======================================================
		} else if (TransportMode.bike.equals(leg.getMode()) || TransportMode.bike.equals(leg.getMode())) {
			if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home")) {
				beta_TC = Surprice.beta_TC_slm_com;
				beta_TT = Surprice.beta_TT_slm_com;
				lambda_I_TC = Surprice.lambda_I_TC_slm_com;
//				lambda_D_TT = Surprice.lambda_D_TT_slm_com;
//				lambda_D_TC = Surprice.lambda_D_TC_slm_com;
			} else if (purpose.equals("shop")) {
				beta_TC = Surprice.beta_TC_slm_shp;
				beta_TT = Surprice.beta_TT_slm_shp;
				lambda_I_TC = Surprice.lambda_I_TC_slm_shp;
//				lambda_D_TT = Surprice.lambda_D_TT_slm_shp;
//				lambda_D_TC = Surprice.lambda_D_TC_slm_shp;
			} else if (purpose.equals("leisure")) {
				beta_TC = Surprice.beta_TC_slm_lei;
				beta_TT = Surprice.beta_TT_slm_lei;
				lambda_I_TC = Surprice.lambda_I_TC_slm_lei;
//				lambda_D_TT = Surprice.lambda_D_TT_slm_lei;
//				lambda_D_TC = Surprice.lambda_D_TC_slm_lei;
			} 
			lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_slm;
			lagT = (mLag_time? 1 : 0) * Surprice.lag_time_slm;
			asc = Surprice.constantPt;
		} 		
		double tmpScore = beta_TT * travelTime + beta_TC * distance * Surprice.costPerKm * Math.pow((this.income / this.avgIncome), lambda_I_TC);
		tmpScore += lagP + lagT;
		
		// TODO: add epsilon
		
		double prevValLag = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legScoreLag") != null) {
			prevValLag = (Double)this.person.getCustomAttributes().get(day + ".legScoreLag");
		}
		person.getCustomAttributes().put(day + ".legScoreLag", prevValLag + tmpScore);
		
		double prevVal = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legScore") != null) {
			prevVal = (Double)this.person.getCustomAttributes().get(day + ".legScore");
		}
		person.getCustomAttributes().put(day + ".legScore", prevVal + tmpScore);
		
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
