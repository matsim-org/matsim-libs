/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterConfigGroup.java
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
package playground.gregor.sims.config;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.core.config.Module;

/**
 * @author laemmel
 * 
 */
public class ShelterConfigGroup extends Module {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum InitialAssignment {
		greedy, random
	}

	public static final String GROUP_NAME = "shelters";

	private static final String PROB_SHIFT = "shiftProbability";

	private static final String PROB_SWITCH = "switchProbability";

	private static final String INITAL_ASSIGNMENT = "initialAssignment";

	private double switchProbability = 0.;

	private double shiftProbability = 0.;

	private InitialAssignment assignment = InitialAssignment.random;

	/**
	 * @param name
	 */
	public ShelterConfigGroup(Module shelters) {
		super(GROUP_NAME);
		for (Entry<String, String> e : shelters.getParams().entrySet()) {
			addParam(e.getKey(), e.getValue());
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (PROB_SHIFT.equals(key)) {
			setShiftProbability(value);
		} else if (PROB_SWITCH.equals(key)) {
			setSwitchProbability(value);
		} else if (INITAL_ASSIGNMENT.equals(key)) {
			setInitialAssignment(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	/**
	 * @param value
	 */
	private void setInitialAssignment(String value) {
		if (value.equals("greedy")) {
			this.assignment = InitialAssignment.greedy;
		} else if (value.equals("random")) {
			this.assignment = InitialAssignment.random;
		} else {
			throw new RuntimeException("unknown assignment type:" + value);
		}

	}

	/**
	 * 
	 * @return
	 */
	public InitialAssignment getInitialAssignment() {
		return this.assignment;
	}

	@Override
	public String getValue(final String key) {
		if (PROB_SHIFT.equals(key)) {
			return Double.toString(getShiftProbability());
		} else if (PROB_SWITCH.equals(key)) {
			return Double.toString(getSwitchProbability());
		} else if (INITAL_ASSIGNMENT.equals(key)) {
			return getInitialAssignment().toString();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(PROB_SHIFT, getValue(PROB_SHIFT));
		map.put(PROB_SWITCH, getValue(PROB_SWITCH));
		map.put(INITAL_ASSIGNMENT, getValue(INITAL_ASSIGNMENT));
		return map;
	}

	/**
	 * @param value
	 */
	private void setSwitchProbability(String value) {
		this.switchProbability = Double.parseDouble(value);

	}

	/**
	 * 
	 * @return
	 */
	public double getSwitchProbability() {
		return this.switchProbability;
	}

	/**
	 * @param value
	 */
	private void setShiftProbability(String value) {
		this.shiftProbability = Double.parseDouble(value);

	}

	/**
	 * 
	 * @return
	 */
	public double getShiftProbability() {
		return this.shiftProbability;
	}

}
