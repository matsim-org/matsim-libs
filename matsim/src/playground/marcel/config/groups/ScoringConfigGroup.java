/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringConfigGroup.java
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

package playground.marcel.config.groups;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.gbl.Gbl;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

public class ScoringConfigGroup implements ConfigGroupI {

	public static final String GROUP_NAME = "scoring";

	private static final String LEARNING_RATE = "learningRate";
	private static final String BRAIN_EXP_BETA = "brainExpBeta";
	private static final String LATE_ARRIVAL = "lateArrival";
	private static final String EARLY_DEPARTURE = "earlyDeparture";
	private static final String PERFORMING = "performing";
	private static final String TRAVELING  = "traveling";
	private static final String WAITING  = "waiting";

	private static final String ACTIVITIES = "activities";

	private static final String PRIORITY = "priority";
	private static final String TYPICAL_DURATION = "typicalDuration";
	private static final String MINIMUM_DURATION = "minimumDuration";
	private static final String OPENING_TIME = "openingTime";
	private static final String CLOSING_TIME = "closingTime";
	private static final String LATEST_START_TIME = "latestStartTime";
	private static final String EARLIEST_END_TIME = "earliestEndTime";

	private double learningRate = 1.0;
	private double brainExpBeta = 2.0;
	private double lateArrival = -18.0;
	private double earlyDeparture = -0.0;
	private double performing = +6.0;
	private double traveling = -6.0;
	private double waiting = -0.0;

	private final ActivitiesList activities = new ActivitiesList();

	private final Set<String> paramKeySet = new LinkedHashSet<String>();
	private final Set<String> listKeySet = new LinkedHashSet<String>();

	public ScoringConfigGroup() {
		this.paramKeySet.add(LEARNING_RATE);
		this.paramKeySet.add(BRAIN_EXP_BETA);
		this.paramKeySet.add(LATE_ARRIVAL);
		this.paramKeySet.add(EARLY_DEPARTURE);
		this.paramKeySet.add(PERFORMING);
		this.paramKeySet.add(TRAVELING);
		this.paramKeySet.add(WAITING);

		this.listKeySet.add(ACTIVITIES);
	}

	public String getName() {
		return GROUP_NAME;
	}

