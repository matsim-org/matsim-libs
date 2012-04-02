/* *********************************************************************** *
 * project: org.matsim.*
 * LJParameterHunter.java
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

package playground.yu.parameterSearch.LJ;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;

public class LJParameterHunter {
	private final Controler ctl;
	private final SimCntLogLikelihoodCtlListener llhListener;

	public LJParameterHunter(Config cfg) {
		ctl = new Controler(cfg);
		llhListener = new SimCntLogLikelihoodCtlListener();
		ctl.addControlerListener(llhListener);
		// TODO add ... listener
		ctl.setOverwriteFiles(true);
		ctl.run();
	}

	// public double getLogLikelihood() {
	// // TODO getter in SimCntLogLikelihoodCtlListener TODO a class or sth.
	// // else? or in another class?
	// return 0;
	// }

	// public Collection<Double> getParameters(Collection<String> names) {
	// // TODO a class or sth. else? or in another class?
	// return null;
	// }

	// TODO setParameters? TODO a class or sth. else? or in another class?

	public static void main(String[] args) {
		new LJParameterHunter(
				ConfigUtils.loadConfig(args[0]/* configfilename */));
	}
}
