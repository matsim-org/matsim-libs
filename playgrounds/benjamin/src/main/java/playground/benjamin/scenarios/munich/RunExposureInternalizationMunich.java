/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich1pct.java
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
package playground.benjamin.scenarios.munich;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityCostModule;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory;
import playground.benjamin.scenarios.munich.exposure.GridTools;
import playground.benjamin.scenarios.munich.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.benjamin.scenarios.munich.exposure.ResponsibilityGridTools;

/**
 * @author benjamin
 *
 */
public class RunExposureInternalizationMunich {

	static String configFile;
	static String emissionCostFactor;
	static String emissionEfficiencyFactor;
	static String considerCO2Costs;
	private static ResponsibilityGridTools rgt;
	private static Map<Id<Link>, Integer> links2xCells;
	private static Map<Id<Link>, Integer> links2yCells;
	
	private static Integer noOfXCells = 160;
	private static Integer noOfYCells = 120;
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;
	private static Double timeBinSize = 3600.;
	private static int noOfTimeBins = 30;


	public static void main(String[] args) {
		configFile = args[0];
		emissionCostFactor = args[1];
		emissionEfficiencyFactor = args[2];
		considerCO2Costs = args[3];

//		configFile = "../../detailedEval/internalization/test/testConfig.xml";
//		emissionCostFactor = "1.0";
//		emissionEfficiencyFactor = "1.0";
//		considerCO2Costs = "true";
		
		Config config = ConfigUtils.createConfig();
		config.addCoreModules(); // TODO remove?
		ConfigReader confReader = new ConfigReader(config);
		confReader.readFile(configFile);

		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();
		scenario = ScenarioUtils.loadScenario(config); // TODO remove?

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		links2xCells = gt.mapLinks2Xcells(noOfXCells);
		links2yCells = gt.mapLinks2Ycells(noOfYCells);
		
		rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(Double.parseDouble(emissionCostFactor),	
				Boolean.parseBoolean(considerCO2Costs), rgt, links2xCells, links2yCells);
		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, 
				emissionCostModule, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});
		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
		
	}
}