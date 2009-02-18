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

package org.matsim.replanning.modules;

import org.matsim.config.Config;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.population.algorithms.ChooseRandomLegMode;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.StringUtils;

/**
 * Changes the transportation mode of all legs in a plan to a randomly chosen
 * different mode (but the same mode for all legs in that plan) given a list
 * of possible modes.
 *
 * By default, the module chooses between "car" and "pt". If other modes should
 * be used, it can be done so in the configuration:
 * <pre>
 * &lt;module name="changeLegMode" />
 *   &lt!-- provide a comma-separated list of leg modes -->
 *   &lt;param name="modes" key="car,walk,bike" />
 * &lt;/module>
 * </pre>
 *
 * @see ChooseRandomLegMode
 *
 * @author mrieser
 */
public class ChangeLegMode extends MultithreadedModuleA {

	/*package*/ final static String CONFIG_MODULE = "changeLegMode";
	/*package*/ final static String CONFIG_PARAM_MODES = "modes";

	private BasicLeg.Mode[] availableModes = new BasicLeg.Mode[] { BasicLeg.Mode.car, BasicLeg.Mode.pt };

	public ChangeLegMode() {
	}

	public ChangeLegMode(final BasicLeg.Mode[] availableModes) {
		this.availableModes = availableModes.clone();
	}

	public ChangeLegMode(final Config config) {
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = new BasicLeg.Mode[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes[i] = BasicLeg.Mode.valueOf(parts[i].trim());
			}
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ChooseRandomLegMode(this.availableModes, MatsimRandom.getLocalInstance());
	}

}
