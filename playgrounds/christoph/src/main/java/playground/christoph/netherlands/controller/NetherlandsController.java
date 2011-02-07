/* *********************************************************************** *
 * project: org.matsim.*
 * NetherlandsController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.netherlands.controller;

import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class NetherlandsController extends Controler {

	public NetherlandsController(final String[] args) {
		super(args);
	}
	
	/*
	 * We use a Scoring Function that get the Facility Opening Times from
	 * the Facilities instead of the Config File.
	 */
	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelOpenTimesScoringFunctionFactory(this.config.planCalcScore(), this.getFacilities());
	}

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: NetherlandsController config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new NetherlandsController(args);
			controler.run();
		}
		System.exit(0);
	}
}