/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserModule.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class TimeModeChooserModule extends AbstractMultithreadedModule {
	private final Controler controler;
	private final DepartureDelayAverageCalculator delay;

	public TimeModeChooserModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
        delay = new DepartureDelayAverageCalculator(
                controler.getScenario().getNetwork(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new TimeModeChooserAlgorithm( controler , delay );
	}
}

