/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.ActivityScoring;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;

/**
 * @author mrieser
 */
public class DynusTScoringFunction implements ScoringFunction {

	private final Plan plan;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final ActivityToZoneMapping act2zoneMapping;
	private double score = Double.NaN;
	private final LegScoring legScorer;
	private final ActivityScoring actScorer;
	
	public DynusTScoringFunction(final Plan plan, final DynamicTravelTimeMatrix ttMatrix, final ActivityToZoneMapping act2zoneMapping, final LegScoring legScorer, final ActivityScoring actScorer) {
		this.plan = plan;
		this.ttMatrix = ttMatrix;
		this.act2zoneMapping = act2zoneMapping;
		this.legScorer = legScorer;
		this.actScorer = actScorer;
	}
	
	@Override
	public void handleActivity(final Activity activity) {
	}

	@Override
	public void handleLeg(final Leg leg) {
	}

	@Override
	public void agentStuck(final double time) {
	}

	@Override
	public void addMoney(final double amount) {
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		if (Double.isNaN(this.score)) {
			
			Leg prevLeg = null;
			Activity prevAct = null;
			int actIndex = 0;
			for (PlanElement pe : this.plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					prevLeg = leg;
				}
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;

					if (!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						if (prevAct == null) {
							// the first act
							actScorer.endActivity(act.getEndTime(), act);
						}
						if (prevAct != null && prevLeg != null) {
							if (prevLeg.getMode().equals(TransportMode.car)) {
								
								String[] zones = this.act2zoneMapping.getAgentActivityZones(plan.getPerson().getId());
								double tt = this.ttMatrix.getAverageTravelTime(prevAct.getEndTime(), zones[actIndex-1], zones[actIndex]);
								
								legScorer.startLeg(prevAct.getEndTime(), prevLeg);
								legScorer.endLeg(prevAct.getEndTime() + tt);
								
								actScorer.startActivity(prevAct.getEndTime() + tt, act);
								if (act.getEndTime() != Time.UNDEFINED_TIME) {
									actScorer.endActivity(act.getEndTime(), act);
								}
								
							} else if (prevLeg.getMode().equals(TransportMode.pt)) {
								// currently, only teleportation based
								double dist = CoordUtils.calcEuclideanDistance(prevAct.getCoord(), act.getCoord());
								double tt = dist / 5.0; // assume average speed of 18km/h
								
								legScorer.startLeg(prevAct.getEndTime(), prevLeg);
								legScorer.endLeg(prevAct.getEndTime() + tt);
								
								actScorer.startActivity(prevAct.getEndTime() + tt, act);
								if (act.getEndTime() != Time.UNDEFINED_TIME) {
									actScorer.endActivity(act.getEndTime(), act);
								}
							} else if (prevLeg.getMode().equals(TransportMode.walk)) {
								// currently, only teleportation based
								double dist = CoordUtils.calcEuclideanDistance(prevAct.getCoord(), act.getCoord());
								double tt = dist; // assume 1m/s = 3.6km/h
								
								legScorer.startLeg(prevAct.getEndTime(), prevLeg);
								legScorer.endLeg(prevAct.getEndTime() + tt);
								
								actScorer.startActivity(prevAct.getEndTime() + tt, act);
								if (act.getEndTime() != Time.UNDEFINED_TIME) {
									actScorer.endActivity(act.getEndTime(), act);
								}
							} else if (prevLeg.getMode().equals(TransportMode.bike)) {
								// currently, only teleportation based
								double dist = CoordUtils.calcEuclideanDistance(prevAct.getCoord(), act.getCoord());
								double tt = dist / 3.0; // assume average speed of 3m/s = 10.8km/h
								
								legScorer.startLeg(prevAct.getEndTime(), prevLeg);
								legScorer.endLeg(prevAct.getEndTime() + tt);
								
								actScorer.startActivity(prevAct.getEndTime() + tt, act);
								if (act.getEndTime() != Time.UNDEFINED_TIME) {
									actScorer.endActivity(act.getEndTime(), act);
								}
							}
						}
						
						prevAct = act;
						actIndex++;
					}
					prevLeg = null;
				}
			}

			this.actScorer.finish();
			this.legScorer.finish();
			this.score = this.actScorer.getScore() + this.legScorer.getScore();
		}
		return this.score;
	}

	public void reset() {
		this.score = Double.NaN;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
