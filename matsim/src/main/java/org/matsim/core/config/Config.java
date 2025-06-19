/* *********************************************************************** *
 * project: org.matsim.*
 * Config.java
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

package org.matsim.core.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.EventsManagerConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.HouseholdsConfigGroup;
import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanInheritanceConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.config.groups.VehiclesConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.mobsim.hermes.HermesConfigGroup;
import org.matsim.core.replanning.annealing.ReplanningAnnealerConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.run.CreateFullConfig;

/**
 * Stores all configuration settings specified in a configuration file and
 * provides access to the settings at runtime.
 *
 * @see CreateFullConfig
 *
 * @author mrieser
 */
public final class Config implements MatsimExtensionPoint {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	/** Map of all config-groups known to this instance. */
	private final TreeMap<String, ConfigGroup> modules = new TreeMap<>();

	/*
	 * the following members are for the direct access to the core config
	 * groups.
	 */

	// config groups that are in org.matsim.core.config.groups:


	private final List<ConfigConsistencyChecker> consistencyCheckers = new ArrayList<>();

	/** static Logger-instance. */
	private static final Logger log = LogManager.getLogger(Config.class);

	private boolean locked = false;
	private URL context;


	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////

	public Config() {
		try {
			URL currentDir = Paths.get("").toUri().toURL();
			setContext(currentDir);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds all the commonly used config-groups, also known as "core modules",
	 * to this config-instance. This should be called before reading any
	 * configuration from file.
	 */
	public void addCoreModules() {
		this.modules.put(GlobalConfigGroup.GROUP_NAME, new GlobalConfigGroup());

		this.modules.put(ControllerConfigGroup.GROUP_NAME, new ControllerConfigGroup());

		this.modules.put(QSimConfigGroup.GROUP_NAME, new QSimConfigGroup());

		this.modules.put(CountsConfigGroup.GROUP_NAME, new CountsConfigGroup());
		this.modules.put(ScoringConfigGroup.GROUP_NAME, new ScoringConfigGroup());

		this.modules.put(NetworkConfigGroup.GROUP_NAME, new NetworkConfigGroup());

		this.modules.put(PlansConfigGroup.GROUP_NAME, new PlansConfigGroup());

		this.modules.put(HouseholdsConfigGroup.GROUP_NAME, new HouseholdsConfigGroup());

		this.modules.put(EventsManagerConfigGroup.GROUP_NAME, new EventsManagerConfigGroup() );

		this.modules.put(FacilitiesConfigGroup.GROUP_NAME, new FacilitiesConfigGroup());

		this.modules.put(ReplanningConfigGroup.GROUP_NAME, new ReplanningConfigGroup());

		this.modules.put(TravelTimeCalculatorConfigGroup.GROUPNAME, new TravelTimeCalculatorConfigGroup());

		this.modules.put(ScenarioConfigGroup.GROUP_NAME, new ScenarioConfigGroup());

		this.modules.put(RoutingConfigGroup.GROUP_NAME, new RoutingConfigGroup());

		this.modules.put(TimeAllocationMutatorConfigGroup.GROUP_NAME, new TimeAllocationMutatorConfigGroup());

		this.modules.put(VspExperimentalConfigGroup.GROUP_NAME, new VspExperimentalConfigGroup());

		this.modules.put(TransitConfigGroup.GROUP_NAME, new TransitConfigGroup());

		this.modules.put(LinkStatsConfigGroup.GROUP_NAME, new LinkStatsConfigGroup());

		this.modules.put(TransitRouterConfigGroup.GROUP_NAME, new TransitRouterConfigGroup());

		this.modules.put( SubtourModeChoiceConfigGroup.GROUP_NAME , new SubtourModeChoiceConfigGroup() );

		this.modules.put( VehiclesConfigGroup.GROUP_NAME , new VehiclesConfigGroup() ) ;

		this.modules.put(ChangeModeConfigGroup.CONFIG_MODULE, new ChangeModeConfigGroup());

		this.modules.put(HermesConfigGroup.NAME, new HermesConfigGroup());

		this.modules.put(ReplanningAnnealerConfigGroup.GROUP_NAME, new ReplanningAnnealerConfigGroup());

		this.modules.put(PlanInheritanceConfigGroup.GROUP_NAME, new PlanInheritanceConfigGroup());

		this.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		this.addConfigConsistencyChecker(new UnmaterializedConfigGroupChecker());
		this.addConfigConsistencyChecker(new BeanValidationConfigConsistencyChecker());
	}

	/**
	 * Checks each module for consistency, e.g. if the parameters that are
	 * currently set make sense in their combination.
	 */
	public void checkConsistency() {
		for (ConfigGroup m : this.modules.values()) {
			m.checkConsistency(this);
		}
		for (ConfigConsistencyChecker c : this.consistencyCheckers) {
			c.checkConsistency(this);
		}
//        for (Module m : this.modules.values()) {
//            if (m.getClass() == Module.class) {
//                throw new RuntimeException("Config group " + m.getName() + " is present, but has never been read." +
//                        "This is probably an error: You may be expecting functionality which is not available." +
//                        "Maybe you need to add something to the Controler?");
//            }
//        }
	}

	// ////////////////////////////////////////////////////////////////////
	// add / set methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new module / config-group with the specified name.
	 *
	 * @param name
	 *            The name of the config-group to be created.
	 *
	 * @return the newly created config group
	 * @throws IllegalArgumentException
	 *             if a config-group with the specified name already exists.
	 */
	public final ConfigGroup createModule(final String name) {
		if (this.modules.containsKey(name)) {
			throw new IllegalArgumentException("Module " + name + " exists already.");
		}
		ConfigGroup m = new ConfigGroup(name);
		this.modules.put(name, m);
		return m;
	}

	/**
	 * Adds the specified module / config-group with the specified name to the
	 * configuration.
	 * <p></p>
	 * This is the typical way to "materialize" material that, so far, exists only as Map, into a specialized module.
	 * @param specializedConfigModule
	 *
	 * @throws IllegalArgumentException
	 *             if a config-group with the specified name already exists.
	 */
	public final void addModule(final ConfigGroup specializedConfigModule) {
		String name = specializedConfigModule.getName();
		if (name == null || name.isEmpty()) {
			throw new RuntimeException("cannot insert module with empty name") ;
		}
		// The logic is as follows:
		// (1) assume that module is some SpecializedConfigModule that extends Module

		// (2) it is presumably found here from parsing, but as a general Module:
		ConfigGroup m = this.modules.get(name);

		if (m != null) {
			// (3) this is the corresponding test: m is general, module is specialized:
			if (m.getClass() == ConfigGroup.class && specializedConfigModule.getClass() != ConfigGroup.class) {
				// (4) go through everything in m (from parsing) and add it to module:
				copyTo(m, specializedConfigModule);

				// (5) register the resulting module under "name" (which will over-write m):
				this.modules.put(name, specializedConfigModule);
			} else {
				throw new IllegalArgumentException("Module " + name + " exists already.");
			}
		}
		this.modules.put(name, specializedConfigModule);
	}

	private static void copyTo(ConfigGroup source, ConfigGroup destination) {
		for (Map.Entry<String, String> e : source.getParams().entrySet()) {
			destination.addParam(e.getKey(), e.getValue());
		}

		for (Collection<? extends ConfigGroup> sourceSets : source.getParameterSets().values()) {
			for (ConfigGroup sourceSet : sourceSets) {
				ConfigGroup destinationSet = destination.createParameterSet(sourceSet.getName());
				copyTo(sourceSet, destinationSet);
				destination.addParameterSet(destinationSet);
			}
		}
	}

	/**
	 * Removes the specified module / config-group with the specified name from
	 * the configuration. Does nothing if this module was not existing.
	 *
	 * @param name
	 *
	 */
	public final void removeModule(final String name) {
		if (this.modules.containsKey(name)) {
			this.modules.remove(name);
			log.warn("Module \"" + name + "\" is removed manually from config");

		}
	}

	// ////////////////////////////////////////////////////////////////////
	// get methods
	// ////////////////////////////////////////////////////////////////////

	public final TreeMap<String, ConfigGroup> getModules() {
		return this.modules;
	}

	/**
	 * Returns the requested module, or <code>null</code> if the module does not
	 * exist.
	 *
	 * @param moduleName
	 *            name of the requested module
	 * @return requested module
	 */
	@Deprecated // please try to use the "typed" access structures.  kai, nov'16
	public final ConfigGroup getModule(final String moduleName) {
		return this.modules.get(moduleName);
	}

	// ////////////////////////////////////////////////////////////////////
	// print methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_modules=" + this.modules.size() + "]";
	}

	// ////////////////////////////////////////////////////////////////////
	// direct access to modules / groups
	// ////////////////////////////////////////////////////////////////////

	public final GlobalConfigGroup global() {
		return (GlobalConfigGroup) this.getModule(GlobalConfigGroup.GROUP_NAME);
	}

	public final ControllerConfigGroup controller() {
		return (ControllerConfigGroup) this.getModule(ControllerConfigGroup.GROUP_NAME);
	}

	public final CountsConfigGroup counts() {
		return (CountsConfigGroup) this.getModule(CountsConfigGroup.GROUP_NAME);
	}

	public final ScoringConfigGroup scoring() {
		return (ScoringConfigGroup) this.getModule(ScoringConfigGroup.GROUP_NAME);
	}

	public final NetworkConfigGroup network() {
		return (NetworkConfigGroup) this.getModule(NetworkConfigGroup.GROUP_NAME);
	}

	public final PlansConfigGroup plans() {
		return (PlansConfigGroup) this.getModule(PlansConfigGroup.GROUP_NAME);
	}

	public final HouseholdsConfigGroup households() {
		return (HouseholdsConfigGroup) this.getModule(HouseholdsConfigGroup.GROUP_NAME);
	}

	public final FacilitiesConfigGroup facilities() {
		return (FacilitiesConfigGroup) this.getModule(FacilitiesConfigGroup.GROUP_NAME);
	}

    public final ReplanningConfigGroup replanning() {
		return (ReplanningConfigGroup) this.getModule(ReplanningConfigGroup.GROUP_NAME);
	}

	public TravelTimeCalculatorConfigGroup travelTimeCalculator() {
		return (TravelTimeCalculatorConfigGroup) this.getModule(TravelTimeCalculatorConfigGroup.GROUPNAME);
	}

	public ScenarioConfigGroup scenario() {
		return (ScenarioConfigGroup) this.getModule(ScenarioConfigGroup.GROUP_NAME);
	}

	public RoutingConfigGroup routing() {
		return (RoutingConfigGroup) this.getModule(RoutingConfigGroup.GROUP_NAME);
	}

	public VspExperimentalConfigGroup vspExperimental() {
		return (VspExperimentalConfigGroup) this.getModule(VspExperimentalConfigGroup.GROUP_NAME);
	}

	public QSimConfigGroup qsim() {
		return (QSimConfigGroup) this.getModule(QSimConfigGroup.GROUP_NAME);
	}

	public TransitConfigGroup transit() {
		return (TransitConfigGroup) this.getModule(TransitConfigGroup.GROUP_NAME);
	}

	public TransitRouterConfigGroup transitRouter() {
		return (TransitRouterConfigGroup) this.getModule(TransitRouterConfigGroup.GROUP_NAME);
	}

	public LinkStatsConfigGroup linkStats() {
		return (LinkStatsConfigGroup) this.getModule(LinkStatsConfigGroup.GROUP_NAME);
	}

	public TimeAllocationMutatorConfigGroup timeAllocationMutator() {
		return (TimeAllocationMutatorConfigGroup) this.getModule(TimeAllocationMutatorConfigGroup.GROUP_NAME);
	}

	public EventsManagerConfigGroup eventsManager() {
		return (EventsManagerConfigGroup) this.getModule(EventsManagerConfigGroup.GROUP_NAME);
	}

	public SubtourModeChoiceConfigGroup subtourModeChoice() {
		return (SubtourModeChoiceConfigGroup) this.getModule(SubtourModeChoiceConfigGroup.GROUP_NAME);
	}

	public ChangeModeConfigGroup changeMode() {
		return (ChangeModeConfigGroup) this.getModule(ChangeModeConfigGroup.CONFIG_MODULE);
	}

	public HermesConfigGroup hermes() {
		return (HermesConfigGroup) this.getModule(HermesConfigGroup.NAME);
	}

	public ReplanningAnnealerConfigGroup replanningAnnealer() {
		return (ReplanningAnnealerConfigGroup) this.getModule(ReplanningAnnealerConfigGroup.GROUP_NAME);
	}

	public PlanInheritanceConfigGroup planInheritance() {
		return (PlanInheritanceConfigGroup) this.getModule(PlanInheritanceConfigGroup.GROUP_NAME);
	}

	// other:

	public void addConfigConsistencyChecker(final ConfigConsistencyChecker checker) {
		boolean alreadyExists = false;
		for (ConfigConsistencyChecker ch : consistencyCheckers) {
			if (ch.getClass().equals(checker.getClass())) {
				alreadyExists = true;
			}
		}
		if ( !alreadyExists ) {
			this.consistencyCheckers.add(checker);
		} else {
			log.info( "ConfigConsistencyChecker with runtime type=" + checker.getClass() + " was already added; not adding it a second time" ) ;
		}
	}

	public void removeConfigConsistencyChecker( final Class clazz ) {
		// I am not saying that I like this.  But I would like to be able to check config consistency before the iterator is created, but by then we still have
		// unmaterialized config groups, and so I need to remove that checker at that point.  Maybe we can sort this in some different way ...
		consistencyCheckers.removeIf( ch -> ch.getClass().equals( clazz ) );
	}

	public final boolean isLocked() {
		return this.locked;
	}

	public final void setLocked(boolean locked) {
		this.locked = locked;
	}

	private void checkIfLocked() {
		if ( this.isLocked() ) {
			log.error("too late in execution sequence to set config items. Use");
			log.error("   Config config = ConfigUtils.loadConfig(filename); ");
			log.error("   config.xxx().setYyy(...); ");
			log.error("   Controler ctrl = new Controler( config );");
			log.error("or") ;
			log.error("   Config config = ConfigUtils.loadConfig(filename); ");
			log.error("   config.xxx().setYyy(...); ");
			log.error("   Scenario scenario = ScenarioUtils.loadScenario(config);") ;
			log.error("   // do something with scenario") ;
			log.error("   Controler ctrl = new Controler( scenario );");
			log.error("This will be changed to an abortive error in the future."); // kai, feb'13
		}
	}

	public final VehiclesConfigGroup vehicles() {
		return (VehiclesConfigGroup) this.getModule(VehiclesConfigGroup.GROUP_NAME);
	}

	public void setContext(URL context) {
		if ( this.context==null  ||  !(context.toString().equals( this.context.toString() ) ) ) {
			log.info("setting context to [" + context + "]");
			// ConfigUtils.createConfig() is used at several places, e.g. when generating an empty
			// scenario to obtain the default factories.  This will evidently produce output here,
			// and in some sense the wrong output, since the relevant context is probably set from
			// some config file path and in fact _not_ changed since this here will be a different
			// ``throwaway'' config instance.  :-(  kai, jun'18
		}
		this.context = context;
	}

	public URL getContext() {
		return context;
	}
}
