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

package playground.singapore.typesPopulation.config.groups;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.gbl.Gbl;

import playground.singapore.typesPopulation.population.PersonImplPops;

/**
 * Configuration group for specifying the plans-replanning to be used.
 *
 * @author mrieser
 */
public class StrategyPopsConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "strategy";
	private static final String MAX_AGENT_PLAN_MEMORY_SIZE = "maxAgentPlanMemorySize";
	private static final String MODULE = "Module";
	private static final String MODULE_PROBABILITY = "ModuleProbability";
	private static final String MODULE_DISABLE_AFTER_ITERATION = "ModuleDisableAfterIteration";
	private static final String MODULE_EXE_PATH = "ModuleExePath";
	private static final String EXTERNAL_EXE_CONFIG_TEMPLATE = "ExternalExeConfigTemplate";
	private static final String EXTERNAL_EXE_TMP_FILE_ROOT_DIR = "ExternalExeTmpFileRootDir";
	private static final String EXTERNAL_EXE_TIME_OUT = "ExternalExeTimeOut";

	private Map<String, Integer> maxAgentPlanMemorySize = new HashMap<String, Integer>();
	private String externalExeConfigTemplate = null;
	private String externalExeTmpFileRootDir = null;
	private long externalExeTimeOut = 3600;

	private final Map<String, LinkedHashMap<Id, StrategySettings>> settings = new HashMap<String, LinkedHashMap<Id, StrategySettings>>();
	
	private static final String PLAN_SELECTOR_FOR_REMOVAL = "planSelectorForRemoval" ;
	private Map<String, String> planSelectorForRemoval = new HashMap<String, String>(); 
	// default is configured in StrategyManager; one may wish to change where the default is defined.  kai, feb'12
	
	//---
	private static final String ITERATION_FRACTION_TO_DISABLE_INNOVATION = "fractionOfIterationsToDisableInnovation" ;
	private Map<String, Double> fraction = new HashMap<String, Double>();
	//---

	public StrategyPopsConfigGroup() {
		super(GROUP_NAME);
	}
	
	public Collection<String> getPopulationIds() {
		return settings.keySet();
	}
	@Override
	public String getValue(final String key) {
		if(key!=null) {
			String[] parts = key.split("_");
			if(MAX_AGENT_PLAN_MEMORY_SIZE.equals(parts[0]))
				return Integer.toString(getMaxAgentPlanMemorySize(parts.length>1?parts[1]:PersonImplPops.DEFAULT_POP));
			else if(EXTERNAL_EXE_CONFIG_TEMPLATE.equals(parts[0]))
				return getExternalExeConfigTemplate();
			else if(EXTERNAL_EXE_TMP_FILE_ROOT_DIR.equals(parts[0]))
				return getExternalExeTmpFileRootDir();
			else if(EXTERNAL_EXE_TIME_OUT.equals(parts[0]))
				return Long.toString(getExternalExeTimeOut());
			else if( PLAN_SELECTOR_FOR_REMOVAL.equals(parts[0]) || ITERATION_FRACTION_TO_DISABLE_INNOVATION.equals(parts[0]))
				throw new RuntimeException("please use direct getter") ;
			else if(parts.length == 2)
				parts = new String[]{parts[0],PersonImplPops.DEFAULT_POP,parts[1]};
			StrategySettings settings = getStrategySettings(parts[1], Id.create(parts[2],StrategySettings.class), false);
			if (settings == null)
				return null;
			else {
				if(parts[0].equals(MODULE))
					return settings.getModuleName();
				else if(parts[0].equals(MODULE_PROBABILITY))
					return Double.toString(settings.getProbability());
				else if(parts[0].equals(MODULE_DISABLE_AFTER_ITERATION)) {
					if (settings.getDisableAfter()==-1)
						return null;
					return Integer.toString(settings.getDisableAfter());
				}
				else if(parts[0].equals(MODULE_EXE_PATH))
					return settings.getExePath();
			}
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if(key!=null) {
			String[] parts = key.split("_");
			if (parts[0].equals(MAX_AGENT_PLAN_MEMORY_SIZE))
				setMaxAgentPlanMemorySize(parts.length>1?parts[1]:PersonImplPops.DEFAULT_POP, Integer.parseInt(value));
			else if (parts[0].equals(PLAN_SELECTOR_FOR_REMOVAL))
				setPlanSelectorForRemoval(parts.length>1?parts[1]:PersonImplPops.DEFAULT_POP, value) ;
			else if (parts[0].equals(ITERATION_FRACTION_TO_DISABLE_INNOVATION))
				setFractionOfIterationsToDisableInnovation(parts.length>1?parts[1]:PersonImplPops.DEFAULT_POP, Double.parseDouble(value));
			else if (parts[0].equals(EXTERNAL_EXE_CONFIG_TEMPLATE))
				setExternalExeConfigTemplate(value);
			else if (parts[0].equals(EXTERNAL_EXE_TMP_FILE_ROOT_DIR))
				setExternalExeTmpFileRootDir(value);
			else if (parts[0].equals(EXTERNAL_EXE_TIME_OUT))
				setExternalExeTimeOut(Long.parseLong(value));
			else {
				if(parts.length == 2)
					parts = new String[]{parts[0],PersonImplPops.DEFAULT_POP,parts[1]};
				StrategySettings settings = getStrategySettings(parts[1], Id.create(parts[2],StrategySettings.class), true);
				if (settings != null) {
					if(parts[0].equals(MODULE))
						settings.setModuleName(value);
					else if(parts[0].equals(MODULE_PROBABILITY))
						settings.setProbability(Double.parseDouble(value));
					else if(parts[0].equals(MODULE_DISABLE_AFTER_ITERATION))
						settings.setDisableAfter(Integer.parseInt(value));
					else if (parts[0].equals(MODULE_EXE_PATH))
						settings.setExePath(value);
				}
				else
					throw new IllegalArgumentException(key);
			}
		}
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for(Entry<String, LinkedHashMap<Id, StrategySettings>> popSettingsE:this.settings.entrySet()) {
			map.put(MAX_AGENT_PLAN_MEMORY_SIZE+"_"+popSettingsE.getKey(), getValue(MAX_AGENT_PLAN_MEMORY_SIZE+"_"+popSettingsE.getKey()));
			for (Map.Entry<Id, StrategySettings>  entry : popSettingsE.getValue().entrySet()) {
				map.put(MODULE+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), entry.getValue().getModuleName());
				map.put(MODULE_PROBABILITY+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), Double.toString(entry.getValue().getProbability()));
				if (entry.getValue().getDisableAfter() == -1) {
					map.put(MODULE_DISABLE_AFTER_ITERATION+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), "null");
				} else {
					map.put(MODULE_DISABLE_AFTER_ITERATION+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), Integer.toString(entry.getValue().getDisableAfter()));
				}
				this.addParameterToMap(map, MODULE_EXE_PATH+"_"+popSettingsE.getKey()+"_"+entry.getKey());
			}
			map.put(PLAN_SELECTOR_FOR_REMOVAL+"_"+popSettingsE.getKey(), this.getPlanSelectorForRemoval(popSettingsE.getKey()) ) ;
			map.put(ITERATION_FRACTION_TO_DISABLE_INNOVATION+"_"+popSettingsE.getKey(),  Double.toString(this.getFractionOfIterationsToDisableInnovation(popSettingsE.getKey())) ) ;
		}
		this.addParameterToMap(map, EXTERNAL_EXE_CONFIG_TEMPLATE);
		this.addParameterToMap(map, EXTERNAL_EXE_TMP_FILE_ROOT_DIR);
		this.addParameterToMap(map, EXTERNAL_EXE_TIME_OUT);
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		for(Entry<String, LinkedHashMap<Id, StrategySettings>> popSettingsE:this.settings.entrySet()) {
			map.put(ITERATION_FRACTION_TO_DISABLE_INNOVATION, "fraction of iterations where innovative strategies are switched off.  Something link 0.8 should be good.  E.g. if you run from iteration 400 to iteration 500, innovation is switched off at iteration 480" ) ;
			map.put(MAX_AGENT_PLAN_MEMORY_SIZE, "maximum number of plans per agent.  ``0'' means ``infinity''.  Currently (2010), ``5'' is a good number");
			int cnt = 0 ;
			for (Map.Entry<Id, StrategySettings>  entry : popSettingsE.getValue().entrySet()) {
				cnt++ ;
				if ( cnt==1 ) {
					// put comments only for the first strategy to improve readability
					map.put(MODULE+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), "name of strategy (if not full class name, resolved in StrategyManagerConfigLoader)");
					map.put(MODULE_PROBABILITY+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), "probability that a strategy is applied to a given a person.  despite its name, this really is a ``weight''");
					map.put(MODULE_DISABLE_AFTER_ITERATION+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), "iteration after which module will be disabled.  most useful for ``innovative'' strategies (new routes, new times, ...)");
					map.put(MODULE_EXE_PATH+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), "path to external executable (if applicable)" ) ;
				} else {
					map.put(MODULE+"_"+popSettingsE.getKey()+"_"+entry.getKey().toString(), Gbl.SEPARATOR ); 
				}
	
			}
			map.put(PLAN_SELECTOR_FOR_REMOVAL,"name of PlanSelector for plans removal.  If not full class name, resolved in " +
					"StrategyManagerConfigLoader.  default is `null', which eventually calls SelectWorstPlan. This is not a good " +
					"choice from a discrete choice theoretical perspective. Alternatives, however, have not been systematically " +
					"tested. kai, feb'12") ;
		}
		return map ;
	}

	@Override
	protected void checkConsistency() {
		super.checkConsistency();

		// check that the strategies are numbered from 1 to n
		for(Entry<String, LinkedHashMap<Id, StrategySettings>> popSettingsE:this.settings.entrySet()) {
			int nofStrategies = popSettingsE.getValue().size();
			for (int i = 1; i <= nofStrategies; i++) {
				StrategySettings settings = getStrategySettings(popSettingsE.getKey(), Id.create(Integer.toString(i),StrategySettings.class), false);
				if (settings == null) {
					throw new RuntimeException("Loaded " + nofStrategies + " strategies which should be numbered from 1 to n, but could not find strategy " + i);
				}
			}
		}

		// check that each strategy has moduleName set and probability >= 0.0
		for(Entry<String, LinkedHashMap<Id, StrategySettings>> popSettingsE:this.settings.entrySet())
			for (Map.Entry<Id, StrategySettings> settings : popSettingsE.getValue().entrySet()) {
				if (settings.getValue().getModuleName() == null) {
					throw new RuntimeException("Strategy "+"_"+popSettingsE.getKey()+"_"+settings.getKey() + " has no module set (Missing 'Module_" + settings.getKey() + "').");
				}
				if (settings.getValue().getModuleName().length() == 0) {
					throw new RuntimeException("Strategy "+"_"+popSettingsE.getKey()+"_"+settings.getKey() + " has no module set ('Module_" + settings.getKey() + "' is empty).");
				}
				if (settings.getValue().getProbability() < 0.0) {
					throw new RuntimeException("Probability for strategy "+"_"+popSettingsE.getKey()+"_"+settings.getKey() + " must be >= 0.0 (Maybe 'ModuleProbability_" + settings.getKey() + "') is missing?");
				}
			}
	}

	/**
	 * Adds the StrategySettings given as parameter to the map storing the settings for the strategies.
	 * An IllegalArgumentException is thrown, if a StrategySEttings instance with the id of the parameter
	 * already exists in the map.
	 * @param stratSets
	 */
	public void addStrategySettings(String populationId, final StrategySettings stratSets) {
		if (this.settings.get(populationId)!=null && this.settings.get(populationId).containsKey(stratSets.getId())) {
			throw new IllegalArgumentException("A strategy with id: " + stratSets.getId() + " is already configured!");
		}
		LinkedHashMap<Id, StrategySettings> popSettings = this.settings.get(populationId);
		if(popSettings==null) {
			popSettings = new LinkedHashMap<Id, StrategySettings>();
			this.settings.put(populationId, popSettings);
		}
		popSettings.put(stratSets.getId(), stratSets);
	}

	public Collection<StrategySettings> getStrategySettings(String populationId) {
		return this.settings.get(populationId).values();
	}

	private StrategySettings getStrategySettings(String populationId, final Id index, final boolean createIfMissing) {
		LinkedHashMap<Id, StrategySettings> popSettings = this.settings.get(populationId);
		if(popSettings == null && createIfMissing) {
			popSettings = new LinkedHashMap<Id, StrategySettings>();
			this.settings.put(populationId, popSettings);
		}
		else if(popSettings == null)
			return null;
		StrategySettings settings = popSettings.get(index);
		if (settings == null && createIfMissing) {
			settings = new StrategySettings(index);
			popSettings.put(index, settings);
		}
		return settings;
	}

	public void setMaxAgentPlanMemorySize(String populationId, final int maxAgentPlanMemorySize) {
		this.maxAgentPlanMemorySize.put(populationId, maxAgentPlanMemorySize);
	}

	public int getMaxAgentPlanMemorySize(String populationId) {
		Integer size = this.maxAgentPlanMemorySize.get(populationId);
		return size==null?1:size;
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

	public static class StrategySettings implements MatsimParameters {
		private Id id;
		private double probability = -1.0;
		private String moduleName = null;
		private int disableAfter = -1;
		private String exePath = null;

		public StrategySettings(final Id id) {
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
			return this.id;
		}

	}

	public String getPlanSelectorForRemoval(String populationId) {
		return planSelectorForRemoval.get(populationId);
	}

	public void setPlanSelectorForRemoval(String populationId, String planSelectorForRemoval) {
		this.planSelectorForRemoval.put(populationId, planSelectorForRemoval);
	}

	public double getFractionOfIterationsToDisableInnovation(String populationId) {
		Double fraction = this.fraction.get(populationId);
		return fraction==null?Double.POSITIVE_INFINITY:fraction;
	}

	public void setFractionOfIterationsToDisableInnovation(String populationId, double fraction) {
		this.fraction.put(populationId, fraction);
	}
}
