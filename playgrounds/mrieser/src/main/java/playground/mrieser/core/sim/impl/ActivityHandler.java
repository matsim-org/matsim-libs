/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.impl;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanElementHandler;
import playground.mrieser.core.sim.features.SimFeature;

/**
 * @author mrieser
 */
public class ActivityHandler implements PlanElementHandler, SimFeature {

	private final static Logger log = Logger.getLogger(ActivityHandler.class);

	private final NewSimEngine simEngine;
	private final PriorityBlockingQueue<ActivityData> activityEndsList = new PriorityBlockingQueue<ActivityData>(500);
	private boolean useActivityDurations = true;

	public ActivityHandler(final NewSimEngine simEngine) {
		this.simEngine = simEngine;
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}

	@Override
	public void handleStart(final PlanElement element, final Plan plan) {
		Activity act = (Activity) element;
		EventsManager em = this.simEngine.getEventsManager();
		double endTime = calculateActivityEndTime(act);
		if (endTime != Time.UNDEFINED_TIME) {
			this.activityEndsList.put(new ActivityData(endTime, plan));
		} else {
			// make some checks
			PlanElement last = plan.getPlanElements().get(plan.getPlanElements().size() - 1);
			if (last != element) {
				log.error("Activity has no end-time specified, but is not the last element in the plan. " +
						"personId = " + plan.getPerson().getId() + ". Activity = " + act);
			}
		}
		em.processEvent(em.getFactory().createActivityStartEvent(this.simEngine.getCurrentTime(),
				plan.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
	}

	@Override
	public void handleEnd(final PlanElement element, final Plan plan) {
		Activity act = (Activity) element;
		EventsManager em = this.simEngine.getEventsManager();
		em.processEvent(em.getFactory().createActivityEndEvent(this.simEngine.getCurrentTime(),
				plan.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
	}

	@Override
	public void doSimStep(final double time) {
		while ((!this.activityEndsList.isEmpty()) && (this.activityEndsList.peek().endTime <= time)) {
			ActivityData data = this.activityEndsList.poll();
			this.simEngine.handleNextPlanElement(data.plan);
		}
	}

	@Override
	public boolean isFinished() {
		return this.activityEndsList.isEmpty();
	}

	private double calculateActivityEndTime(final Activity act) {
		double now = this.simEngine.getCurrentTime();
		double departure = 0;

		if (this.useActivityDurations) {
			ActivityImpl a = (ActivityImpl) act;
			if ((a.getDuration() == Time.UNDEFINED_TIME) && (a.getEndTime() == Time.UNDEFINED_TIME)) {
				return Time.UNDEFINED_TIME;
			}
			/* The person leaves the activity either 'actDur' later or
			 * when the end is defined of the activity, whatever comes first. */
			if (a.getDuration() == Time.UNDEFINED_TIME) {
				departure = act.getEndTime();
			} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
				departure = now + a.getDuration();
			} else {
				departure = Math.min(act.getEndTime(), now + a.getDuration());
			}
		} else {
			if (act.getEndTime() == Time.UNDEFINED_TIME) {
				return Time.UNDEFINED_TIME;
			}
			departure = act.getEndTime();
		}

		if (departure < now) {
			// we cannot depart before we arrived, thus change the time so the timestamp in events will be right
			departure = now;
			// actually, we will depart in (now+1) because we already missed the departing in this time step
		}

		return departure;
	}

	private static class ActivityData implements Comparable<ActivityData> {
		public final double endTime;
		public final Plan plan;

		public ActivityData(final double endTime, final Plan plan) {
			this.endTime = endTime;
			this.plan = plan;
		}

		@Override
		public int compareTo(ActivityData o) {
			return Double.compare(this.endTime, o.endTime);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ActivityData)) {
				return false;
			}
			ActivityData data = (ActivityData) obj;
			return (this.plan == data.plan) && (this.endTime == data.endTime);
		}
	}

}
