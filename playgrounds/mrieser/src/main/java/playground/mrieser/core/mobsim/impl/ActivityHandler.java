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

package playground.mrieser.core.mobsim.impl;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.api.MobsimKeepAlive;
import playground.mrieser.core.mobsim.features.MobsimFeature;

/**
 * @author mrieser
 */
public class ActivityHandler implements PlanElementHandler, MobsimFeature, MobsimKeepAlive {

	private final static Logger log = Logger.getLogger(ActivityHandler.class);

	private final NewMobsimEngine simEngine;
	private final PriorityBlockingQueue<ActivityData> activityEndsList = new PriorityBlockingQueue<ActivityData>(500);
	private boolean useActivityDurations = true;

	public ActivityHandler(final NewMobsimEngine simEngine) {
		this.simEngine = simEngine;
		this.simEngine.addKeepAlive(this);
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}

	@Override
	public void handleStart(final PlanAgent agent) {
		Activity act = (Activity) agent.getCurrentPlanElement();
		double endTime = calculateActivityEndTime(act);
		if (endTime != Time.UNDEFINED_TIME) {
			this.activityEndsList.put(new ActivityData(endTime, agent));
		} else {
			// make some checks
			PlanElement last = agent.getPlan().getPlanElements().get(agent.getPlan().getPlanElements().size() - 1);
			if (last != act) {
				log.error("Activity has no end-time specified, but is not the last element in the plan. " +
						"personId = " + agent.getPlan().getPerson().getId() + ". Activity = " + act);
			}
		}
		if (act != agent.getPlan().getPlanElements().get(0)) {
			// do not generate event for first activity
			EventsManager em = this.simEngine.getEventsManager();
			em.processEvent(em.getFactory().createActivityStartEvent(this.simEngine.getCurrentTime(),
					agent.getPlan().getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		}
	}

	@Override
	public void handleEnd(final PlanAgent agent) {
		Activity act = (Activity) agent.getCurrentPlanElement();
		if (act != agent.getPlan().getPlanElements().get(agent.getPlan().getPlanElements().size() - 1)) {
			// do not generate event for last plan element
			EventsManager em = this.simEngine.getEventsManager();
			em.processEvent(em.getFactory().createActivityEndEvent(this.simEngine.getCurrentTime(),
					agent.getPlan().getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		}
	}

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void doSimStep(final double time) {
		while ((!this.activityEndsList.isEmpty()) && (this.activityEndsList.peek().endTime <= time)) {
			ActivityData data = this.activityEndsList.poll();
			this.simEngine.handleAgent(data.agent);
		}
	}

	@Override
	public void afterMobSim() {
	}

	@Override
	public boolean keepAlive() {
		return !this.activityEndsList.isEmpty();
	}

	private double calculateActivityEndTime(final Activity act) {
		double now = this.simEngine.getCurrentTime();
		double departure = 0;

		if (this.useActivityDurations) {
			ActivityImpl a = (ActivityImpl) act;
			if ((a.getMaximumDuration() == Time.UNDEFINED_TIME) && (a.getEndTime() == Time.UNDEFINED_TIME)) {
				return Time.UNDEFINED_TIME;
			}
			/* The person leaves the activity either 'actDur' later or
			 * when the end is defined of the activity, whatever comes first. */
			if (a.getMaximumDuration() == Time.UNDEFINED_TIME) {
				departure = act.getEndTime();
			} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
				departure = now + a.getMaximumDuration();
			} else {
				departure = Math.min(act.getEndTime(), now + a.getMaximumDuration());
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
		public final PlanAgent agent;

		public ActivityData(final double endTime, final PlanAgent agent) {
			this.endTime = endTime;
			this.agent = agent;
		}

		@Override
		public int compareTo(ActivityData o) {
			if (this.endTime < o.endTime) {
				return -1;
			}
			if (this.endTime > o.endTime) {
				return +1;
			}
			return -this.agent.getPlan().getPerson().getId().compareTo(o.agent.getPlan().getPerson().getId()); // the '-' is for backwards compatibility
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ActivityData)) {
				return false;
			}
			ActivityData data = (ActivityData) obj;
			return (this.agent == data.agent) && (this.endTime == data.endTime);
		}

		@Override
		public int hashCode() {
			return this.agent.hashCode();
		}
	}

}
