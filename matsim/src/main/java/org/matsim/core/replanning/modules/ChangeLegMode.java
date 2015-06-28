/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegMode.java
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

package org.matsim.core.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.population.algorithms.ChooseRandomLegMode;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Changes the transportation mode of all legs in a plan to a randomly chosen
 * different mode (but the same mode for all legs in that plan) given a list
 * of possible modes.
 *
 * By default, the module chooses between "car" and "pt". If other modes should
 * be used, it can be done so in the configuration. Also, this module is able to (optionally)
 * respect car-availability:
 * <pre>
 * &lt;module name="changeLegMode">
 *   &lt!-- provide a comma-separated list of leg modes -->
 *   &lt;param name="modes" value="car,walk,bike" />
 *   &lt;param name="ignoreCarAvailability" value="false" />
 * &lt;/module>
 * </pre>
 *
 * @see ChooseRandomLegMode
 *
 * @author mrieser
 */
public class ChangeLegMode extends AbstractMultithreadedModule {

	private final static Logger log = Logger.getLogger(ChangeLegMode.class);

	public final static String CONFIG_MODULE = "changeLegMode";
	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";
	// (I made the above static final variables public so they can be used in scripts-in-java. kai, jun'15)

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public ChangeLegMode(final Config config) {
		super(config.global().getNumberOfThreads());

		// try to get the modes from the "changeLegMode" module of the config file
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		String ignorance = config.findParam(CONFIG_MODULE, CONFIG_PARAM_IGNORECARAVAILABILITY);

		// if there was anything in there, replace the default availableModes by the entries in the config file:
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = new String[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes[i] = parts[i].trim().intern();
			}
		}

		if (ignorance != null) {
			this.ignoreCarAvailability = Boolean.parseBoolean(ignorance);
			log.info("using ignoreCarAvailability from configuration: " + this.ignoreCarAvailability);
		}

	}
	
	public ChangeLegMode(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomLegMode algo = new ChooseRandomLegMode(this.availableModes, MatsimRandom.getLocalInstance());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
