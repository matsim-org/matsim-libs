/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;

/**
 * @author thibautd
 */
public class JointTripsSelectorModule extends AbstractMultithreadedModule {
	private final AbstractMultithreadedModule optDelegate;
	private final Controler controler;

	public JointTripsSelectorModule(final Controler controler) {
		super(controler.getConfig().global());
		optDelegate = new JointTimeModeChooserModule( controler );
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm subroutine = optDelegate.getPlanAlgoInstance();

		return new JointTripsSelector(
				subroutine,
				controler.getScoringFunctionFactory());
	}
}

