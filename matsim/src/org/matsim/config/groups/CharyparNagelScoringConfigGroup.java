/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringConfigGroup.java
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

package org.matsim.config.groups;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.config.Module;
import org.matsim.gbl.Gbl;

public class CharyparNagelScoringConfigGroup extends Module {

	public static final String GROUP_NAME = "planCalcScore"; // TODO [MR] switch to better name

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "BrainExpBeta";
	private static final String PATH_SIZE_LOGIT_BETA = "PathSizeLogitBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";
	private static final String TRAVELING = "traveling";
	private static final String TRAVELING_PT = "travelingPt";
	private static final String WAITING  = "waiting";
	private static final String DISTANCE_COST = "distanceCost";

	@Deprecated
	private static final String NUM_ACTIVITIES = "numActivities";

	private static final String ACTIVITY_TYPE = "activityType_";
	private static final String ACTIVITY_PRIORITY = "activityPriority_";
	private static final String ACTIVITY_TYPICAL_DURATION = "activityTypicalDuration_";
	private static final String ACTIVITY_MINIMAL_DURATION = "activityMinimalDuration_";
	private static final String ACTIVITY_OPENING_TIME = "activityOpeningTime_";
	private static final String ACTIVITY_LATEST_START_TIME = "activityLatestStartTime_";
	private static final String ACTIVITY_EARLIEST_END_TIME = "activityEarliestEndTime_";
	private static final String ACTIVITY_CLOSING_TIME = "activityClosingTime_";

	public CharyparNagelScoringConfigGroup() {
		super(GROUP_NAME);
	}

	private double learningRate = 1.0;
	private double brainExpBeta = 2.0;
	private double pathSizeLogitBeta = 1.0;
	private double lateArrival = -18.0;
	private double earlyDeparture = -0.0;
	private double performing = +6.0;
	private double traveling = -6.0;
	private double travelingPt = -6.0;
	private double distanceCost = 0.0;
	private double waiting = -0.0;

	private final Map<String, ActivityParams> activityTypes = new HashMap<String, ActivityParams>();
	private final Map<String, ActivityParams> activityTypesByNumber = new HashMap<String, ActivityParams>();

	private static final Logger log = Logger.getLogger(CharyparNagelScoringConfigGroup.class);
	
