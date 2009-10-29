/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalysisTest
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
package playground.dgrether.analysis;

import java.io.File;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestCase;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.io.DgAnalysisPopulationReader;


/**
 * @author dgrether
 *
 */
public class DgAnalysisTest extends MatsimTestCase {

	
	public void testCRSCreation(){
			CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
			assertNotNull(targetCRS);
	}
	
	
	public void estReadPopulation(){
		String netFilename = "test/scenarios/equil/network.xml";
		String plansFilename = "test/scenarios/equil/plans100.xml";
		String runId = "testRun23";
		ScenarioImpl sc = new ScenarioImpl();
		Config config = sc.getConfig();
		config.network().setInputFile(netFilename);
		config.plans().setInputFile(plansFilename);
		config.controler().setOutputDirectory(this.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setRunId(runId);
		
		Controler controler = new Controler(sc);
		controler.setCreateGraphs(false);
		controler.run();
		
		String outputNetworkFilename = this.getOutputDirectory() + "testRun23.output_network.xml.gz";
		String outputPlansFilename = this.getOutputDirectory() + "testRun23.output_plans.xml.gz";
		
		assertTrue("no output network with default name written!", new File(outputNetworkFilename).exists());
		assertTrue("no output population with default name written!", new File(outputPlansFilename).exists());
		
		
		DgAnalysisPopulationReader reader = new DgAnalysisPopulationReader(sc);
//		reader.readPopulationFile();
	}
}
