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

package playground.jbischoff.csberlin.replanning;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomSingleLegMode;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

/**
 * 
 * @see ChooseRandomSingleLegMode
 *
 * @author mrieser, jbischoff
 */
public class ChangeSingleLegModeWithPermissibleModes extends AbstractMultithreadedModule {

	private PermissibleModesCalculator calculator;

	public ChangeSingleLegModeWithPermissibleModes(final int nOfThreads, PermissibleModesCalculator calculator) {
		super(nOfThreads);
		this.calculator = calculator;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomSingleLegModeWithPermissibleModes algo = new ChooseRandomSingleLegModeWithPermissibleModes( calculator, MatsimRandom.getLocalInstance());
		return algo;
	}

}
