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

package org.matsim.config;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.config.groups.ConfigConfigGroup;
import org.matsim.config.groups.ControlerConfigGroup;
import org.matsim.config.groups.CountsConfigGroup;
import org.matsim.config.groups.EvacuationConfigGroup;
import org.matsim.config.groups.EventsConfigGroup;
import org.matsim.config.groups.FacilitiesConfigGroup;
import org.matsim.config.groups.GlobalConfigGroup;
import org.matsim.config.groups.MatricesConfigGroup;
import org.matsim.config.groups.NetworkConfigGroup;
import org.matsim.config.groups.PlansConfigGroup;
import org.matsim.config.groups.RoadPricingConfigGroup;
import org.matsim.config.groups.SimulationConfigGroup;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.config.groups.WithindayConfigGroup;
import org.matsim.config.groups.WorldConfigGroup;

public class Config {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final TreeMap<String, Module> modules = new TreeMap<String,Module>();

	private GlobalConfigGroup global = null;
	private ConfigConfigGroup config = null;
	private ControlerConfigGroup controler = null;
	private SimulationConfigGroup simulation = null;
	private WithindayConfigGroup withinDay = null;
	private CountsConfigGroup counts = null;
	private CharyparNagelScoringConfigGroup charyparNagelScoring = null;
	private WorldConfigGroup world = null;
	private NetworkConfigGroup network = null;
	private PlansConfigGroup plans = null;
	private FacilitiesConfigGroup facilities = null;
	private MatricesConfigGroup matrices = null;
	private EventsConfigGroup events = null;
	private RoadPricingConfigGroup roadpricing = null;
	private EvacuationConfigGroup evacuation = null;
	private StrategyConfigGroup strategy = null;
	private SocNetConfigGroup socnetmodule = null;

