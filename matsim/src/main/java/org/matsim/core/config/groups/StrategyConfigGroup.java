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

package org.matsim.core.config.groups;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Module;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Configuration group for specifying the plans-replanning to be used.
 * It can still be specified using the "underscored" way, but can only be written
 * in hierarchical format "v2".
 * Trying to write v1 will result in the strategy settings being silently lost!
 *
 * @author mrieser
 */
public class StrategyConfigGroup extends Module {

	public static final String GROUP_NAME = "strategy";
	private static final String MODULE = "Module_";
	private static final String MODULE_PROBABILITY = "ModuleProbability_";
	private static final String MODULE_DISABLE_AFTER_ITERATION = "ModuleDisableAfterIteration_";
	private static final String MODULE_EXE_PATH = "ModuleExePath_";
	private static final String MODULE_SUBPOPULATION = "ModuleSubpopulation_";

	private final NonFlatStrategyConfigGroup delegate = new NonFlatStrategyConfigGroup();
	
	public StrategyConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		// first check if the parameter is in "underscored" form
		if ( key.startsWith(MODULE)
				|| key.startsWith(MODULE_PROBABILITY)
				|| key.startsWith(MODULE_DISABLE_AFTER_ITERATION)
				|| key.startsWith(MODULE_EXE_PATH)
				|| key.startsWith(MODULE_SUBPOPULATION) ) {
			throw new IllegalArgumentException( "getting underscored parameter "+key+" is not allowed anymore. The supported way to get those parameters is via parameter sets." );
		}

