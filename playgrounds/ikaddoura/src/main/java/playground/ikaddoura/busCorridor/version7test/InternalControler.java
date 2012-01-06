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
package playground.ikaddoura.busCorridor.version7test;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
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
	private String networkFile;
	private double fare;
	
//	private final double TRAVEL_PT_AUD_PER_HOUR = 18.19;
//	private final double TRAVEL_CAR_AUD_PER_HOUR = 18.68;
////	private final double TRAVEL_WALK_UTILS_PER_HOUR = 19.91;
//	private final double MONEY_UTILS_PER_EURO = 1;
////	private final double TRAVEL_PT_WAITING_AUD_PER_HOUR = -30.53;
//	private final double CONSTANT_CAR_AUD = 0;
//	private final double CONSTANT_PT_AUD = -12.20;
	
	private final double TRAVEL_PT = -3.2982;
	private final double TRAVEL_CAR = -1.8534;
	private final double TRAVEL_WALK = -3.6102;
	private final double MONEY_UTILS = 0.1813; // has to be positive!
	private final double CONSTANT_CAR = 0;
	private final double CONSTANT_PT = 2.2118;

	
	public InternalControler(String configFile, int extItNr, String directoryExtIt, int lastInternalIteration, String populationFile, String outputExternalIterationDirPath, int numberOfBuses, String networkFile, double fare) {
		this.configFile = configFile;
		this.directoryExtIt = directoryExtIt;
		this.lastInternalIteration = lastInternalIteration;
		this.extItNr = extItNr;
		this.populationFile = populationFile;
		this.outputExternalIterationDirPath = outputExternalIterationDirPath;
		this.networkFile = networkFile;
		this.fare = fare;
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
		controler.addControlerListener(new MyControlerListener(fare));
		
		TransitConfigGroup transit = controler.getConfig().transit();
		transit.setTransitScheduleFile(this.directoryExtIt+"/scheduleFile.xml");
		transit.setVehiclesFile(this.directoryExtIt+"/vehiclesFile.xml");
		
		PlansConfigGroup plans = controler.getConfig().plans();
		plans.setInputFile(population);
		
		NetworkConfigGroup network = controler.getConfig().network();
		network.setInputFile(networkFile);
		
		ControlerConfigGroup controlerConfGroup = controler.getConfig().controler();
		controlerConfGroup.setFirstIteration(0);
		controlerConfGroup.setLastIteration(this.lastInternalIteration);
		controlerConfGroup.setWriteEventsInterval(1);
		controlerConfGroup.setWritePlansInterval(1);
		controlerConfGroup.setOutputDirectory(this.directoryExtIt+"/internalIterations");
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();	
		planCalcScoreConfigGroup.setTravelingPt_utils_hr(TRAVEL_PT);
		planCalcScoreConfigGroup.setMarginalUtilityOfMoney(MONEY_UTILS);
		planCalcScoreConfigGroup.setTraveling_utils_hr(TRAVEL_CAR);
		planCalcScoreConfigGroup.setTravelingWalk_utils_hr(TRAVEL_WALK);
		planCalcScoreConfigGroup.setConstantCar(CONSTANT_CAR);
		planCalcScoreConfigGroup.setConstantPt(CONSTANT_PT);
		
		MyScoringFunctionFactory scoringfactory = new MyScoringFunctionFactory(planCalcScoreConfigGroup);
		controler.setScoringFunctionFactory(scoringfactory);
		controler.run();		
	}

}
