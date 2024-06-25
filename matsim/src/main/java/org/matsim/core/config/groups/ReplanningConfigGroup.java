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

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultPlansRemover;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

import java.util.Collection;
import java.util.Map;

/**
 * Configuration group for specifying the plans-replanning to be used.
 * It can still be specified using the "underscored" way, but can only be written
 * in hierarchical format "v2".
 * Trying to write v1 will result in the strategy settings being silently lost!
 *
 * @author mrieser
 */
public final class ReplanningConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "replanning";

	// in the following, it is still named "module", for the following reason:
	// the "right" side is the outside interface, used in the config files, which is left with the old keys for backwards compatibility.
	// kai/mz, dec'14
	private static final String MODULE = "Module_";
	private static final String MODULE_PROBABILITY = "ModuleProbability_";
	private static final String MODULE_DISABLE_AFTER_ITERATION = "ModuleDisableAfterIteration_";
	private static final String MODULE_EXE_PATH = "ModuleExePath_";
	private static final String MODULE_SUBPOPULATION = "ModuleSubpopulation_";

	private final ReflectiveDelegate delegate = new ReflectiveDelegate();
	// yy could you please describe why this indirect design was done?  Was ist just because it made refactoring easier, or does it provide
	// an advantage or is even necessary?  Thanks ...  kai, oct'14
	// To maintain backwards compatibility (underscored parameters), one needs to override the "getValue" and "add_param" methods,
	// which are final in ReflectiveModule. However, using reflective module for the
	// rest of the parameters still made refactoring much easier. So let's call
	// this "necessary". td, apr'15

	public static class StrategySettings extends ReflectiveConfigGroup implements MatsimParameters {
		public static final String SET_NAME = "strategysettings";
		private Id<StrategySettings> id;
		private double probability = -1.0;
		private String strategyName = null;
		private int disableAfter = -1;
		private String exePath = null;
		private String subpopulation = null;

		public StrategySettings() {
			this( Id.create( MatsimRandom.getRandom().nextLong(), StrategySettings.class) );
		}

		@Deprecated // use empty constructor. kai/mz, nov'15
		public StrategySettings(final Id<StrategySettings> id) {
			super( SET_NAME );
			this.id = id;
//			this.strategyName = id.toString() ; // safety net, can be overridden by (also deprecated) setStrategyName(...).  kai/mz, nov'15
			// putting the above into the code fails at least one test.  We would vote for removing that test ...
		}

		@Override
		public final Map<String, String> getComments() {
			Map<String,String> map = super.getComments();

			// put comments only for the first strategy to improve readability
			// I think that the above comment is a todo, not a description of the code status. kai, feb'15

			StringBuilder sels = new StringBuilder() ;
			sels.append( DefaultSelector.SelectRandom ) ;
			sels.append( ' ' );
			sels.append( DefaultSelector.BestScore ) ;
			sels.append( ' ' );
			sels.append( DefaultSelector.KeepLastSelected ) ;
			sels.append( ' ' );
			sels.append( DefaultSelector.ChangeExpBeta ) ;
			sels.append( ' ' );
			sels.append( DefaultSelector.SelectExpBeta ) ;
			sels.append( ' ' );
			sels.append( DefaultSelector.SelectPathSizeLogit ) ;

			StringBuilder strats = new StringBuilder() ;
			strats.append( DefaultStrategy.ReRoute ) ;
			sels.append( ' ' );
			strats.append( DefaultStrategy.TimeAllocationMutator ) ;
			sels.append( ' ' );
			strats.append( DefaultStrategy.TimeAllocationMutator_ReRoute ) ;
			sels.append( ' ' );
			strats.append( DefaultStrategy.ChangeSingleTripMode ) ;
			sels.append( ' ' );
			strats.append( DefaultStrategy.ChangeTripMode ) ;
			sels.append( ' ' );
			strats.append( DefaultStrategy.SubtourModeChoice ) ;

			map.put( "strategyName",
					"strategyName of strategy.  Possible default names: " + sels + " (selectors), " + strats + " (innovative strategies)." );
			map.put( "weight",
					"weight of a strategy: for each agent, a strategy will be selected with a probability proportional to its weight");
			map.put( "disableAfterIteration",
					"iteration after which strategy will be disabled.  most useful for ``innovative'' strategies (new routes, new times, ...). "
					+ "Normally, better use fractionOfIterationsToDisableInnovation");
			map.put( "executionPath",
					"path to external executable (if applicable)" ) ;
			map.put( "subpopulation",
					"subpopulation to which the strategy applies. \"null\" refers to the default population, that is, the set of persons for which no explicit subpopulation is defined (ie no subpopulation attribute)" ) ;

			return map ;
		}

		@Override
		protected void checkConsistency(Config config) {
			super.checkConsistency(config);

			if ( getStrategyName() == null || getStrategyName().length() == 0 ) {
				throw new RuntimeException("Strategy strategyName is not set");
			}
			if ( getWeight() < 0.0 ) {
				throw new RuntimeException("Weight for strategy " + getStrategyName() + " must be >= 0.0" );
			}
		}

		@StringSetter( "weight" )
		public StrategySettings setWeight(final double probability) {
			this.probability = probability;
			return this ;
		}

		@StringGetter( "weight" )
		public double getWeight() {
			return this.probability;
		}

		@StringSetter( "strategyName" )
		public StrategySettings setStrategyName(final String name) {
			this.strategyName = name;
			return this ;
		}

		@StringGetter( "strategyName" )
		public String getStrategyName() {
			return this.strategyName;
		}

		@StringSetter( "disableAfterIteration" )
		public StrategySettings setDisableAfter(final int disableAfter) {
			this.disableAfter = disableAfter;
			return this ;
		}

		@StringGetter( "disableAfterIteration" )
		public int getDisableAfter() {
			return this.disableAfter;
		}

		@StringSetter( "executionPath" )
		public StrategySettings setExePath(final String exePath) {
			this.exePath = exePath;
			return this ;
		}

		@StringGetter( "executionPath" )
		public String getExePath() {
			return this.exePath;
		}

		@Deprecated // not clear if this is only for backwards compatibility (config v1) or should actually be used. kai/mz, nov'15
		public Id<StrategySettings> getId() {
			return this.id;
		}

		@StringSetter( "subpopulation" )
		public StrategySettings setSubpopulation(final String subpopulation) {
			this.subpopulation = subpopulation;
			return this ;
		}

		@StringGetter( "subpopulation" )
		public String getSubpopulation() {
			return subpopulation;
		}
	}

	public ReplanningConfigGroup() {
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

		return delegate.getValue( key );
	}

	@Override
	public void addParam(final String key, final String value) {
		// adding underscore parameters is still supported for backward compatibility.
		if (key != null && key.startsWith(MODULE)) {
			StrategySettings settings = getStrategySettings(Id.create(key.substring(MODULE.length()), StrategySettings.class), true);
			settings.addParam( "strategyName" , value);
		}
		else if (key != null && key.startsWith(MODULE_PROBABILITY)) {
			StrategySettings settings = getStrategySettings(Id.create(key.substring(MODULE_PROBABILITY.length()), StrategySettings.class), true);
			settings.addParam( "weight" , value );
		}
		else if (key != null && key.startsWith(MODULE_DISABLE_AFTER_ITERATION)) {
			StrategySettings settings = getStrategySettings(Id.create(key.substring(MODULE_DISABLE_AFTER_ITERATION.length()), StrategySettings.class), true);
			settings.setDisableAfter(Integer.parseInt(value));
			settings.addParam( "disableAfterIteration" , value );
		}
		else if (key != null && key.startsWith(MODULE_EXE_PATH)) {
			StrategySettings settings = getStrategySettings(Id.create(key.substring(MODULE_EXE_PATH.length()), StrategySettings.class), true);
			settings.addParam( "executionPath" , value );
		}
		else if (key != null && key.startsWith(MODULE_SUBPOPULATION)) {
			StrategySettings settings = getStrategySettings(Id.create(key.substring(MODULE_SUBPOPULATION.length()), StrategySettings.class), true);
			settings.addParam( "subpopulation" , value );
		}
		else {
			delegate.addParam( key , value );
		}
	}

	private StrategySettings getStrategySettings(final Id<StrategySettings> index, final boolean createIfMissing) {
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


	/////////////////////////////////
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ReflectiveDelegate.ITERATION_FRACTION_TO_DISABLE_INNOVATION, "fraction of iterations where innovative strategies are switched off.  Something like 0.8 should be good.  E.g. if you run from iteration 400 to iteration 500, innovation is switched off at iteration 480. If the ReplanningAnnealer is used, it will also be switched off." ) ;
		map.put(ReflectiveDelegate.MAX_AGENT_PLAN_MEMORY_SIZE, "maximum number of plans per agent.  ``0'' means ``infinity''.  Currently (2010), ``5'' is a good number");

		StringBuilder strb = new StringBuilder() ;
		for ( DefaultPlansRemover name : DefaultPlansRemover.values() ) {
			strb.append( name.toString() + " " ) ;
		}
		map.put(ReflectiveDelegate.PLAN_SELECTOR_FOR_REMOVAL,"strategyName of PlanSelector for plans removal.  "
				+ "Possible defaults: " + strb.toString() + ". The current default, WorstPlanSelector is not a good " +
				"choice from a discrete choice theoretical perspective. Alternatives, however, have not been systematically " +
				"tested. kai, feb'12") ;

		map.put(ReflectiveDelegate.EXTERNAL_EXE_CONFIG_TEMPLATE,"the external executable will be called with a config file as argument.  This is the pathname to a possible "
				+ "skeleton config, to which additional information will be added.  Can be null.");
		map.put(ReflectiveDelegate.EXTERNAL_EXE_TMP_FILE_ROOT_DIR, "root directory for temporary files generated by the external executable. Provided as a service; "
				+ "I don't think this is used by MATSim.") ;
		map.put(ReflectiveDelegate.EXTERNAL_EXE_TIME_OUT, "time out value (in seconds) after which matsim will consider the external strategy as failed") ;
		return map ;
	}

	@Override
	protected void checkParameterSet(final ConfigGroup set) {
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
	public ConfigGroup createParameterSet(final String type) {
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

	public final void clearStrategySettings() {
		this.clearParameterSetsForType( StrategySettings.SET_NAME ) ;
	}

	public Collection<StrategySettings> getStrategySettings() {
		// This does look pretty wrong, but is actually OK,
		// as the checkParameterSet method checks that strategy settings
		// parameter sets which are added have the proper type.
		// A cleaner solution would be nice, though... td, sep'14
		return (Collection<StrategySettings>) getParameterSets(StrategySettings.SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		// to make available to tests
		super.checkConsistency(config);
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
		switch ( planSelectorForRemoval ) {
		case "SelectExpBeta" :
			throw new RuntimeException("'SelectExpBeta' was replaced by 'SelectExpBetaForRemoval' in the plans removal setting" ) ;
		case "ChangeExpBeta" :
			throw new RuntimeException("'ChangeExpBeta' was replaced by 'ChangeExpBetaForRemoval' in the plans removal setting" ) ;
		case "PathSizeLogitSelector" :
			throw new RuntimeException("'PathSizeLogitSelector' was replaced by 'PathSizeLogitSelectorForRemoval' in the plans removal setting" ) ;
		default:
			delegate.setPlanSelectorForRemoval(planSelectorForRemoval) ;
		}
	}

	public double getFractionOfIterationsToDisableInnovation() {
		return delegate.getFractionOfIterationsToDisableInnovation();
	}

	public void setFractionOfIterationsToDisableInnovation(double fraction) {
		delegate.setFractionOfIterationsToDisableInnovation(fraction);
	}

	@Override
	public final Map<String, String> getParams() {
		return delegate.getParams();
	}

	private static class ReflectiveDelegate extends ReflectiveConfigGroup {
		 static final String MAX_AGENT_PLAN_MEMORY_SIZE = "maxAgentPlanMemorySize";
		 static final String EXTERNAL_EXE_CONFIG_TEMPLATE = "ExternalExeConfigTemplate";
		 static final String EXTERNAL_EXE_TMP_FILE_ROOT_DIR = "ExternalExeTmpFileRootDir";
		 static final String EXTERNAL_EXE_TIME_OUT = "ExternalExeTimeOut";
		 static final String ITERATION_FRACTION_TO_DISABLE_INNOVATION = "fractionOfIterationsToDisableInnovation" ;
		 static final String PLAN_SELECTOR_FOR_REMOVAL = "planSelectorForRemoval" ;

		private int maxAgentPlanMemorySize = 5;
		private String externalExeConfigTemplate = null;
		private String externalExeTmpFileRootDir = null;
		private long externalExeTimeOut = 3600;

		private String planSelectorForRemoval = "WorstPlanSelector";

		//---
		private double fraction = Double.POSITIVE_INFINITY ;
		//---

		public ReflectiveDelegate() {
			super( ReplanningConfigGroup.GROUP_NAME );
		}


		@StringSetter( MAX_AGENT_PLAN_MEMORY_SIZE )
		public void setMaxAgentPlanMemorySize(final int maxAgentPlanMemorySize) {
			this.maxAgentPlanMemorySize = maxAgentPlanMemorySize;
		}

		@StringGetter( MAX_AGENT_PLAN_MEMORY_SIZE )
		public int getMaxAgentPlanMemorySize() {
			return this.maxAgentPlanMemorySize;
		}

		@StringSetter( EXTERNAL_EXE_CONFIG_TEMPLATE )
		public void setExternalExeConfigTemplate(final String externalExeConfigTemplate) {
			this.externalExeConfigTemplate = externalExeConfigTemplate;
		}

		@StringGetter( EXTERNAL_EXE_CONFIG_TEMPLATE )
		public String getExternalExeConfigTemplate() {
			return this.externalExeConfigTemplate;
		}

		@StringSetter( EXTERNAL_EXE_TMP_FILE_ROOT_DIR )
		public void setExternalExeTmpFileRootDir(final String externalExeTmpFileRootDir) {
			this.externalExeTmpFileRootDir = externalExeTmpFileRootDir;
		}

		@StringGetter( EXTERNAL_EXE_TMP_FILE_ROOT_DIR )
		public String getExternalExeTmpFileRootDir() {
			return this.externalExeTmpFileRootDir;
		}

		@StringSetter( EXTERNAL_EXE_TIME_OUT )
		public void setExternalExeTimeOut(final long externalExeTimeOut) {
			this.externalExeTimeOut = externalExeTimeOut;
		}

		@StringGetter( EXTERNAL_EXE_TIME_OUT )
		public long getExternalExeTimeOut() {
			return this.externalExeTimeOut;
		}

		@StringGetter( PLAN_SELECTOR_FOR_REMOVAL )
		public String getPlanSelectorForRemoval() {
			return planSelectorForRemoval;
		}

		@StringSetter( PLAN_SELECTOR_FOR_REMOVAL )
		public void setPlanSelectorForRemoval(String planSelectorForRemoval) {
			this.planSelectorForRemoval = planSelectorForRemoval;
		}

		@StringGetter( ITERATION_FRACTION_TO_DISABLE_INNOVATION )
		public double getFractionOfIterationsToDisableInnovation() {
			return fraction;
		}

		@StringSetter( ITERATION_FRACTION_TO_DISABLE_INNOVATION )
		public void setFractionOfIterationsToDisableInnovation(double fraction) {
			this.fraction = fraction;
		}
	}
}

