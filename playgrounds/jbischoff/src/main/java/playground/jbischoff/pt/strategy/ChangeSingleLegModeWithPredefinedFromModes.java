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

package playground.jbischoff.pt.strategy;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomSingleLegMode;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

/**
 * Changes the transportation mode of one leg of a predefined mode in a plan to a randomly chosen
 * mode, given a list of possible modes. Insures that the newly chosen mode
 * is different from the existing mode (if possible)
 *
 * By default, the module chooses between "car" and "pt". If other modes should
 * be used, it can be done so in the configuration. Also, this module is able to (optionally)
 * respect car-availability:
 * <pre>
 * &lt;module name="changeMode">
 *   &lt!-- provide a comma-separated list of leg modes -->
 *   &lt;param name="modes" value="car,walk,bike" />
 *   &lt;param name="ignoreCarAvailability" value="false" />
 * &lt;/module>
 * </pre>
 *
 * @see ChooseRandomSingleLegMode
 *
 * @author jbischoff after mrieser
 */
public class ChangeSingleLegModeWithPredefinedFromModes extends AbstractMultithreadedModule {

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public ChangeSingleLegModeWithPredefinedFromModes(final GlobalConfigGroup globalConfigGroup, ChangeModeConfigGroup changeLegModeConfigGroup) {
		super(globalConfigGroup.getNumberOfThreads());
		this.availableModes = changeLegModeConfigGroup.getModes();
		this.ignoreCarAvailability = changeLegModeConfigGroup.getIgnoreCarAvailability();
	}
	
	public ChangeSingleLegModeWithPredefinedFromModes(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomSingleLegModeWithPredefinedFromModes algo = new ChooseRandomSingleLegModeWithPredefinedFromModes(this.availableModes, this.availableModes, MatsimRandom.getLocalInstance());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
