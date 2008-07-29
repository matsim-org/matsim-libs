package org.matsim.socialnetworks.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 *  SocializingScoringFunction.java
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

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;

/**
 * A special {@linkplain ScoringFunction scoring function} that takes the face to face encounters
 * between the agents into account when calculating the score of a plan.
 *
 * @author jhackney
 */
public class SocializingScoringFunction implements ScoringFunction{

	static final private Logger log = Logger.getLogger(SocializingScoringFunction.class);
	private final ScoringFunction scoringFunction;
	private final Plan plan;
	private final SpatialScorer spatialScorer;
	private final String factype;

//	private double toll = 0.0;
	private double friendFoeRatio=0.;
	private double nFriends=0;
	private double timeWithFriends=0;
	
	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();

	private double betaFriendFoe = Double.parseDouble(socnetConfig.getBeta1());
	private double betaNFriends= Double.parseDouble(socnetConfig.getBeta2());
	private double betaLogNFriends= Double.parseDouble(socnetConfig.getBeta3());
	private double betaTimeWithFriends= Double.parseDouble(socnetConfig.getBeta4());

	public SocializingScoringFunction(final Plan plan, final ScoringFunction scoringFunction, String factype, SpatialScorer spatialScorer) {
//		this.paidToll = paidToll;
		this.scoringFunction = scoringFunction;
		this.plan = plan;
		this.spatialScorer=spatialScorer;
		this.factype=factype;
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
		//TODO why does this iterate over acts here and again in spatialScorer?
		//It should sent the act AND the plan and only return the value for the act.
		//If there are more than one acts of type factype in the plan, this is
		//counting the score once per instance of this activity!!!  JH
		while(ait.hasNext()){
			Act act = (Act)ait.next();
			if(act.getType().equals(factype)){
//				this.friendFoeRatio+=this.spatialScorer.calculateFriendtoFoeInTimeWindow(plan);
				this.friendFoeRatio+=this.spatialScorer.calculateTimeWindowStats(plan).get(0);
				this.nFriends+=this.spatialScorer.calculateTimeWindowStats(plan).get(1);
				this.timeWithFriends+=this.spatialScorer.calculateTimeWindowStats(plan).get(2);
			}
		}
	}

	public void agentStuck(final double time) {
		this.scoringFunction.agentStuck(time);
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
		betaTimeWithFriends * timeWithFriends;
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
