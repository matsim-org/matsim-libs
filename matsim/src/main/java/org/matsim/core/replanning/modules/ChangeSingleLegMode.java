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
import org.matsim.population.algorithms.ChooseRandomSingleLegMode;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Changes the transportation mode of each legs in a plan to a randomly chosen
 * mode, given a list of possible modes. Each leg can have another mode assigned,
 * and it may be possible that the mode is not changed at all (i.e., the same mode
 * was randomly chosen again).
 *
 * <b>Warning:</b> Using this class in a replanning strategy may result in many more
 * iterations being needed until a useful state can be reached.
 *
 * By default, the module chooses between "car" and "pt". If other modes should
 * be used, it can be done so in the configuration:
 * <pre>
 * &lt;module name="changeLegMode">
 *   &lt!-- provide a comma-separated list of leg modes -->
 *   &lt;param name="modes" value="car,walk,bike" />
 * &lt;/module>
 * </pre>
 *
 * @see ChooseRandomSingleLegMode
 *
 * @author mrieser
 */
public class ChangeSingleLegMode extends AbstractMultithreadedModule {

	private final static Logger log = Logger.getLogger(ChangeSingleLegMode.class);

	/*package*/ final static String CONFIG_MODULE = "changeLegMode";
	/*package*/ final static String CONFIG_PARAM_MODES = "modes";
	/*package*/ final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public ChangeSingleLegMode(final Config config) {
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

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomSingleLegMode algo = new ChooseRandomSingleLegMode(this.availableModes, MatsimRandom.getLocalInstance());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
