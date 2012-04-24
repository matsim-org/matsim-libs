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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * @author Ihab
 *
 */
public class InternalControler {

	PtLegHandler ptLegHandler;
	
	private final Scenario scenario;
	private final String directoryExtIt;
	private final int lastInternalIteration;
	private final double fare;
	
	// TODO: adjust parameters
	final double MARGINAL_UTILITY_OF_MONEY = 0.14026;
	private final double TRAVEL_PT = 0; // not used --> instead: TRAVEL_PT_IN_VEHICLE & TRAVEL_PT_WAITING
	private final double TRAVEL_CAR = 0;
	private final double TRAVEL_WALK = -1.7568;
	private final double CONSTANT_CAR = -2.2118;
	private final double CONSTANT_PT = 0;
	private final double PERFORMING = 1.8534;
	private final double LATE_ARRIVAL = 0;
	
	private final double monetaryCostPerKm = -0.11; // AUD per km 
	private final double agentStuckScore = -100;
	
	private final double TRAVEL_PT_IN_VEHICLE = -1.4448; // Utils per Hour
	private final double TRAVEL_PT_WAITING = -3.6822; // Utils per Hour

	public InternalControler(Scenario scenario, String directoryExtIt, int lastInternalIteration, double fare) {
		this.scenario = scenario;
		this.directoryExtIt = directoryExtIt;
		this.lastInternalIteration = lastInternalIteration;
		this.fare = fare;
		this.ptLegHandler = new PtLegHandler();
	}
	
	public void run() {
		
		Controler controler = new Controler(this.scenario);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new PtControlerListener(this.fare, this.ptLegHandler));
		
		TransitConfigGroup transit = controler.getConfig().transit();
		transit.setTransitScheduleFile(this.directoryExtIt + "/scheduleFile.xml");
		transit.setVehiclesFile(this.directoryExtIt + "/vehiclesFile.xml");
		
		ControlerConfigGroup controlerConfGroup = controler.getConfig().controler();
		controlerConfGroup.setFirstIteration(0);
		controlerConfGroup.setLastIteration(this.lastInternalIteration);
		
		int writeInterval = 0;
		if (this.lastInternalIteration==0){
			writeInterval = 1;
		}
		else {
			writeInterval = this.lastInternalIteration;
		}
		controlerConfGroup.setWriteEventsInterval(writeInterval);
		controlerConfGroup.setWritePlansInterval(writeInterval);
		
		controlerConfGroup.setOutputDirectory(this.directoryExtIt + "/internalIterations");
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();	
		planCalcScoreConfigGroup.setTravelingPt_utils_hr(TRAVEL_PT);
		planCalcScoreConfigGroup.setMarginalUtilityOfMoney(MARGINAL_UTILITY_OF_MONEY);
		planCalcScoreConfigGroup.setTraveling_utils_hr(TRAVEL_CAR);
		planCalcScoreConfigGroup.setTravelingWalk_utils_hr(TRAVEL_WALK);
		planCalcScoreConfigGroup.setConstantCar(CONSTANT_CAR);
		planCalcScoreConfigGroup.setConstantPt(CONSTANT_PT);
		planCalcScoreConfigGroup.setPerforming_utils_hr(PERFORMING);
		planCalcScoreConfigGroup.setLateArrival_utils_hr(LATE_ARRIVAL);
		
		// TODO: monetaryCostRateCar aus Config bzw. ConfigGroup; Egress vs. Access-Scoring seperately?
		MyScoringFunctionFactory scoringfactory = new MyScoringFunctionFactory(planCalcScoreConfigGroup, ptLegHandler, TRAVEL_PT_IN_VEHICLE, TRAVEL_PT_WAITING, monetaryCostPerKm, agentStuckScore);
		controler.setScoringFunctionFactory(scoringfactory);
		controler.run();		
	}

	public double getMarginalUtlOfMoney() {
		return MARGINAL_UTILITY_OF_MONEY;
	}
	
	public double getSumOfWaitingTimes() {
		return this.ptLegHandler.getSumOfWaitingTimes();
	}
}
