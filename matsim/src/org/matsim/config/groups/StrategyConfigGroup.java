/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyConfigGroup.java
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

package org.matsim.config.groups;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.config.Module;

/**
 * Configuration group for specifying the plans-replanning to be used.
 *
 * @author mrieser
 */
public class StrategyConfigGroup extends Module {

	public static final String GROUP_NAME = "strategy";
	private static final String MAX_AGENT_PLAN_MEMORY_SIZE = "maxAgentPlanMemorySize";
	private static final String MODULE = "Module_";
	private static final String MODULE_PROBABILITY = "ModuleProbability_";
	private static final String MODULE_DISABALE_AFTER_ITERATION = "ModuleDisableAfterIteration_";

	private int maxAgentPlanMemorySize = 0;

	private final Map<String, StrategySettings> settings = new HashMap<String, StrategySettings>();

	public StrategyConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			return Integer.toString(getMaxAgentPlanMemorySize());
		}
		if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE.length()), false);
			if (settings == null) {
				return null;
			}
			return settings.getModuleName();
		}
		if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE_PROBABILITY.length()), false);
			if (settings == null) {
				return null;
			}
			return Double.toString(settings.getProbability());
		}
		if (key != null && key.startsWith(MODULE_DISABALE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE_PROBABILITY.length()), false);
			if (settings == null || settings.getDisableAfter() == -1) {
				return null;
			}
			return Integer.toString(settings.getDisableAfter());
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			setMaxAgentPlanMemorySize(Integer.parseInt(value));
		} else if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE.length()), true);
			settings.setModuleName(value);
		} else if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE_PROBABILITY.length()), true);
			settings.setProbability(Double.parseDouble(value));
		} else if (key != null && key.startsWith(MODULE_DISABALE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(key.substring(MODULE_DISABALE_AFTER_ITERATION.length()), true);
			settings.setDisableAfter(Integer.parseInt(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(MAX_AGENT_PLAN_MEMORY_SIZE, getValue(MAX_AGENT_PLAN_MEMORY_SIZE));
		for (Map.Entry<String, StrategySettings>  entry : this.settings.entrySet()) {
			map.put(MODULE + entry.getKey(), entry.getValue().getModuleName());
			map.put(MODULE_PROBABILITY + entry.getKey(), Double.toString(entry.getValue().getProbability()));
			if (entry.getValue().getDisableAfter() == -1) {
				map.put(MODULE_DISABALE_AFTER_ITERATION + entry.getKey(), "null");
			} else {
				map.put(MODULE_DISABALE_AFTER_ITERATION + entry.getKey(), Integer.toString(entry.getValue().getDisableAfter()));
			}
		}

		return map;
	}

	@Override
	protected void checkConsistency() {
		super.checkConsistency();

		// check that the strategies are numbered from 1 to n
		int nofStrategies = this.settings.size();
		for (int i = 1; i <= nofStrategies; i++) {
			StrategySettings settings = getStrategySettings(Integer.toString(i), false);
			if (settings == null) {
				throw new RuntimeException("Loaded " + nofStrategies + " strategies which should be numbered from 1 to n, but could not find strategy " + i);
			}
		}

		// check that each strategy has moduleName set and probability >= 0.0
		for (Map.Entry<String, StrategySettings> settings : this.settings.entrySet()) {
			if (settings.getValue().getModuleName() == null) {
				throw new RuntimeException("Strategy " + settings.getKey() + " has no module set (Missing 'Module_" + settings.getKey() + "').");
			}
			if (settings.getValue().getModuleName().length() == 0) {
				throw new RuntimeException("Strategy " + settings.getKey() + " has no module set ('Module_" + settings.getKey() + "' is empty).");
			}
			if (settings.getValue().getProbability() < 0.0) {
				throw new RuntimeException("Probability for strategy " + settings.getKey() + " must be >= 0.0 (Maybe 'ModuleProbability_" + settings.getKey() + "') is missing?");
			}
		}
	}

	private StrategySettings getStrategySettings(final String index, final boolean createIfMissing) {
		StrategySettings settings = this.settings.get(index);
		if (settings == null && createIfMissing) {
			settings = new StrategySettings();
			this.settings.put(index, settings);
		}
		return settings;
	}

	public void setMaxAgentPlanMemorySize(final int maxAgentPlanMemorySize) {
		this.maxAgentPlanMemorySize = maxAgentPlanMemorySize;
	}

	public int getMaxAgentPlanMemorySize() {
		return this.maxAgentPlanMemorySize;
	}

	public static class StrategySettings {
		private double probability = -1.0;
		private String moduleName = null;
		private int disableAfter = -1;

		public void setProbability(final double probability) {
			this.probability = probability;
		}

		public double getProbability() {
			return this.probability;
		}

		public void setModuleName(final String moduleName) {
			this.moduleName = moduleName;
		}

		public String getModuleName() {
			return this.moduleName;
		}

		public void setDisableAfter(final int disableAfter) {
			this.disableAfter = disableAfter;
		}

		public int getDisableAfter() {
			return this.disableAfter;
		}
	}
}
