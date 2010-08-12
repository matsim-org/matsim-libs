/* *********************************************************************** *
 * project: org.matsim.*
 * KTIPhDControler.java
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

package playground.meisterk.phd.controler;

import org.matsim.core.controler.Controler;

import playground.meisterk.kti.controler.KTIControler;
import playground.meisterk.phd.config.PopulationConvergenceConfigGroup;

public class KTIPhDControler extends KTIControler {

	private final PopulationConvergenceConfigGroup populationConvergenceConfigGroup = new PopulationConvergenceConfigGroup();

	public KTIPhDControler(String[] args) {
		super(args);
		super.config.addModule(PopulationConvergenceConfigGroup.GROUP_NAME, populationConvergenceConfigGroup);
	}

	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		this.addControlerListener(new PersonTreatmentRecorder(this.populationConvergenceConfigGroup));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KTIPhDControler(args);
			controler.run();
		}
		System.exit(0);
	}

}
