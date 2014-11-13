/* *********************************************************************** *
 * project: org.matsim.*
 * MZComparisonListener.java
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
package playground.yu.analysis.MZComparison;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author yu
 * 
 */
public class MZComparisonListener implements IterationEndsListener,
		StartupListener {
	private final MZComparisonDataIO mzcdi = new MZComparisonDataIO();

	@Override
	public void notifyStartup(StartupEvent event) {
		mzcdi.readMZData(event.getControler().getConfig().vspExperimental()
				.getInputMZ05File());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iter = event.getIteration();
		if (iter % 100 == 0) {

			Controler ctl = event.getControler();
			OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
            Population pop = ctl.getScenario().getPopulation();

			MZComparisonData mzcd = new MZComparisonData((RoadPricingScheme) ctl.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME));
			mzcd.run(pop);
			mzcdi.setData2Compare(mzcd);
			mzcdi.write(ctlIO.getIterationFilename(iter, "MZ05Comparison"));

			// GeometricDistanceExtractor lde = new
			// GeometricDistanceExtractor(ctl
			// .getRoadPricing().getRoadPricingScheme(), ctlIO
			// .getIterationFilename(iter, "geoDistKanton"));
			// lde.run(pop);
			// lde.write();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controler controler = new Controler(args);
		controler.addControlerListener(new MZComparisonListener());
		controler.run();
	}
}