		// if not, ask delegate.
		return delegate.getValue( key );
	}

	@Override
	public void addParam(final String key, final String value) {
		// adding underscore parameters is still supported for backward compatibility.
		if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE.length())), true);
			settings.setModuleName(value);
		}
		else if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_PROBABILITY.length())), true);
			settings.setProbability(Double.parseDouble(value));
		}
		else if (key != null && key.startsWith(MODULE_DISABLE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_DISABLE_AFTER_ITERATION.length())), true);
			settings.setDisableAfter(Integer.parseInt(value));
		}
		else if (key != null && key.startsWith(MODULE_EXE_PATH)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_EXE_PATH.length())), true);
			settings.setExePath(value);
		}
		else if (key != null && key.startsWith(MODULE_SUBPOPULATION)) {
			StrategySettings settings = getStrategySettings(new IdImpl(key.substring(MODULE_SUBPOPULATION.length())), true);
			settings.setSubpopulation(value);
		}
		else {
			delegate.addParam( key , value );
		}
	}

	@Override
	protected void checkConsistency() {
		delegate.checkConsistency();
	}

	/**
	 * Adds the StrategySettings given as parameter to the map storing the settings for the strategies.
	 * An IllegalArgumentException is thrown, if a StrategySEttings instance with the id of the parameter
	 * already exists in the map.
	 * @param stratSets
	 */
	public void addStrategySettings(final StrategySettings stratSets) {
		this.delegate.addStrategySettings( stratSets );
	}

	public Collection<StrategySettings> getStrategySettings() {
		return this.delegate.getStrategySettings();
	}

	private StrategySettings getStrategySettings(final Id index, final boolean createIfMissing) {
		StrategySettings settings = null;

		// should be in a map, but it is difficult to keep consistency with the
		// delegate...
		for ( StrategySettings s : getStrategySettings() ) {
			if ( !s.getId().equals( index ) ) continue;
			if ( settings != null ) throw new IllegalStateException( "several settings with id "+index );
			settings = s;
		}

		if (settings == null && createIfMissing) {
			settings = new StrategySettings(index);
			addStrategySettings( settings );
		}

		return settings;
	}

	public static class StrategySettings extends ReflectiveModule implements MatsimParameters {
		public static final String SET_NAME = "strategysettings";
		private Id id;
		private double probability = -1.0;
		private String moduleName = null;
		private int disableAfter = -1;
		private String exePath = null;
		private String subpopulation = null;

		public StrategySettings() {
			this( new IdImpl( MatsimRandom.getRandom().nextLong() ) );
		}

		public StrategySettings(final Id id) {
			super( SET_NAME );
			this.id = id;
		}

		@Override
		public final Map<String, String> getComments() {
			Map<String,String> map = super.getComments();

			// put comments only for the first strategy to improve readability
			map.put( "moduleName",
					"name of strategy (if not full class name, resolved in StrategyManagerConfigLoader)");
			map.put( "probability",
					"probability that a strategy is applied to a given a person.  despite its name, this really is a ``weight''");
			map.put( "disableAfterIteration",
					"iteration after which module will be disabled.  most useful for ``innovative'' strategies (new routes, new times, ...)");
			map.put( "executionPath",
					"path to external executable (if applicable)" ) ;
			map.put( "subpopulation",
					"subpopulation to which the module applies. \"null\" refers to the default population, that is, the set of persons for which no explicit subpopulation is defined (ie no subpopulation attribute)" ) ;

			return map ;
		}

		@Override
		protected void checkConsistency() {
			super.checkConsistency();

			if ( getModuleName() == null || getModuleName().length() == 0 ) {
				throw new RuntimeException("Strategy has no module set");
			}
			if ( getProbability() < 0.0 ) {
				throw new RuntimeException("Probability for strategy " + getModuleName() + " must be >= 0.0" ); 
			}
		}

		@StringSetter( "probability" )
		public void setProbability(final double probability) {
			this.probability = probability;
		}

		@StringGetter( "probability" )
		public double getProbability() {
			return this.probability;
		}

		@StringSetter( "moduleName" )
		public void setModuleName(final String moduleName) {
			this.moduleName = moduleName;
		}

		@StringGetter( "moduleName" )
		public String getModuleName() {
			return this.moduleName;
		}

		@StringSetter( "disableAfterIteration" )
		public void setDisableAfter(final int disableAfter) {
			this.disableAfter = disableAfter;
		}

		@StringGetter( "disableAfterIteration" )
		public int getDisableAfter() {
			return this.disableAfter;
		}

		@StringSetter( "executionPath" )
		public void setExePath(final String exePath) {
			this.exePath = exePath;
		}

		@StringGetter( "executionPath" )
		public String getExePath() {
			return this.exePath;
		}

		public Id getId() {
			return this.id;
		}

		@StringSetter( "subpopulation" )
		public void setSubpopulation(final String subpopulation) {
			this.subpopulation = subpopulation;
		}

		@StringGetter( "subpopulation" )
		public String getSubpopulation() {
			return subpopulation;
		}
	}

	// ///////////////////////////////////////////////////////////
	// pure delegation
	@Override
	public Map<String, String> getParams() {
		return delegate.getParams();
	}

	@Override
	public final Map<String, String> getComments() {
		return delegate.getComments();
	}


	public Module createParameterSet(String type) {
		return delegate.createParameterSet(type);
	}

	public void setMaxAgentPlanMemorySize(int maxAgentPlanMemorySize) {
		delegate.setMaxAgentPlanMemorySize(maxAgentPlanMemorySize);
	}

	public int getMaxAgentPlanMemorySize() {
		return delegate.getMaxAgentPlanMemorySize();
	}

	public void setExternalExeConfigTemplate(String externalExeConfigTemplate) {
		delegate.setExternalExeConfigTemplate(externalExeConfigTemplate);
	}

	public String getExternalExeConfigTemplate() {
		return delegate.getExternalExeConfigTemplate();
	}

	public void setExternalExeTmpFileRootDir(String externalExeTmpFileRootDir) {
		delegate.setExternalExeTmpFileRootDir(externalExeTmpFileRootDir);
	}

	public String getExternalExeTmpFileRootDir() {
		return delegate.getExternalExeTmpFileRootDir();
	}

	public void setExternalExeTimeOut(long externalExeTimeOut) {
		delegate.setExternalExeTimeOut(externalExeTimeOut);
	}

	public long getExternalExeTimeOut() {
		return delegate.getExternalExeTimeOut();
	}

	public String getPlanSelectorForRemoval() {
		return delegate.getPlanSelectorForRemoval();
	}

	public void setPlanSelectorForRemoval(String planSelectorForRemoval) {
		delegate.setPlanSelectorForRemoval(planSelectorForRemoval);
	}

	public double getFractionOfIterationsToDisableInnovation() {
		return delegate.getFractionOfIterationsToDisableInnovation();
	}

	public void setFractionOfIterationsToDisableInnovation(double fraction) {
		delegate.setFractionOfIterationsToDisableInnovation(fraction);
	}
}

class NonFlatStrategyConfigGroup extends ReflectiveModule {
	private static final String MAX_AGENT_PLAN_MEMORY_SIZE = "maxAgentPlanMemorySize";
	private static final String EXTERNAL_EXE_CONFIG_TEMPLATE = "ExternalExeConfigTemplate";
	private static final String EXTERNAL_EXE_TMP_FILE_ROOT_DIR = "ExternalExeTmpFileRootDir";
	private static final String EXTERNAL_EXE_TIME_OUT = "ExternalExeTimeOut";
	private static final String ITERATION_FRACTION_TO_DISABLE_INNOVATION = "fractionOfIterationsToDisableInnovation" ;
	private static final String PLAN_SELECTOR_FOR_REMOVAL = "planSelectorForRemoval" ;

	private int maxAgentPlanMemorySize = 5;
	private String externalExeConfigTemplate = null;
	private String externalExeTmpFileRootDir = null;
	private long externalExeTimeOut = 3600;

	private String planSelectorForRemoval = null ; 
	// default is configured in StrategyManager; one may wish to change where the default is defined.  kai, feb'12
	
	//---
	private double fraction = Double.POSITIVE_INFINITY ;
	//---


