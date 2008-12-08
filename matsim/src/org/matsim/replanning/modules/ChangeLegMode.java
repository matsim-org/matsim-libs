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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Config;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.algorithms.ChooseRandomLegMode;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.StringUtils;

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
