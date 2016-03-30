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

package playground.singapore.springcalibration.run.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/* 
 * @author anhorni
 */
public class SingaporeChangeSingleLegMode extends AbstractMultithreadedModule {

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public SingaporeChangeSingleLegMode(final GlobalConfigGroup globalConfigGroup, ChangeLegModeConfigGroup changeLegModeConfigGroup) {
		super(globalConfigGroup.getNumberOfThreads());
		this.availableModes = changeLegModeConfigGroup.getModes();
		this.ignoreCarAvailability = changeLegModeConfigGroup.getIgnoreCarAvailability();
	}
	
	public SingaporeChangeSingleLegMode(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		SingaporeChooseRandomSingleLegMode algo = new SingaporeChooseRandomSingleLegMode(this.availableModes, MatsimRandom.getLocalInstance());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
