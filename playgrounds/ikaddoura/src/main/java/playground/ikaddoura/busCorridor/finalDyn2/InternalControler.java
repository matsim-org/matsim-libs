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
package playground.ikaddoura.busCorridor.finalDyn2;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * @author Ihab
 *
 */
public class InternalControler {

	private PtLegHandler ptLegHandler;
	
	private String configFile;
	private String directoryExtIt;
	private String outputExternalIterationDirPath;
	private int lastInternalIteration;
	private int extItNr;
	private String populationFile;
	private String networkFile;
	private double fare;
	
	private final double TRAVEL_PT = 0; // not used --> instead: TRAVEL_PT_IN_VEHICLE & TRAVEL_PT_WAITING
	
	private final double TRAVEL_CAR = -1.8534;
	private final double TRAVEL_WALK = -3.6102;
	private final double MONEY_UTILS = 0.14026; // has to be positive, because costs are negative!
	private final double CONSTANT_CAR = -2.2118;
	private final double CONSTANT_PT = 0;
	private final double TRAVEL_PT_IN_VEHICLE = -3.2982; // Utils per Hour
	private final double TRAVEL_PT_WAITING = -5.5356; // Utils per Hour
	
	private final double PERFORMING = 0;
	private final double LATE_ARRIVAL = 0;
	private final double EARLY_DEPARTURE = 0;
	
	public InternalControler(String configFile, int extItNr, String directoryExtIt, int lastInternalIteration, String populationFile, String outputExternalIterationDirPath, int numberOfBuses, String networkFile, double fare) {
		this.configFile = configFile;
		this.directoryExtIt = directoryExtIt;
		this.lastInternalIteration = lastInternalIteration;
		this.extItNr = extItNr;
		this.populationFile = populationFile;
		this.outputExternalIterationDirPath = outputExternalIterationDirPath;
		this.networkFile = networkFile;
		this.fare = fare;
		this.ptLegHandler = new PtLegHandler();
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
		controler.addControlerListener(new MyControlerListener(fare, this.ptLegHandler));
		
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
		
		planCalcScoreConfigGroup.setPerforming_utils_hr(PERFORMING);
		planCalcScoreConfigGroup.setLateArrival_utils_hr(LATE_ARRIVAL);
		planCalcScoreConfigGroup.setEarlyDeparture_utils_hr(EARLY_DEPARTURE);
		
		MyScoringFunctionFactory scoringfactory = new MyScoringFunctionFactory(planCalcScoreConfigGroup, this.ptLegHandler, TRAVEL_PT_IN_VEHICLE, TRAVEL_PT_WAITING);
		controler.setScoringFunctionFactory(scoringfactory);
		controler.run();		
	}

}
