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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
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
	private static final String MODULE_EXE_PATH = "ModuleExePath_";
	private static final String EXTERNAL_EXE_CONFIG_TEMPLATE = "ExternalExeConfigTemplate";
	private static final String EXTERNAL_EXE_TMP_FILE_ROOT_DIR = "ExternalExeTmpFileRootDir";
	private static final String EXTERNAL_EXE_TIME_OUT = "ExternalExeTimeOut";

	private int maxAgentPlanMemorySize = 0;
	private String externalExeConfigTemplate = null;
	private String externalExeTmpFileRootDir = null;
	private long externalExeTimeOut = 3600;

	private final Map<Id, StrategySettings> settings = new LinkedHashMap<Id, StrategySettings>();

	public StrategyConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			return Integer.toString(getMaxAgentPlanMemorySize());
		}
		if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE.length())), false);
			if (settings == null) {
				return null;
			}
			return settings.getModuleName();
		}
		if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_PROBABILITY.length())), false);
			if (settings == null) {
				return null;
			}
			return Double.toString(settings.getProbability());
		}
		if (key != null && key.startsWith(MODULE_DISABALE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_PROBABILITY.length())), false);
			if (settings == null || settings.getDisableAfter() == -1) {
				return null;
			}
			return Integer.toString(settings.getDisableAfter());
		}
		if (key != null && key.startsWith(MODULE_EXE_PATH)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_EXE_PATH.length())), false);
			if (settings == null) {
				return null;
			}
			return settings.getExePath();
		}
		if (EXTERNAL_EXE_CONFIG_TEMPLATE.equals(key)) {
			return getExternalExeConfigTemplate();
		}
		if (EXTERNAL_EXE_TMP_FILE_ROOT_DIR.equals(key)) {
			return getExternalExeTmpFileRootDir();
		}
		if (EXTERNAL_EXE_TIME_OUT.equals(key)) {
			return Long.toString(getExternalExeTimeOut());
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			setMaxAgentPlanMemorySize(Integer.parseInt(value));
		} else if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE.length())), true);
			settings.setModuleName(value);
		} else if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_PROBABILITY.length())), true);
			settings.setProbability(Double.parseDouble(value));
		} else if (key != null && key.startsWith(MODULE_DISABALE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_DISABALE_AFTER_ITERATION.length())), true);
			settings.setDisableAfter(Integer.parseInt(value));
		} else if (key != null && key.startsWith(MODULE_EXE_PATH)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_EXE_PATH.length())), true);
			settings.setExePath(value);
		} else if (EXTERNAL_EXE_CONFIG_TEMPLATE.equals(key)) {
			setExternalExeConfigTemplate(value);
		} else if (EXTERNAL_EXE_TMP_FILE_ROOT_DIR.equals(key)) {
			setExternalExeTmpFileRootDir(value);
		} else if (EXTERNAL_EXE_TIME_OUT.equals(key)) {
			setExternalExeTimeOut(Long.parseLong(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(MAX_AGENT_PLAN_MEMORY_SIZE, getValue(MAX_AGENT_PLAN_MEMORY_SIZE));
		for (Map.Entry<Id, StrategySettings>  entry : this.settings.entrySet()) {
			map.put(MODULE + entry.getKey().toString(), entry.getValue().getModuleName());
			map.put(MODULE_PROBABILITY + entry.getKey().toString(), Double.toString(entry.getValue().getProbability()));
			if (entry.getValue().getDisableAfter() == -1) {
				map.put(MODULE_DISABALE_AFTER_ITERATION + entry.getKey().toString(), "null");
			} else {
				map.put(MODULE_DISABALE_AFTER_ITERATION + entry.getKey().toString(), Integer.toString(entry.getValue().getDisableAfter()));
			}
			this.addNotNullParameterToMap(map, MODULE_EXE_PATH + entry.getKey());
		}
		this.addNotNullParameterToMap(map, EXTERNAL_EXE_CONFIG_TEMPLATE);
		this.addNotNullParameterToMap(map, EXTERNAL_EXE_TMP_FILE_ROOT_DIR);
		this.addNotNullParameterToMap(map, EXTERNAL_EXE_TIME_OUT);
		return map;
	}

	@Override
	protected void checkConsistency() {
		super.checkConsistency();

		// check that the strategies are numbered from 1 to n
		int nofStrategies = this.settings.size();
		for (int i = 1; i <= nofStrategies; i++) {
			StrategySettings settings = getStrategySettings(new IdImpl(Integer.toString(i)), false);
			if (settings == null) {
				throw new RuntimeException("Loaded " + nofStrategies + " strategies which should be numbered from 1 to n, but could not find strategy " + i);
			}
		}

		// check that each strategy has moduleName set and probability >= 0.0
		for (Map.Entry<Id, StrategySettings> settings : this.settings.entrySet()) {
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
	
	/**
	 * Adds the StrategySettings given as parameter to the map storing the settings for the strategies.
	 * An IllegalArgumentException is thrown, if a StrategySEttings instance with the id of the parameter
	 * already exists in the map.
	 * @param stratSets
	 */
	public void addStrategySettings(StrategySettings stratSets) {
		if (this.settings.containsKey(stratSets.getId())) {
			throw new IllegalArgumentException("A strategy with id: " + stratSets.getId() + " is already configured!");
		}
		this.settings.put(stratSets.getId(), stratSets);
	}

	public Collection<StrategySettings> getStrategySettings() {
		return this.settings.values();
	}

	private StrategySettings getStrategySettings(final Id index, final boolean createIfMissing) {
		StrategySettings settings = this.settings.get(index);
		if (settings == null && createIfMissing) {
			settings = new StrategySettings(index);
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

	public void setExternalExeConfigTemplate(final String externalExeConfigTemplate) {
		this.externalExeConfigTemplate = externalExeConfigTemplate;
	}

	public String getExternalExeConfigTemplate() {
		return this.externalExeConfigTemplate;
	}

	public void setExternalExeTmpFileRootDir(final String externalExeTmpFileRootDir) {
		this.externalExeTmpFileRootDir = externalExeTmpFileRootDir;
	}

	public String getExternalExeTmpFileRootDir() {
		return this.externalExeTmpFileRootDir;
	}

	public void setExternalExeTimeOut(final long externalExeTimeOut) {
		this.externalExeTimeOut = externalExeTimeOut;
	}

	public long getExternalExeTimeOut() {
		return this.externalExeTimeOut;
	}

	public static class StrategySettings {
		private Id id;
		private double probability = -1.0;
		private String moduleName = null;
		private int disableAfter = -1;
		private String exePath = null;

		public StrategySettings(Id id) {
			this.id = id;
		}
		
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

		public void setExePath(final String exePath) {
			this.exePath = exePath;
		}

		public String getExePath() {
			return this.exePath;
		}
		
		public Id getId() {
			return id;
		}
		
		public void setId(Id id) {
			this.id = id;
		}

	}
}
