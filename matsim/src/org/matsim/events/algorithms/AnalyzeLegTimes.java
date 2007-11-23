/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeLegTimes.java
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

package org.matsim.events.algorithms;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

/**
 * @author marcel
 *
 * Counts the number of vehicles departed, arrived or got stuck in a time bin
 * based on events. If plans are given, the numbers are differentiated for plans
 * containing work or edu, and other plans.
 *
 * @deprecated user org.matsim.demandmodeling.analysis.LegHistogram instead
 */
@Deprecated
public class AnalyzeLegTimes implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, EventHandlerAgentStuckI {

	private final static String WORK1 = "work1";
	private final static String WORK2 = "work2";
	private final static String WORK3 = "work3";
	private final static String EDU = "edu";
	private final static String UNI = "uni";

	private final int binSize;
	private final int maxTime;
	private int[][] countsDep;
	private int[][] countsArr;
	private int[][] countsStuck;
	private Plans plans = null;

	public AnalyzeLegTimes(final int binSize, final Plans plans) {
		super();
		this.binSize = binSize;
		this.maxTime = ((30*3600 / binSize)) * binSize - 1;
		int maxIndex = getBinIndex(this.maxTime);
		if (plans == null) {
			this.countsDep = new int[1][maxIndex + 2]; // index 0 exists also --> +1; and +1 for all times out of our range, thus + 2 in total
			this.countsArr = new int[1][maxIndex + 2];
			this.countsStuck = new int[1][maxIndex + 2];
		} else {
			this.countsDep = new int[3][maxIndex + 2]; // index 0 exists also --> +1; and +1 for all times out of our range, thus + 2 in total
			this.countsArr = new int[3][maxIndex + 2]; // primary index: 0 = home-{work,edu}-*, 1 = home-{non-work, non-edu}-*, 2 = undefinied
			this.countsStuck = new int[3][maxIndex + 2];
		}
		this.plans = plans;
		reset(0);
	}

	public AnalyzeLegTimes(final int binSize) {
		this(binSize, null);
	}

	public void handleEvent(final EventAgentDeparture event) {
		this.countsDep[getCountsIndex(event)][getBinIndex(event.time)]++;
	}

	public void handleEvent(final EventAgentArrival event) {
		this.countsArr[getCountsIndex(event)][getBinIndex(event.time)]++;
	}

	public void handleEvent(final EventAgentStuck event) {
		this.countsStuck[getCountsIndex(event)][getBinIndex(event.time)]++;
	}

	private int getBinIndex(final double time) {
		if (time <= this.maxTime) {
			return (int)(time / this.binSize);
		}
		return (this.maxTime + 1) / this.binSize;
	}

	private int getCountsIndex(final BasicEvent event) {
		if (this.plans == null) return 0;

		Person person = this.plans.getPerson(event.agentId);
		if (person != null) {
			Plan plan = person.getSelectedPlan();
			if (plan.getActsLegs().size() >= 3) {
				String acttype = ((Act)plan.getActsLegs().get(2)).getType().intern();
				if (WORK1 == acttype || WORK2 == acttype || WORK3 == acttype || UNI == acttype || EDU == acttype)
					return 0;
				return 1;
			}
			return this.countsDep.length - 1;
		}
		return this.countsDep.length - 1;
	}

	public int[][] getLegDepCounts() {
		return this.countsDep;
	}
	public int[][] getLegArrCounts() {
		return this.countsArr;
	}
	public int[][] getStuckCounts() {
		return this.countsStuck;
	}

	public void reset(final int iteration) {
		for (int j = 0; j < this.countsDep.length; j++) {
			for (int i = 0; i < this.countsDep[j].length; i++ ) {
				this.countsDep[j][i] = 0;
				this.countsArr[j][i] = 0;
				this.countsStuck[j][i] = 0;
			}
		}
	}

}
