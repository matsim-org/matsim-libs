/* *********************************************************************** *
 * project: org.matsim.*
 * InternalControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.version4;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * @author Ihab
 *
 */
public class InternalControler {

	private String configFile;
	private String directoryExtIt;
	private String outputExternalIterationDirPath;
	private int lastInternalIteration;
	private int extItNr;
	private String populationFile;
	
	public InternalControler(String configFile, int extItNr, String directoryExtIt, int lastInternalIteration, String populationFile, String outputExternalIterationDirPath, int numberOfBuses) {
		this.configFile = configFile;
		this.directoryExtIt = directoryExtIt;
		this.lastInternalIteration = lastInternalIteration;
		this.extItNr = extItNr;
		this.populationFile = populationFile;
		this.outputExternalIterationDirPath = outputExternalIterationDirPath;		
	}
	
	public void run() {
		String population = null;
		if (this.extItNr==0){
			population = populationFile;
		}
		else {
			population = this.outputExternalIterationDirPath+"/extITERS/extIt."+(this.extItNr-1)+"/internalIterations/output_plans.xml.gz";
		}
		
		Config config = new Config();
		config.addCoreModules();
		
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);
		
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new MyControlerListener());
		
		TransitConfigGroup transit = controler.getConfig().transit();
		transit.setTransitScheduleFile(this.directoryExtIt+"/scheduleFile.xml");
		transit.setVehiclesFile(this.directoryExtIt+"/vehiclesFile.xml");
		
		PlansConfigGroup plans = controler.getConfig().plans();
		plans.setInputFile(population);
		
		ControlerConfigGroup controlerConfGroup = controler.getConfig().controler();
		controlerConfGroup.setFirstIteration(0);
		controlerConfGroup.setLastIteration(this.lastInternalIteration);
		controlerConfGroup.setWriteEventsInterval(this.lastInternalIteration);
		controlerConfGroup.setWritePlansInterval(this.lastInternalIteration);
		controlerConfGroup.setOutputDirectory(this.directoryExtIt+"/internalIterations");
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();	
		planCalcScoreConfigGroup.setTravelingPt_utils_hr(-6);
		planCalcScoreConfigGroup.setMarginalUtilityOfMoney(1);
		planCalcScoreConfigGroup.setTraveling_utils_hr(-6);
		planCalcScoreConfigGroup.setTravelingWalk_utils_hr(-2);
		planCalcScoreConfigGroup.setConstantCar(-4.115);
		
		MyScoringFunctionFactory scoringfactory = new MyScoringFunctionFactory(planCalcScoreConfigGroup);
		controler.setScoringFunctionFactory(scoringfactory);
		controler.run();		
	}

}