	@Override
	public String getValue(final String key) {
		if (LEARNING_RATE.equals(key)) {
			return Double.toString(getLearningRate());
		} else if (BRAIN_EXP_BETA.equals(key)) {
			return Double.toString(getBrainExpBeta());
		} else if (PATH_SIZE_LOGIT_BETA.equals(key)) {
			return Double.toString(getPathSizeLogitBeta());
		} else if (LATE_ARRIVAL.equals(key)) {
			return Double.toString(getLateArrival());
		} else if (EARLY_DEPARTURE.equals(key)) {
			return Double.toString(getEarlyDeparture());
		} else if (PERFORMING.equals(key)) {
			return Double.toString(getPerforming());
		} else if (TRAVELING.equals(key)) {
			return Double.toString(getTraveling());
		} else if (TRAVELING_PT.equals(key)) {
			return Double.toString(getTravelingPt());
		} else if (DISTANCE_COST.equals(key)) {
			return Double.toString(getDistanceCost());
		} else if (WAITING.equals(key)) {
			return Double.toString(getWaiting());
		} else if (key != null && key.startsWith(ACTIVITY_TYPE)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPE.length()), false);
			return actParams == null ? null : actParams.getType();
		} else if (key != null && key.startsWith(ACTIVITY_PRIORITY)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_PRIORITY.length()), false);
			return Double.toString(actParams.getPriority());
		} else if (key != null && key.startsWith(ACTIVITY_TYPICAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPICAL_DURATION.length()), false);
			return Gbl.writeTime(actParams.getTypicalDuration());
		} else if (key != null && key.startsWith(ACTIVITY_MINIMAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_MINIMAL_DURATION.length()), false);
			return Gbl.writeTime(actParams.getMinimalDuration());
		} else if (key != null && key.startsWith(ACTIVITY_OPENING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_OPENING_TIME.length()), false);
			return Gbl.writeTime(actParams.getOpeningTime());
		} else if (key != null && key.startsWith(ACTIVITY_LATEST_START_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_LATEST_START_TIME.length()), false);
			return Gbl.writeTime(actParams.getLatestStartTime());
		} else if (key != null && key.startsWith(ACTIVITY_EARLIEST_END_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_EARLIEST_END_TIME.length()), false);
			return Gbl.writeTime(actParams.getEarliestEndTime());
		} else if (key != null && key.startsWith(ACTIVITY_CLOSING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_CLOSING_TIME.length()), false);
			return Gbl.writeTime(actParams.getClosingTime());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (LEARNING_RATE.equals(key)) {
			setLearningRate(Double.parseDouble(value));
		} else if (BRAIN_EXP_BETA.equals(key)) {
			setBrainExpBeta(Double.parseDouble(value));
		} else if (PATH_SIZE_LOGIT_BETA.equals(key)) {
			setPathSizeLogitBeta(Double.parseDouble(value));
		} else if (LATE_ARRIVAL.equals(key)) {
			setLateArrival(Double.parseDouble(value));
		} else if (EARLY_DEPARTURE.equals(key)) {
			setEarlyDeparture(Double.parseDouble(value));
		} else if (PERFORMING.equals(key)) {
			setPerforming(Double.parseDouble(value));
		} else if (TRAVELING.equals(key)) {
			setTraveling(Double.parseDouble(value));
		} else if (TRAVELING_PT.equals(key)) {
			setTravelingPt(Double.parseDouble(value));
		} else if (DISTANCE_COST.equals(key)) {
			setDistanceCost(Double.parseDouble(value));
		} else if (WAITING.equals(key)) {
			setWaiting(Double.parseDouble(value));
		} else if (NUM_ACTIVITIES.equals(key)) {
			log.info("The parameter " + NUM_ACTIVITIES + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else if (key != null && key.startsWith(ACTIVITY_TYPE)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPE.length()), true);
			this.activityTypes.remove(actParams.getType());
			actParams.setType(value);
			this.activityTypes.put(value, actParams);
		} else if (key != null && key.startsWith(ACTIVITY_PRIORITY)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_PRIORITY.length()), true);
			actParams.setPriority(Double.parseDouble(value));
		} else if (key != null && key.startsWith(ACTIVITY_TYPICAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_TYPICAL_DURATION.length()), true);
			actParams.setTypicalDuration(Gbl.parseTime(value));
		} else if (key != null && key.startsWith(ACTIVITY_MINIMAL_DURATION)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_MINIMAL_DURATION.length()), true);
			actParams.setMinimalDuration(Gbl.parseTime(value));
		} else if (key != null && key.startsWith(ACTIVITY_OPENING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_OPENING_TIME.length()), true);
			actParams.setOpeningTime(Gbl.parseTime(value));
		} else if (key != null && key.startsWith(ACTIVITY_LATEST_START_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_LATEST_START_TIME.length()), true);
			actParams.setLatestStartTime(Gbl.parseTime(value));
		} else if (key != null && key.startsWith(ACTIVITY_EARLIEST_END_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_EARLIEST_END_TIME.length()), true);
			actParams.setEarliestEndTime(Gbl.parseTime(value));
		} else if (key != null && key.startsWith(ACTIVITY_CLOSING_TIME)) {
			ActivityParams actParams = getActivityTypeByNumber(key.substring(ACTIVITY_CLOSING_TIME.length()), true);
			actParams.setClosingTime(Gbl.parseTime(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}
	
	protected TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(LEARNING_RATE, getValue(LEARNING_RATE));
		map.put(BRAIN_EXP_BETA, getValue(BRAIN_EXP_BETA));
		map.put(PATH_SIZE_LOGIT_BETA, getValue(PATH_SIZE_LOGIT_BETA));
		map.put(LATE_ARRIVAL, getValue(LATE_ARRIVAL));
		map.put(EARLY_DEPARTURE, getValue(EARLY_DEPARTURE));
		map.put(PERFORMING, getValue(PERFORMING));
		map.put(TRAVELING, getValue(TRAVELING));
		map.put(TRAVELING_PT, getValue(TRAVELING_PT));
		map.put(WAITING, getValue(WAITING));
		map.put(DISTANCE_COST, getValue(DISTANCE_COST));

		for(Entry<String, ActivityParams> entry : this.activityTypesByNumber.entrySet()) {
			String key = entry.getKey();
			map.put(ACTIVITY_TYPE + key, getValue(ACTIVITY_TYPE + key));
			map.put(ACTIVITY_PRIORITY + key, getValue(ACTIVITY_PRIORITY + key));
			map.put(ACTIVITY_TYPICAL_DURATION + key, getValue(ACTIVITY_TYPICAL_DURATION + key));
			map.put(ACTIVITY_MINIMAL_DURATION + key, getValue(ACTIVITY_MINIMAL_DURATION + key));
			map.put(ACTIVITY_OPENING_TIME + key, getValue(ACTIVITY_OPENING_TIME + key));
			map.put(ACTIVITY_LATEST_START_TIME + key, getValue(ACTIVITY_LATEST_START_TIME + key));
			map.put(ACTIVITY_EARLIEST_END_TIME + key, getValue(ACTIVITY_EARLIEST_END_TIME + key));
			map.put(ACTIVITY_CLOSING_TIME + key, getValue(ACTIVITY_CLOSING_TIME + key));
		}
		return map;
	}


	private ActivityParams getActivityTypeByNumber(final String number, final boolean createIfMissing) {
		ActivityParams actType = this.activityTypesByNumber.get(number);
		if (actType == null && createIfMissing) {
			actType = new ActivityParams(number);
			this.activityTypesByNumber.put(number, actType);
			this.activityTypes.put(number, actType);
		}
		return actType;
	}

	public Collection<String> getActivityTypes() {
		return this.activityTypes.keySet();
	}

	public Collection<ActivityParams> getActivityParams() {
		return this.activityTypes.values();
	}

	/* direct access */

	public double getLearningRate() {
		return this.learningRate;
	}
	public void setLearningRate(final double learningRate) {
		this.learningRate = learningRate;
	}

	public double getBrainExpBeta() {
		return this.brainExpBeta;
	}
	public void setBrainExpBeta(final double beta) {
		this.brainExpBeta = beta;
	}
	
	public double getPathSizeLogitBeta() {
		return this.pathSizeLogitBeta;
	}	
	public void setPathSizeLogitBeta(final double beta) {
		this.pathSizeLogitBeta = beta;
	}
	public double getLateArrival() {
		return this.lateArrival;
	}
	public void setLateArrival(final double lateArrival) {
		this.lateArrival = lateArrival;
	}

	public double getEarlyDeparture() {
		return this.earlyDeparture;
	}
	public void setEarlyDeparture(final double earlyDeparture) {
		this.earlyDeparture = earlyDeparture;
	}

	public double getPerforming() {
		return this.performing;
	}
	public void setPerforming(final double performing) {
		this.performing = performing;
	}

	public double getTraveling() {
		return this.traveling;
	}
	public void setTraveling(final double traveling) {
		this.traveling = traveling;
	}

	public double getTravelingPt() {
		return this.travelingPt;
	}
	public void setTravelingPt(final double travelingPt) {
		this.travelingPt = travelingPt;
	}

	public double getDistanceCost() {
		return this.distanceCost;
	}
	public void setDistanceCost(final double distanceCost) {
		this.distanceCost = distanceCost;
	}

	public double getWaiting() {
		return this.waiting;
	}
	public void setWaiting(final double waiting) {
		this.waiting = waiting;
	}

	public ActivityParams getActivityParams(final String actType) {
		return this.activityTypes.get(actType);
	}

	/* complex classes */

	public static class ActivityParams {
		private String type;
		private double priority;
		private double typicalDuration = Gbl.UNDEFINED_TIME;
		private double minimalDuration = Gbl.UNDEFINED_TIME;
		private double openingTime = Gbl.UNDEFINED_TIME;
		private double latestStartTime = Gbl.UNDEFINED_TIME;
		private double earliestEndTime = Gbl.UNDEFINED_TIME;
		private double closingTime = Gbl.UNDEFINED_TIME;

		public ActivityParams(final String type) {
			this.type = type;
		}

		public String getType() {
			return this.type;
		}
		public void setType(final String type) {
			this.type = type;
		}

		public double getPriority() {
			return this.priority;
		}
		public void setPriority(final double priority) {
			this.priority = priority;
		}

		public double getTypicalDuration() {
			return this.typicalDuration;
		}
		public void setTypicalDuration(final double typicalDuration) {
			this.typicalDuration = typicalDuration;
		}

		public double getMinimalDuration() {
			return this.minimalDuration;
		}
		public void setMinimalDuration(final double minimalDuration) {
			this.minimalDuration = minimalDuration;
		}

		public double getOpeningTime() {
			return this.openingTime;
		}
		public void setOpeningTime(final double openingTime) {
			this.openingTime = openingTime;
		}

		public double getLatestStartTime() {
			return this.latestStartTime;
		}
		public void setLatestStartTime(final double latestStartTime) {
			this.latestStartTime = latestStartTime;
		}

		public double getEarliestEndTime() {
			return this.earliestEndTime;
		}
		public void setEarliestEndTime(final double earliestEndTime) {
			this.earliestEndTime = earliestEndTime;
		}

		public double getClosingTime() {
			return this.closingTime;
		}
		public void setClosingTime(final double closingTime) {
			this.closingTime = closingTime;
		}
	}

}
