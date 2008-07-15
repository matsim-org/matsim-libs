package playground.jhackney.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 *  SNScoringFriendFoeRatio.java
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

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;

/**
 * A special {@linkplain ScoringFunction scoring function} that takes the paid tolls by the agents into
 * account when calculating the score of a plan.
 *
 * @author mrieser
 */
public class SNScoringFriendFoeRatio implements ScoringFunction{

//	private final CalcPaidToll paidToll;
	private final ScoringFunction scoringFunction;
	private final Plan plan;
	private final SpatialScorer spatialScorer;
	private final String factype;

//	private double toll = 0.0;
	private double socialPlanAdjustment=0.;

	public SNScoringFriendFoeRatio(final Plan plan, final ScoringFunction scoringFunction, String factype, SpatialScorer spatialScorer) {
//		this.paidToll = paidToll;
		this.scoringFunction = scoringFunction;
		this.plan = plan;
		this.spatialScorer=spatialScorer;
		this.factype=factype;
//		System.out.println("#### SNSCoring function initialized");
	}

	public void finish() {
		this.scoringFunction.finish();
//		this.toll = this.paidToll.getAgentToll(this.person.getId().toString());
System.out.println("#######SNSCoring function finish");
		ActIterator ait = this.plan.getIteratorAct();
		while(ait.hasNext()){
			Act act = (Act)ait.next();
			if(act.getType().equals(factype)){
				this.socialPlanAdjustment+=this.spatialScorer.scoreFriendtoFoeInTimeWindow(plan);
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
		return this.scoringFunction.getScore() + 10.*this.socialPlanAdjustment;
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
