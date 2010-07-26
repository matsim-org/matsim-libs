/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mfeil.FilesForTests;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class Initializer {

	private Controler controler;

	public Initializer() {
	}

	public void init(MatsimTestCase testCase) {
		System.out.println(testCase.getPackageInputDirectory());
		String path = testCase.getPackageInputDirectory() + "config.xml";
		Config config = testCase.loadConfig(path);
		this.controler = new ControlerForTests(config);
		this.controler.setOverwriteFiles(true);
	}

	public void run (){
		this.controler.run();
	}

	public Controler getControler() {
		return controler;
	}
	public void setControler(Controler controler) {
		this.controler = controler;
	}

}