	private static final Logger log = Logger.getLogger(Config.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Config() {
		// nothing to do
	}

	public void addCoreModules() {
		this.global = new GlobalConfigGroup();
		this.modules.put(GlobalConfigGroup.GROUP_NAME, this.global);

		this.config  = new ConfigConfigGroup();
		this.modules.put(ConfigConfigGroup.GROUP_NAME, this.config);

		this.controler = new ControlerConfigGroup();
		this.modules.put(ControlerConfigGroup.GROUP_NAME, this.controler);

		this.simulation = new SimulationConfigGroup();
		this.modules.put(SimulationConfigGroup.GROUP_NAME, this.simulation);

		this.withinDay = new WithindayConfigGroup(WithindayConfigGroup.GROUP_NAME);
		this.modules.put(WithindayConfigGroup.GROUP_NAME, this.withinDay);

		this.counts = new CountsConfigGroup();
		this.modules.put(CountsConfigGroup.GROUP_NAME, this.counts);

		this.charyparNagelScoring = new CharyparNagelScoringConfigGroup();
		this.modules.put(CharyparNagelScoringConfigGroup.GROUP_NAME, this.charyparNagelScoring);

		this.world = new WorldConfigGroup();
		this.modules.put(WorldConfigGroup.GROUP_NAME, this.world);

		this.network = new NetworkConfigGroup();
		this.modules.put(NetworkConfigGroup.GROUP_NAME, this.network);

		this.plans = new PlansConfigGroup();
		this.modules.put(PlansConfigGroup.GROUP_NAME, this.plans);

		this.facilities = new FacilitiesConfigGroup();
		this.modules.put(FacilitiesConfigGroup.GROUP_NAME, this.facilities);

		this.strategy = new StrategyConfigGroup();
		this.modules.put(StrategyConfigGroup.GROUP_NAME, this.strategy);

		this.matrices = new MatricesConfigGroup();
		this.modules.put(MatricesConfigGroup.GROUP_NAME, this.matrices);

		this.events = new EventsConfigGroup();
		this.modules.put(EventsConfigGroup.GROUP_NAME, this.events);

		this.roadpricing = new RoadPricingConfigGroup();
		this.modules.put(RoadPricingConfigGroup.GROUP_NAME, this.roadpricing);

		this.evacuation = new EvacuationConfigGroup();
		this.modules.put(EvacuationConfigGroup.GROUP_NAME, this.evacuation);

//		 SN TEST (not a default module)
		this.socnetmodule = new SocNetConfigGroup();
		this.modules.put(SocNetConfigGroup.GROUP_NAME, this.socnetmodule);
	}

	/** Checks each module for consistency, e.g. if the parameters that are currently set make sense
	 * in their combination. */
	public void checkConsistency() {
		for (Module m : this.modules.values()) {
			m.checkConsistency();
		}
	}

	//////////////////////////////////////////////////////////////////////
	// add / set methods
	//////////////////////////////////////////////////////////////////////

	public final Module createModule(final String modulename) {
		if (this.modules.containsKey(modulename)) {
			throw new IllegalArgumentException("Module " + modulename + " exists already.");
		}
		Module m = new Module(modulename);
		this.modules.put(modulename, m);
		return m;
	}

	public final void addModule(final String modulename, final Module module) {
		if (this.modules.containsKey(modulename)) {
			throw new IllegalArgumentException("Module " + modulename + " exists already.");
		}
		this.modules.put(modulename, module);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	protected final TreeMap<String, Module> getModules() {
		return this.modules;
	}

	/**
	 * Returns the requested module, or <code>null</code> if the module does not exist.
	 *
	 * @param moduleName name of the requested module
	 * @return requested module
	 */
	public final Module getModule(final String moduleName) {
		return this.modules.get(moduleName);
	}

	/** Returns the requested parameter. If the module or parameter is not known, an
	 * error is logged and an IllegalArgumentException is thrown.
	 *
	 * @param moduleName
	 * @param paramName
	 * @return the requested parameter
	 *
	 * @throws IllegalArgumentException if the module or parameter does not exist
	 * @see #findParam(String, String)
	 */
	public final String getParam(final String moduleName, final String paramName) {
		Module m = this.modules.get(moduleName);
		if (m == null) {
			log.error("Module \"" + moduleName + "\" is not known.");
			throw new IllegalArgumentException("Module \"" + moduleName + "\" is not known.");
		}
		String str = m.getValue(paramName);
		if (str == null) {
			log.error("Parameter \"" + paramName + "\" of module \"" + moduleName + "\" is not known");
			throw new IllegalArgumentException("Module \"" + moduleName + "\" is not known.");
		}
		return str;
	}

	/**
	 * Returns the value of the specified parameter if it exists, or <code>null</code> otherwise.
	 *
	 * @param moduleName name of the config-module
	 * @param paramName name of parameter in the specified module
	 * @return value of the parameter if it exists, <code>null</code> otherwise
	 *
	 * @see #getParam(String, String)
	 */
	public final String findParam(final String moduleName, final String paramName) {
		Module m = this.modules.get(moduleName);
		if (m == null) {
			return null;
		}
		try {
			String str = m.getValue(paramName);
			if (str == null) {
				return null;
			}
			return str;
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_modules=" + this.modules.size() + "]";
	}

	//////////////////////////////////////////////////////////////////////
	// is used for using Config without an config-file given
	//////////////////////////////////////////////////////////////////////
	public final void setParam(final String module_name, final String param_name, final String value) {
		Module m = this.modules.get(module_name);
		if (m == null) {
			m = createModule(module_name);
			log.info("module \"" + module_name + "\" added.");
		}
		if (m != null) {
			m.addParam(param_name, value);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// direct access to modules / groups
	//////////////////////////////////////////////////////////////////////

	public final GlobalConfigGroup global() {
		return this.global;
	}

	public final ConfigConfigGroup config() {
		return this.config;
	}

	public final ControlerConfigGroup controler() {
		return this.controler;
	}

	public final SimulationConfigGroup simulation() {
		return this.simulation;
	}

	public final CountsConfigGroup counts() {
		return this.counts;
	}

	public final CharyparNagelScoringConfigGroup charyparNagelScoring() {
		return this.charyparNagelScoring;
	}

	public final WorldConfigGroup world() {
		return this.world;
	}

	public final WithindayConfigGroup withinday() {
		return this.withinDay;
	}

	public final NetworkConfigGroup network() {
		return this.network;
	}

	public final PlansConfigGroup plans() {
		return this.plans;
	}

	public final FacilitiesConfigGroup facilities() {
		return this.facilities;
	}

	public final MatricesConfigGroup matrices() {
		return this.matrices;
	}

	public final EventsConfigGroup events() {
		return this.events;
	}

	public final RoadPricingConfigGroup roadpricing() {
		return this.roadpricing;
	}

	public final EvacuationConfigGroup evacuation(){
		return this.evacuation;
	}

	public final StrategyConfigGroup strategy() {
		return this.strategy;
	}

	public final SocNetConfigGroup socnetmodule() {
		return this.socnetmodule;
	}

}