	public NonFlatStrategyConfigGroup() {
		super( StrategyConfigGroup.GROUP_NAME );
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ITERATION_FRACTION_TO_DISABLE_INNOVATION, "fraction of iterations where innovative strategies are switched off.  Something link 0.8 should be good.  E.g. if you run from iteration 400 to iteration 500, innovation is switched off at iteration 480" ) ;
		map.put(MAX_AGENT_PLAN_MEMORY_SIZE, "maximum number of plans per agent.  ``0'' means ``infinity''.  Currently (2010), ``5'' is a good number");
		map.put(PLAN_SELECTOR_FOR_REMOVAL,"name of PlanSelector for plans removal.  If not full class name, resolved in " +
				"StrategyManagerConfigLoader.  default is `null', which eventually calls SelectWorstPlan. This is not a good " +
				"choice from a discrete choice theoretical perspective. Alternatives, however, have not been systematically " +
				"tested. kai, feb'12") ;
		map.put(EXTERNAL_EXE_CONFIG_TEMPLATE,"the external executable will be called with a config file as argument.  This is the pathname to a possible "
				+ "skeleton config, to which additional information will be added.  Can be null.");
		map.put(EXTERNAL_EXE_TMP_FILE_ROOT_DIR, "root directory for temporary files generated by the external executable. Provided as a service; "
				+ "I don't think this is used by MATSim.") ;
		map.put(EXTERNAL_EXE_TIME_OUT, "time out value (in seconds) after which matsim will consider the external module as failed") ;
		return map ;
	}

	@Override
	protected void checkParameterSet(final Module set) {
		switch ( set.getName() ) {
			case StrategySettings.SET_NAME:
				if ( !(set instanceof StrategySettings) ) {
					throw new RuntimeException( set+" is not an instance of StrategySettings" );
				}
				break;
			default:
				throw new IllegalArgumentException( "unknown set type "+set.getName() );
		}
	}

	@Override
	public Module createParameterSet(final String type) {
		switch ( type ) {
			case StrategySettings.SET_NAME:
				return new StrategySettings( );
			default:
				throw new IllegalArgumentException( "unknown set type "+type );
		}
	}

	// the two next method are just convenience methods
	/**
	 * Adds the StrategySettings given as parameter to the map storing the settings for the strategies.
	 * An IllegalArgumentException is thrown, if a StrategySEttings instance with the id of the parameter
	 * already exists in the map.
	 * @param stratSets
	 */
	public void addStrategySettings(final StrategySettings stratSets) {
		addParameterSet( stratSets );
	}

	public Collection<StrategySettings> getStrategySettings() {
		// This does look pretty wrong, but is actually OK,
		// as the checkParameterSet method checks that strategy settings
		// parameter sets which are added have the proper type.
		// A cleaner solution would be nice, though... td, sep'14
		return (Collection<StrategySettings>) getParameterSets(StrategySettings.SET_NAME);
	}

	@StringSetter( "maxAgentPlanMemorySize" )
	public void setMaxAgentPlanMemorySize(final int maxAgentPlanMemorySize) {
		this.maxAgentPlanMemorySize = maxAgentPlanMemorySize;
	}

	@StringGetter( "maxAgentPlanMemorySize" )
	public int getMaxAgentPlanMemorySize() {
		return this.maxAgentPlanMemorySize;
	}

	@StringSetter( "externalExeConfigTemplate" )
	public void setExternalExeConfigTemplate(final String externalExeConfigTemplate) {
		this.externalExeConfigTemplate = externalExeConfigTemplate;
	}

	@StringGetter( "externalExeConfigTemplate" )
	public String getExternalExeConfigTemplate() {
		return this.externalExeConfigTemplate;
	}

	@StringSetter( "externalExeTmpFileRootDir" )
	public void setExternalExeTmpFileRootDir(final String externalExeTmpFileRootDir) {
		this.externalExeTmpFileRootDir = externalExeTmpFileRootDir;
	}

	@StringGetter( "externalExeTmpFileRootDir" )
	public String getExternalExeTmpFileRootDir() {
		return this.externalExeTmpFileRootDir;
	}

	@StringSetter( "externalExeTimeOut" )
	public void setExternalExeTimeOut(final long externalExeTimeOut) {
		this.externalExeTimeOut = externalExeTimeOut;
	}

	@StringGetter( "externalExeTimeOut" )
	public long getExternalExeTimeOut() {
		return this.externalExeTimeOut;
	}

	@StringGetter( "planSelectorForRemoval" )
	public String getPlanSelectorForRemoval() {
		return planSelectorForRemoval;
	}

	@StringSetter( "planSelectorForRemoval" )
	public void setPlanSelectorForRemoval(String planSelectorForRemoval) {
		this.planSelectorForRemoval = planSelectorForRemoval;
	}

	@StringGetter( "fractionOfIterationsToDisableInnovation" )
	public double getFractionOfIterationsToDisableInnovation() {
		return fraction;
	}

	@StringSetter( "fractionOfIterationsToDisableInnovation" )
	public void setFractionOfIterationsToDisableInnovation(double fraction) {
		this.fraction = fraction;
	}

	@Override
	protected void checkConsistency() {
		// to make available to StrategyConfigGroup (not visible otherwise)
		super.checkConsistency();
	}
}
