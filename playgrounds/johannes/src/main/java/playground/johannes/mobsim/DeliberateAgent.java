/* *********************************************************************** *
 * project: org.matsim.*
 * DeliberateAgent.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * A DeliberateAgent is an agent that can modify its plan conforming to a
 * {@link IntradayStrategy}. The DeliberateAgent acts as a kind of controller
 * for re-planning and decides when the {@link IntradayStrategy} is request to
 * re-plan. Currently the DeliberateAgent triggers the re-planning mechanism
 * each time {@link #getNextLink(double)} is called. After a re-plan is trigger
 * a certain cool down time defined by {@link #setReplanCoolDownTime(int)} must
 * pass until a repeated re-plan can be triggered.
 * 
 * @author illenberger
 * 
 */
public class DeliberateAgent extends MobsimAgentDecorator<PlanAgent> {
	
	public static final int DEFAULT_REPLAN_COOL_DOWN_TIME = 1;

	// =======================================================
	// private fields
	// =======================================================

	private static final Logger log = Logger.getLogger(DeliberateAgent.class);

	private IntradayStrategy strategy;

	/*
	 * TODO: Discuss the name of this field!
	 */
	private int replanCoolDownTime = DEFAULT_REPLAN_COOL_DOWN_TIME;

	private double lastReplanTime;

	// =======================================================
	// constructor
	// =======================================================

	/**
	 * Creates a new DeliberateAgent decorating <tt>agent</tt> and with
	 * re-plan capabilities conforming to <tt>strategy</tt>. The default cool
	 * down time for re-planning is set to 1 s.
	 * 
	 * @param agent
	 *            the decorated {@link PlanAgent}.
	 * @param strategy
	 *            the re-planning strategy.
	 */
	public DeliberateAgent(PlanAgent agent, IntradayStrategy strategy) {
		super(agent);
		this.strategy = strategy;
	}
	
	// =======================================================
	// accessor methods
	// =======================================================

	/**
	 * Sets the cool down time for re-planning, i.e., once
	 * {@link #replan(double)} is called, <tt>time</tt> must pass before a
	 * repeated call of {@link #replan(double)} will have any effect.
	 * 
	 * @param time
	 *            the cool down time for re-planning.
	 */
	public void setReplanCoolDownTime(int time) {
		this.replanCoolDownTime = time;
	}

	/**
	 * @return the cool down time for re-planning.
	 */
	public int getReplanCoolDownTime() {
		return replanCoolDownTime;
	}

	// =======================================================
	// re-plan controlling
	// =======================================================

	/**
	 * Calls {@link #replan(double)} followed by
	 * {@link MobsimAgentDecorator#getNextLink(double)}.
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	@Override
	public Link getNextLink(double time) {
		replan(time);
		return super.getNextLink(time);
	}

	/**
	 * Triggers the re-planning mechanism.<br>
	 * First checks if the cool down time since the last re-planning has passed.
	 * If not the methods returns, if yes
	 * {@link IntradayStrategy#replan(double)} is called. If the plan returned
	 * by {@link IntradayStrategy#replan(double)} is not null and valid, it will
	 * replace the current selected plan. A plan is valid if its size is odd and
	 * greater than {@link PlanAgent#getCurrentPlanIndex()}. TODO: Introduce
	 * more stringent plan validation.
	 * 
	 * @param time
	 *            the current simulation time.
	 */
	public void replan(double time) {
		/*
		 * Avoid repeated re-planning in a short time interval.
		 */
		if (time - lastReplanTime > replanCoolDownTime) {
			lastReplanTime = time;

			PlanImpl newPlan = strategy.replan(time);
			if (newPlan != null) {
				/*
				 * Do some simple plan validation. The new plan size must be
				 * greater then the current plan index and odd.
				 */
				int size = newPlan.getPlanElements().size();
				if (size > agent.getCurrentPlanIndex() && size % 2 != 0) {
					/*
					 * Replace the selected plan. In future we may append the
					 * new plan and leave the old plan untouched. Needs further
					 * investigation...
					 */
					((PersonImpl) agent.getPerson()).exchangeSelectedPlan(newPlan, false);
				} else {
					log.warn(String.format(
								"Re-planning failed! The plan size is incorrect (size=%1$s, index=%2$s).",
								size, agent.getCurrentPlanIndex()));
				}
			}
		}
	}
}
