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
package playground.julia.newInternalization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.vsp.emissions.EmissionModule;

/**
 * @author benjamin
 *
 */
public class RunNewInternalizationMunich {

	static String configFile;
	static String emissionCostFactor;
	static String emissionEfficiencyFactor;
	static String considerCO2Costs;
	private static ResponsibilityGridTools rgt;


	public static void main(String[] args) {
		//		configFile = "../../detailedEval/internalization/munich1pct/input/config_munich_1pct.xml";
		//		emissionCostFactor = "1.0";
		//		emissionEfficiencyFactor = "1.0";
		//		considerCO2Costs = "true";

		configFile = args[0];
		emissionCostFactor = args[1];
		emissionEfficiencyFactor = args[2];
		considerCO2Costs = args[3];

		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);

		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs), rgt );

//		EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
//		controler.setTravelDisutilityFactory(emissionTducf);
		EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		controler.setTravelDisutilityFactory(emfac);

//		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt));

		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.run();
	}
}