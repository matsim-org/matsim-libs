/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.usecases.opdytsintegration.modechoice;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestUtils;

import playground.kairuns.run.KNBerlinControler;

/**
 * @author nagel
 *
 */
public class KNModeChoiceCalibTestIT {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

//	@Test public final void testZero() {
//		boolean equil = true ;
//		boolean calib = false ;
//		boolean assignment = false ;
//		
//		Gbl.assertIf(equil); // test case not prepared for other setting
//		Gbl.assertIf(!calib); // test case not prepared for other setting
//		Gbl.assertIf(!assignment); // test case not prepared for other setting
//
//		final Config config = KNBerlinControler.prepareConfig(null, assignment, equil) ;
//		
//		String outputDirectory = utils.getOutputDirectory() ;
//		config.controler().setOutputDirectory(outputDirectory);
//
//		config.plans().setInputFile("relaxed_plans.xml.gz");
//
//		KNModeChoiceCalibMain.run(config, null, equil, calib, assignment, outputDirectory) ;
//
//	}
	@Test public final void testOne() {
		boolean equil = true ;
		boolean calib = true ;
		boolean assignment = false ;
		boolean modeChoice = false ;
		
		Gbl.assertIf(equil); // test case not prepared for other setting
		Gbl.assertIf(calib); // test case not prepared for other setting
		Gbl.assertIf(!assignment); // test case not prepared for other setting
		Gbl.assertIf(!modeChoice); // test case not prepared for other setting
		
		String[] args = new String[]{ utils.getPackageInputDirectory() + "/config.xml"  } ;

		final Config config = KNBerlinControler.prepareConfig(args, assignment, equil, modeChoice) ;
		
		config.plans().setInputFile("relaxed_plans.xml.gz");
		config.network().setInputFile("network.xml.gz");
		
		String outputDirectory = utils.getOutputDirectory() ;
		config.controler().setOutputDirectory(outputDirectory);
		
		config.controler().setLastIteration(1);

		KNModeChoiceCalibMain.run(config, equil, calib, assignment, outputDirectory, true) ;

	}

}
