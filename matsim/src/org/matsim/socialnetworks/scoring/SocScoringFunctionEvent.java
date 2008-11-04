package org.matsim.socialnetworks.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 *  SocializingScoringFunction2.java
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

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;


/**
 * A special {@linkplain ScoringFunction scoring function} that takes the face to face encounters
 * between the agents into account when calculating the score of a plan.
 *
 * @author jhackney
 */
public class SocScoringFunctionEvent implements ScoringFunction{

	static final private Logger log = Logger.getLogger(SocScoringFunctionPlan.class);
	private final ScoringFunction scoringFunction;
	private final Plan plan;
//	private final TrackEventsOverlap teo;
	private final Hashtable<Act,ArrayList<Double>> actStats;
	private final String factype;

	private double friendFoeRatio=0.;
	private double nFriends=0;
	private double timeWithFriends=0;

	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();

	private double betaFriendFoe = Double.parseDouble(socnetConfig.getBeta1());
	private double betaNFriends= Double.parseDouble(socnetConfig.getBeta2());
	private double betaLogNFriends= Double.parseDouble(socnetConfig.getBeta3());
	private double betaTimeWithFriends= Double.parseDouble(socnetConfig.getBeta4());

	public SocScoringFunctionEvent(final Plan plan, final ScoringFunction scoringFunction, String factype, final Hashtable<Act,ArrayList<Double>> actStats) {
//		this.paidToll = paidToll;
		this.scoringFunction = scoringFunction;
		this.plan = plan;
//		this.teo=teo;
		this.factype=factype;
		this.actStats=actStats;
		if(this.betaNFriends!= 0 && this.betaLogNFriends!=0){
			log.warn("Utility function values linear AND log number of Friends in spatial meeting");
		}

	}

	/**
	 * Totals the act scores, including socializing during acts, for the entire plan
	 *
	 * @see org.matsim.scoring.ScoringFunction#finish()
	 */
	public void finish() {
		this.scoringFunction.finish();

		ActIterator ait = this.plan.getIteratorAct();

//		Hashtable<Act,ArrayList<Double>> actStats = this.spatialScorer.calculateTimeWindowActStats(plan);
//		Hashtable<Act,ArrayList<Double>> actStats = CompareTimeWindows.calculateTimeWindowEventActStats(teo.getTimeWindowMap());
//		ArrayList<Double> stats = this.spatialScorer.calculateTimeWindowStats(plan);
		while(ait.hasNext()){
			Act act = (Act)ait.next();
			if(act.getType().equals(factype)){
//				this.friendFoeRatio+=stats.get(0);
//				this.nFriends+=stats.get(1);
//				this.timeWithFriends+=stats.get(2);
				this.friendFoeRatio+=actStats.get(act).get(0);
				this.nFriends+=actStats.get(act).get(1);
				this.timeWithFriends+=actStats.get(act).get(2);
			}
		}
//		log.info("SSTEST Person "+plan.getPerson().getId()+" meets nFriends "+this.nFriends+" for "+this.timeWithFriends+" at activity "+ factype);
	}

	public void agentStuck(final double time) {
		this.scoringFunction.agentStuck(time);
	}

	public void addUtility(final double amount) {
		this.scoringFunction.addUtility(amount);
	}

	public void endActivity(final double time) {
		this.scoringFunction.endActivity(time);
	}

	public void endLeg(final double time) {
		this.scoringFunction.endLeg(time);
	}

	public double getScore() {
//		log.info("FFR "+this.friendFoeRatio+" NF "+this.nFriends+" LNF "+Math.log(this.nFriends+1));

		return this.scoringFunction.getScore() +
		betaFriendFoe*this.friendFoeRatio+
		betaNFriends * this.nFriends +
		betaLogNFriends * Math.log(this.nFriends+1) +
		betaTimeWithFriends * Math.log(this.timeWithFriends/3600.+1);
	}

	public void reset() {
		this.scoringFunction.finish();
	}

	public void startActivity(final double time, final Act act) {
		this.scoringFunction.startActivity(time, act);
	}

	public void startLeg(final double time, final Leg leg) {
		this.scoringFunction.startLeg(time, leg);
	}
}
