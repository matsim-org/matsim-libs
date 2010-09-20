/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalConfigGroup.java
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

package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;

public class MultiModalConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "multimodal";
	
	private static final String MULTI_MODAL_SIMULATION_ENABLED = "multiModalSimulationEnabled";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String SIMULATED_MODES = "simulatedModes";
	private static final String CREATE_MULTI_MODAL_NETWORK = "createMultiModalNetwork";
	private static final String CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES  = "cuttoffValueForNonCarModes";
	private static final String DROP_NON_CAR_ROUTES = "dropNonCarRoutes";
	private static final String ENSURE_ACTIVITY_REACHABILITY = "ensureActivityReachability";
	private static final String PT_SCALE_FACTOR = "ptScaleFactor";

	private boolean multiModalSimulationEnabled = false;
	private int numberOfThreads = 1;
	private String simulatedModes = "pt,walk,ride,bike"; 
	private boolean createMultiModalNetwork = false;
	private double cuttoffValueForNonMotorizedModes = 80/3.6;	// 80km/h -> m/s
	private boolean dropNonCarRoutes = false;
	private boolean ensureActivityReachability = false;
	private double ptScaleFactor = 1.25;

	public MultiModalConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		if (MULTI_MODAL_SIMULATION_ENABLED.equals(key)) {
			setMultiModalSimulationEnabled(Boolean.parseBoolean(value.trim()));
		} else if (NUMBER_OF_THREADS.equals(key)) {
			setNumberOfThreads(Integer.parseInt(value));
		} else if (SIMULATED_MODES.equals(key)) {
			setSimulatedModes(value);
		} else if (CREATE_MULTI_MODAL_NETWORK.equals(key)) {
			setCreateMultiModalNetwork(Boolean.parseBoolean(value.trim()));
		} else if (CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES.equals(key)) {
			setCutoffValueForNonCarModes(Double.parseDouble(value));
		} else if (DROP_NON_CAR_ROUTES.equals(key)) {
			setDropNonCarRoutes(Boolean.parseBoolean(value.trim()));
		} else if (ENSURE_ACTIVITY_REACHABILITY.equals(key)) {
			setEnsureActivityReachability(Boolean.parseBoolean(value.trim()));
		} else if (PT_SCALE_FACTOR.equals(key)) {
			setPtScaleFactor(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}
	
	@Override
	public final String getValue(final String key) {
		if (MULTI_MODAL_SIMULATION_ENABLED.equals(key)) {
			return Boolean.toString(isMultiModalSimulationEnabled());
		} else if (NUMBER_OF_THREADS.equals(key)) {
			return Integer.toString(getNumberOfThreads());
		} else if (SIMULATED_MODES.equals(key)) {
			return getSimulatedModes();
		} else if (CREATE_MULTI_MODAL_NETWORK.equals(key)) {
			return Boolean.toString(isCreateMultiModalNetwork());
		} else if (CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES.equals(key)) {
			return Double.toString(getCutoffValueForNonCarModes());
		} else if (DROP_NON_CAR_ROUTES.equals(key)) {
			return Boolean.toString(isDropNonCarRoutes());
		} else if (ENSURE_ACTIVITY_REACHABILITY.equals(key)) {
			return Boolean.toString(isEnsureActivityReachability());
		} else if (PT_SCALE_FACTOR.equals(key)) {
			return Double.toString(getPtScaleFactor());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(MULTI_MODAL_SIMULATION_ENABLED, getValue(MULTI_MODAL_SIMULATION_ENABLED));
		map.put(NUMBER_OF_THREADS, getValue(NUMBER_OF_THREADS));
		map.put(SIMULATED_MODES, getValue(SIMULATED_MODES));
		map.put(CREATE_MULTI_MODAL_NETWORK, getValue(CREATE_MULTI_MODAL_NETWORK));
		map.put(CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES, getValue(CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES));
		map.put(DROP_NON_CAR_ROUTES, getValue(DROP_NON_CAR_ROUTES));
		map.put(PT_SCALE_FACTOR, getValue(PT_SCALE_FACTOR));
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(NUMBER_OF_THREADS, "Use number of threads > 1 for parallel version using the specified number of threads.");
		map.put(CREATE_MULTI_MODAL_NETWORK, "Use this, if your network is not multi modal. Links with free speeds that are lower than the specified cutoff value will be usable for walk and bike trips.");
		map.put(CUTOFF_VALUE_FOR_NON_MOTORIZED_MODES, "Only used, if createMultiModalNetwork is enabled (set value in m/s).");
		map.put(PT_SCALE_FACTOR, "Scale factor that is used to calculate PT travel times (PT travel time = car travel time * scale factor).");
		return map;
	}
	
	/* direct access */
	public void setMultiModalSimulationEnabled(final boolean enabled) {
		this.multiModalSimulationEnabled = enabled;
	}

	public boolean isMultiModalSimulationEnabled() {
		return this.multiModalSimulationEnabled;
	}
	
	public void setNumberOfThreads(final int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}

	public void setSimulatedModes(final String simulatedModes) {
		this.simulatedModes = simulatedModes;
	}

	public String getSimulatedModes() {
		return this.simulatedModes;
	}

	public void setCreateMultiModalNetwork(final boolean enabled) {
		this.createMultiModalNetwork = enabled;
	}

	public boolean isCreateMultiModalNetwork() {
		return this.createMultiModalNetwork;
	}
	
	public void setCutoffValueForNonCarModes(final double cutoffValue) {
		this.cuttoffValueForNonMotorizedModes = cutoffValue;
	}

	public double getCutoffValueForNonCarModes() {
		return this.cuttoffValueForNonMotorizedModes;
	}
	
	public void setDropNonCarRoutes(final boolean enabled) {
		this.dropNonCarRoutes = enabled;
	}

	public boolean isDropNonCarRoutes() {
		return this.dropNonCarRoutes;
	}
	
	public void setEnsureActivityReachability(final boolean enabled) {
		this.ensureActivityReachability = enabled;
	}

	public boolean isEnsureActivityReachability() {
		return this.ensureActivityReachability;
	}
	
	public void setPtScaleFactor(final double scaleFactor) {
		this.ptScaleFactor = scaleFactor;
	}

	public double getPtScaleFactor() {
		return this.ptScaleFactor;
	}
}