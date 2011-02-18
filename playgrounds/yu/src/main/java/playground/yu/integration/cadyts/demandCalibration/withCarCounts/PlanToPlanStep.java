/* *********************************************************************** *
w * project: org.matsim.*
 * PlanToPlanStep.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.yu.analysis.PlanModeJudger;
import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;

public class PlanToPlanStep {

	private final PlanBuilder<Link> planStepFactory = new PlanBuilder<Link>();
	private final TravelTime ttime;
	private Network net;

	private static final int maxTime = 24 * 3600 - 1;

	/**
	 * is only called in {@code StrategyManager} and its subclass
	 * 
	 * @param ttime
	 * @param net
	 */
	public PlanToPlanStep(final TravelTime ttime, final Network net) {
		this.ttime = ttime;
		this.net = net;
	}

	private boolean foundPT = false;

	public final void convert(final PlanImpl plan) {

		if (PlanModeJudger.usePt(plan)) {
			if (!foundPT) {
				System.out.println("BSE: found a PT plan");
				foundPT = true;
			}
			planStepFactory.reset();
			return;
		}

		double time = plan.getFirstActivity().getEndTime();
		if (time > maxTime) {
			return;
		}
		for (int i = 1; i < plan.getPlanElements().size() - 1; i += 2) {
			// that's the leg we're gonna handle now
			LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
			ActivityImpl fromAct = (ActivityImpl) plan.getPreviousActivity(leg);
			ActivityImpl toAct = (ActivityImpl) plan.getNextActivity(leg);

			// entry
			planStepFactory.addEntry(net.getLinks().get(fromAct.getLinkId()),
					(int) time/* endTime of first activity */);
			// turns for all links
			for (Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
				Link link = net.getLinks().get(linkId);
				planStepFactory.addTurn(link, (int) time);
				time += ttime.getLinkTravelTime(link, time);
				if (time > maxTime) {
					planStepFactory.addExit(maxTime);
					return;
				}
			}
			// last turn to destination link
			planStepFactory.addTurn(net.getLinks().get(toAct.getLinkId()),
					(int) time);
			time += ttime.getLinkTravelTime(net.getLinks().get(
					toAct.getLinkId()), time);
			if (time > maxTime) {
				planStepFactory.addExit(maxTime);
				return;
			}
			// regular exit at end of leg
			planStepFactory.addExit((int) time);
			// advance time
			if (toAct.getMaximumDuration() != Time.UNDEFINED_TIME
					&& toAct.getEndTime() != Time.UNDEFINED_TIME) {
				time = Math.max(time, Math.min(time + toAct.getMaximumDuration(),
						toAct.getEndTime()));
			} else if (toAct.getMaximumDuration() != Time.UNDEFINED_TIME) {
				time += toAct.getMaximumDuration();
			} else {
				time = Math.max(time, toAct.getEndTime());
			}
		} // for-loop: handle next leg
	}

	public final Plan<Link> getPlanSteps() {
		final Plan<Link> steps = planStepFactory.getResult();
		planStepFactory.reset();
		return steps;
	}

}
