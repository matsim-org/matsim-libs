/* *********************************************************************** *
 * project: org.matsim.*
 * SocializingScoringFunction.java
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
package playground.jhackney.socialnetworks.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;

import playground.jhackney.SocNetConfigGroup;

/**
 * A special {@linkplain ScoringFunction scoring function} that takes the face to face encounters
 * between the agents into account when calculating the score of a plan.
 *
 * @author jhackney
 */
public class PlanSocScoringFunction implements ScoringFunction{

	static final private Logger log = Logger.getLogger(PlanSocScoringFunction.class);
	private final ScoringFunction scoringFunction;
	private final Plan plan;
	private final TrackActsOverlap spatialScorer;
	private final String factype;

	private double friendFoeRatio=0.;
	private double nFriends=0;
	private double timeWithFriends=0;

	private SocNetConfigGroup socnetConfig;

	private double betaFriendFoe = Double.parseDouble(socnetConfig.getBeta1());
	private double betaNFriends= Double.parseDouble(socnetConfig.getBeta2());
	private double betaLogNFriends= Double.parseDouble(socnetConfig.getBeta3());
	private double betaTimeWithFriends= Double.parseDouble(socnetConfig.getBeta4());

	public PlanSocScoringFunction(final Plan plan, final ScoringFunction scoringFunction, String factype, TrackActsOverlap spatialScorer, SocNetConfigGroup snConfig) {
//		this.paidToll = paidToll;
		this.socnetConfig = snConfig;
		this.scoringFunction = scoringFunction;
		this.plan = plan;
		this.spatialScorer=spatialScorer;
		this.factype=factype;
		if((this.betaNFriends!= 0) && (this.betaLogNFriends!=0)){
			log.warn("Utility function values linear AND log number of Friends in spatial meeting");
		}

	}

	/**
	 * Totals the act scores, including socializing during acts, for the entire plan
	 *
	 * @see org.matsim.core.scoring.ScoringFunction#finish()
	 */
	public void finish() {
		this.scoringFunction.finish();


		LinkedHashMap<Activity,ArrayList<Double>> actStats = this.spatialScorer.calculateTimeWindowActStats(plan);
//		ArrayList<Double> stats = this.spatialScorer.calculateTimeWindowStats(plan);
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if(act.getType().equals(factype)){
//				this.friendFoeRatio+=stats.get(0);
//				this.nFriends+=stats.get(1);
//				this.timeWithFriends+=stats.get(2);
					this.friendFoeRatio+=actStats.get(act).get(0);
					this.nFriends+=actStats.get(act).get(1);
					this.timeWithFriends+=actStats.get(act).get(2);
				}
			}
		}

//		log.info("Person "+plan.getPerson().getId()+" meets nFriends "+this.nFriends+" for "+this.timeWithFriends+" at activity "+ factype);
	}

	public void agentStuck(final double time) {
		this.scoringFunction.agentStuck(time);
	}

	public void addMoney(final double amount) {
		this.scoringFunction.addMoney(amount);
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

	public void startActivity(final double time, final Activity act) {
		this.scoringFunction.startActivity(time, act);
	}

	public void startLeg(final double time, final Leg leg) {
		this.scoringFunction.startLeg(time, leg);
	}

}