	public String getValue(final String key) {
		if (LEARNING_RATE.equals(key)) {
			return Double.toString(getLearningRate());
		} else if (BRAIN_EXP_BETA.equals(key)) {
			return Double.toString(getBrainExpBeta());
		} else if (LATE_ARRIVAL.equals(key)) {
			return Double.toString(getLateArrival());
		} else if (EARLY_DEPARTURE.equals(key)) {
			return Double.toString(getEarlyDeparture());
		} else if (PERFORMING.equals(key)) {
			return Double.toString(getPerforming());
		} else if (TRAVELING.equals(key)) {
			return Double.toString(getTraveling());
		} else if (WAITING.equals(key)) {
			return Double.toString(getWaiting());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public void setValue(final String key, final String value) {
		if (LEARNING_RATE.equals(key)) {
			setLearningRate(Double.parseDouble(value));
		} else if (BRAIN_EXP_BETA.equals(key)) {
			setBrainExpBeta(Double.parseDouble(value));
		} else if (LATE_ARRIVAL.equals(key)) {
			setLateArrival(Double.parseDouble(value));
		} else if (EARLY_DEPARTURE.equals(key)) {
			setEarlyDeparture(Double.parseDouble(value));
		} else if (PERFORMING.equals(key)) {
			setPerforming(Double.parseDouble(value));
		} else if (TRAVELING.equals(key)) {
			setTraveling(Double.parseDouble(value));
		} else if (WAITING.equals(key)) {
			setWaiting(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public ConfigListI getList(final String key) {
		if (ACTIVITIES.equals(key)) {
			return this.activities;
		}
		throw new IllegalArgumentException(key);
	}

	public Set<String> listKeySet() {
		return this.listKeySet;
	}

	public Set<String> paramKeySet() {
		return this.paramKeySet;
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

	public double getWaiting() {
		return this.waiting;
	}
	public void setWaiting(final double waiting) {
		this.waiting = waiting;
	}

	public ActivitySettings getActivity(final String activityType) {
		return this.activities.getActivity(activityType);
	}

	/* helper classes */

	public class ActivitySettings implements ConfigGroupI {
		private double priority = 1.0;
		private double typicalDuration = Gbl.UNDEFINED_TIME;
		private double minimumDuration = Gbl.UNDEFINED_TIME;
		private double openingTime = Gbl.UNDEFINED_TIME;
		private double closingTime = Gbl.UNDEFINED_TIME;
		private double latestStartTime = Gbl.UNDEFINED_TIME;
		private double earliestEndTime = Gbl.UNDEFINED_TIME;

		private final String name;
		private final Set<String> keySet = new LinkedHashSet<String>();
		// TODO [MR] keyset could be static, but the initialization is more complicated in the static case...

		private ActivitySettings(final String name) {
			this.name = name;
			this.keySet.add(PRIORITY);
			this.keySet.add(TYPICAL_DURATION);
			this.keySet.add(MINIMUM_DURATION);
			this.keySet.add(OPENING_TIME);
			this.keySet.add(CLOSING_TIME);
			this.keySet.add(LATEST_START_TIME);
			this.keySet.add(EARLIEST_END_TIME);
		}

		public String getName() {
			return this.name;
		}

		public String getValue(final String key) {
			if (PRIORITY.equals(key)) {
				return Double.toString(getPriority());
			} else if (TYPICAL_DURATION.equals(key)) {
				return Gbl.writeTime(getTypicalDuration());
			} else if (MINIMUM_DURATION.equals(key)) {
				return Gbl.writeTime(getMinimumDuration());
			} else if (OPENING_TIME.equals(key)) {
				return Gbl.writeTime(getOpeningTime());
			} else if (CLOSING_TIME.equals(key)) {
				return Gbl.writeTime(getClosingTime());
			} else if (LATEST_START_TIME.equals(key)) {
				return Gbl.writeTime(getLatestStartTime());
			} else if (EARLIEST_END_TIME.equals(key)) {
				return Gbl.writeTime(getEarliestEndTime());
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public void setValue(final String key, final String value) {
			if (PRIORITY.equals(key)) {
				setPriority(Double.parseDouble(value));
			} else if (TYPICAL_DURATION.equals(key)) {
				setTypicalDuration(Gbl.parseTime(value));
			} else if (MINIMUM_DURATION.equals(key)) {
				setMinimumDuration(Gbl.parseTime(value));
			} else if (OPENING_TIME.equals(key)) {
				setOpeningTime(Gbl.parseTime(value));
			} else if (CLOSING_TIME.equals(key)) {
				setClosingTime(Gbl.parseTime(value));
			} else if (LATEST_START_TIME.equals(key)) {
				setLatestStartTime(Gbl.parseTime(value));
			} else if (EARLIEST_END_TIME.equals(key)) {
				setEarliestEndTime(Gbl.parseTime(value));
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public Set<String> paramKeySet() {
			return this.keySet;
		}

		public Set<String> listKeySet() {
			return new HashSet<String>();
		}

		public ConfigListI getList(final String key) {
			throw new UnsupportedOperationException();
		}

		/* direct access */

		public void setPriority(final double priority) {
			this.priority = priority;
		}
		public double getPriority() {
			return this.priority;
		}

		public void setTypicalDuration(final double typicalDuration) {
			this.typicalDuration = typicalDuration;
		}
		public double getTypicalDuration() {
			return this.typicalDuration;
		}

		public void setMinimumDuration(final double minimumDuration) {
			this.minimumDuration = minimumDuration;
		}
		public double getMinimumDuration() {
			return this.minimumDuration;
		}

		public void setOpeningTime(final double openingTime) {
			this.openingTime = openingTime;
		}
		public double getOpeningTime() {
			return this.openingTime;
		}

		public void setClosingTime(final double closingTime) {
			this.closingTime = closingTime;
		}
		public double getClosingTime() {
			return this.closingTime;
		}

		public void setLatestStartTime(final double latestStartTime) {
			this.latestStartTime = latestStartTime;
		}
		public double getLatestStartTime() {
			return this.latestStartTime;
		}

		public void setEarliestEndTime(final double earliestEndTime) {
			this.earliestEndTime = earliestEndTime;
		}
		public double getEarliestEndTime() {
			return this.earliestEndTime;
		}

	}

	private class ActivitiesList implements ConfigListI {

		private final Map<String, ActivitySettings> entries = new LinkedHashMap<String, ActivitySettings>();

		public ConfigGroupI addGroup(final String key) {
			ActivitySettings entry = new ActivitySettings(key);
			this.entries.put(key, entry);
			return entry;
		}

		public ConfigGroupI getGroup(final String key) {
			return this.entries.get(key);
		}

		public Set<String> keySet() {
			return this.entries.keySet();
		}

		public ActivitySettings getActivity(final String type) {
			return this.entries.get(type);
		}

	}

}
