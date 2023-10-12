/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.OutputDirectoryHierarchy;


/**
 * This is currently only a substitute to the full Controler.
 *
 * This class is deprecated. Use {@link org.matsim.core.controler.Controler} instead
 *
 * @author mrieser
 */
public class RunMatsim {

	private final org.matsim.core.controler.Controler controler;

	public RunMatsim(final String[] args) {
		this.controler = new org.matsim.core.controler.Controler(args);
	}

	public RunMatsim(final String configFilename) {
		this.controler = new org.matsim.core.controler.Controler(configFilename);
	}

	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.controler.getConfig().controller().setOverwriteFileSetting(
				overwriteFiles ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
	}

	public Scenario getScenario() {
		return this.controler.getScenario() ;
	}

	public void run() {
		this.controler.run();
	}

	public  static void main(String[] args) {
		new RunMatsim(args).run();
	}
}
