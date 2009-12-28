/* *********************************************************************** *
 * project: org.matsim.*
 * EctmControler.java
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

package playground.mrieser.ectm;

import org.matsim.core.controler.Controler;

public class EctmControler extends Controler {

	public EctmControler(final String[] args) {
		super(args);
	}

	@Override
	protected void runMobSim() {
		new EctmSim(this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Controler controler = new EctmControler(args);
//		controler.setOverwriteFiles(true);
		controler.run();
	}

}
