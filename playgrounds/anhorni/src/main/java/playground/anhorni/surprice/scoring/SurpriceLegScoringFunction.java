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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.anhorni.surprice.AgentMemory;



public class SurpriceLegScoringFunction implements LegScoring, BasicScoring {

	protected double score;
	private Person person;
	private double lastTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	private Leg currentLeg;
    protected Network network;
    
    private String day;
    private AgentMemory memory;
    private double dudm;
    
    private int legCnt = -1;        
    
    public SurpriceLegScoringFunction(final CharyparNagelScoringParameters params, Network network, AgentMemory memory, 
    		String day, Person person, double dudm) {
		this.params = params;
        this.network = network;
        this.memory = memory;
        this.day = day;    	
    	this.person = person;    	
    	this.dudm = dudm;
        
		this.reset();		
	}
        	
	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.score = INITIAL_SCORE;
		this.person.getCustomAttributes().put(day + ".legScore", null);
		this.person.getCustomAttributes().put(day + ".legMonetaryCosts", null);
		this.person.getCustomAttributes().put(day + ".legScoreLag", null);
		this.legCnt = -1;
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
		String purpose = "undef";
		this.legCnt +=2;
		int actlegIndex = 0;
		for (PlanElement pe : ((PlanImpl)this.person.getSelectedPlan()).getPlanElements()) {	
			if (pe instanceof Activity && actlegIndex == this.legCnt + 1) {	
				Activity act = (Activity)pe;
				purpose = act.getType();
				break;
			} 
			actlegIndex++;
		}
			 // let's just hope that works
		Params params = new Params();
		params.setParams(purpose, leg.getMode(), this.memory, this.day, departureTime);
				
		double beta_TD = params.getBeta_TD();
		double beta_TT = params.getBeta_TT();
		double asc = params.getAsc();
		
		double lagP = params.getLagP();
		double lagT = params.getLagT();
								
		double travelTime = arrivalTime - departureTime; // travel time in seconds
		double distance = getDistance(leg.getRoute());
				
		double tmpScore = beta_TT * travelTime + beta_TD * distance;
		tmpScore += asc;
		tmpScore += lagP + lagT;
		
		// add leg travel costs: -----------------------------------------
		// distance costs + constant
		// make them also purpose dependent?
		double constCost = params.getConstCost(); // [EUR]
		double distanceCostFactor = params.getDistanceCostFactor(); // [EUR / m]
		
		tmpScore += dudm * (distanceCostFactor * distance + constCost); 
		
		// future: add epsilon ... TODO
		
		double prevValLag = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legScoreLag") != null) {
			prevValLag = (Double)this.person.getCustomAttributes().get(day + ".legScoreLag");
		}
		person.getCustomAttributes().put(day + ".legScoreLag", prevValLag + lagP + lagT);
		
		double prevValMonetary = 0.0;
		if (this.person.getCustomAttributes().get(day + ".legMonetaryCosts") != null) {
			prevValMonetary = (Double)this.person.getCustomAttributes().get(day + ".legMonetaryCosts");
		}
		person.getCustomAttributes().put(day + ".legMonetaryCosts", prevValMonetary + dudm * (distanceCostFactor * distance + constCost));
		
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
