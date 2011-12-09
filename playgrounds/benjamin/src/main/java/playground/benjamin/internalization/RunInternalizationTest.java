/* *********************************************************************** *
 * project: org.matsim.*
 * RunInternalizationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.internalization;

import org.matsim.core.config.Config;

/**
 * @author benjamin
 *
 */
public class RunInternalizationTest {

	private final String outputDirectory = "../../detailedEval/internalization/test/";

	private Config config;

	private void run() {
		
		setUpTest();
		
	}

	private void setUpTest() {
		
		this.config = new Config();
		this.config.addCoreModules();
		this.config.controler().setOutputDirectory(this.outputDirectory);
		
	}

	public static void main(String[] args) {
		RunInternalizationTest test = new RunInternalizationTest();
		test.run();
	}

}
