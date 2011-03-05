/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;
import cadyts.demand.PlanStep;

public class PlanToPlanStep {

	private final PlanBuilder<Link> planStepFactory = new PlanBuilder<Link>();
	private final TravelTime ttime;
	private Network net;

	private static final int maxTime = 24 * 3600 - 1;

	public PlanToPlanStep(final TravelTime ttime, final Network net) {
		this.ttime = ttime;
		this.net = net;
	}

	private boolean foundPT = false, write = false;

	/* package */public final void convert(final PlanImpl plan) {

		// //////////////////////////////////////////
		// System.out.println("BSETEST PlanToPlanStep print test!!!");
		PlanMutateTimeAllocation toWrite = (PlanMutateTimeAllocation) plan
				.getCustomAttributes().get("pass5");
		write = toWrite != null;
		if (write) {
			Person person = plan.getPerson();
			System.out.println("BSETEST PlanToPlanStep\tperson\t"
					+ person.getId() + "\tplan index\t"
					+ person.getPlans().indexOf(plan));
		}
		// /////////////////////////////////////////////
		String planType = plan.getType();
		if (planType != null) {
			if (PlanImpl.DeprecatedConstants.PT.equals(planType)) {
				if (!foundPT) {
					System.out.println("BSE: found a PT plan");
					foundPT = true;
				}
				planStepFactory.reset();
				return;
			}
		}
		double time = plan.getFirstActivity().getEndTime();
		if (time > maxTime) {
			return;
		}

		// ////////////////////////////
		if (write) {
			System.out.print("BSETEST PlanToPlanStep time from matsim");
		}
		// ////////////////////////////////

		for (int i = 1; i < plan.getPlanElements().size() - 1; i += 2) {

			// that's the leg we're gonna handle now
			LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
			ActivityImpl fromAct = (ActivityImpl) plan.getPreviousActivity(leg);
			ActivityImpl toAct = (ActivityImpl) plan.getNextActivity(leg);

			if (TransportMode.car.equals(leg.getMode())) {
				// entry
				planStepFactory.addEntry(net.getLinks().get(
						fromAct.getLinkId()), (int) time);

				// turns for all links
				for (Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					Link link = net.getLinks().get(linkId);
					planStepFactory.addTurn(link, (int) time);

					// ////////////////////////////
					if (write) {
						System.out.print("\t" + time);
					}
					// ////////////////////////////////

					time += ttime.getLinkTravelTime(link, time);
					if (time > maxTime) {
						planStepFactory.addExit(maxTime);
						return;
					}
				}
				// last turn to destination link
				planStepFactory.addTurn(net.getLinks().get(
						toAct.getLinkId()), (int) time);

				// ////////////////////////////
				if (write) {
					System.out.print("\t" + time);
				}
				// ////////////////////////////////

				time += ttime.getLinkTravelTime(net.getLinks().get(
						toAct.getLinkId()), time);
				if (time > maxTime) {
					planStepFactory.addExit(maxTime);
					return;
				}
				// regular exit at end of leg
				planStepFactory.addExit((int) time);
			}

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
		// /////////////////////////////////
		if (write) {
			System.out.println();
		}
		// ////////////////////////////////////////
	}

	public final Plan<Link> getPlanSteps() {
		final Plan<Link> steps = planStepFactory.getResult();
		planStepFactory.reset();

		// for (int i = 0; i < steps.size(); i++) {
		// if (steps.getStep(i).getLink().getId().equals(new IdImpl("5"))) {
		// System.out.println("BSETEST\t" + planStepsToString(steps));
		// }
		// }
		// /////////////////////
		if (write) {
			System.out.print("BSETEST PlanToPlanStep\ttime for cadyts");
			for (int i = 0; i < steps.size(); i++) {
				System.out.print("\t" + steps.getStep(i).getEntryTime_s());
			}
			System.out.println();
		}
		// ///////////////////
		return steps;
	}

	private static String planStepsToString(cadyts.demand.Plan<Link> planSteps) {
		StringBuffer stringBuffer = new StringBuffer();
		int size = planSteps.size();
		for (int i = 0; i < size; i++) {
			stringBuffer.append("\t" + planStepToString(planSteps.getStep(i)));
		}
		return stringBuffer.toString();
	}

	private static String planStepToString(PlanStep<Link> planStep) {
		return "[\t" + planStep.getEntryTime_s() + ",\t"
				+ planStep.getLink().getId() + "]";

	}
}
