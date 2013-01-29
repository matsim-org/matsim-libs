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
package playground.ikaddoura.optimization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.optimization.handler.PtLegHandler;
import playground.ikaddoura.optimization.scoring.OptimizationScoringFunctionFactory;

/**
 * @author Ihab
 *
 */
public class InternalControler {
	private final static Logger log = Logger.getLogger(InternalControler.class);

	private PtLegHandler ptScoringHandler;
	
	private final Scenario scenario;
	private final String directoryExtIt;
	private final long randomSeed;
	private final double fare;
	
	private final double MARGINAL_UTILITY_OF_MONEY = 0.062;
	private final double PERFORMING = 0.96;

	private final double CONSTANT_CAR = 0.0;
	private final double TRAVEL_CAR = 0.0;
	private final double MONETARY_DISTANCE_COST_RATE_CAR = -0.00040;

	private final double CONSTANT_WALK = 0.0;
	private final double TRAVEL_WALK = -20.0; // for the ptRouter to avoid transit walks over longer distances, not used for scoring because of the following differentiation in access and egress time:
	private final double TRAVEL_PT_ACCESS = -0.;
	private final double TRAVEL_PT_EGRESS = -0.;
	
	private double CONSTANT_PT;
	private final double TRAVEL_PT_IN_VEHICLE = -0.18;
	private final double TRAVEL_PT_WAITING = -0.096;
	private final double MONETARY_DISTANCE_COST_RATE_PT = 0.0;
	private final double LINE_SWITCH = 0.0;

	private double LATE_ARRIVAL = 0.0;
	private final double EARLY_DEPARTURE = 0.0;
	private final double WAITING = 0.0;
	private final double STUCK_SCORE = -100;

	public InternalControler(Scenario scenario, String directoryExtIt, double fare, long randomSeed) {
		this.scenario = scenario;
		this.directoryExtIt = directoryExtIt;
		this.randomSeed = randomSeed;
		this.fare = fare;
		this.ptScoringHandler = new PtLegHandler();

		this.CONSTANT_PT = scenario.getConfig().planCalcScore().getConstantPt();
		log.info("Pt constant set to " + this.CONSTANT_PT);
		
//		this.LATE_ARRIVAL = -1. * this.PERFORMING * 2.; // coming early (which is the opportunity costs of time) multiplied by 3 --> multiplying by 2 (see Hollander 2006)

	}
	
	public void run() {
		
		if (randomSeed==0) {
			log.info("Random seed is taken from configFile. Random seed: " + scenario.getConfig().global().getRandomSeed());
		} else {
			log.info("Random seed is not taken from configFile. Setting random seed to " + randomSeed);
			scenario.getConfig().global().setRandomSeed(randomSeed);
		}
	
		new TransitScheduleReaderV1(scenario).readFile(this.directoryExtIt + "/scheduleFile.xml");
		new VehicleReaderV1(((ScenarioImpl) scenario).getVehicles()).readFile(this.directoryExtIt + "/vehiclesFile.xml");

		Controler controler = new Controler(this.scenario);
		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addControlerListener(new PtControlerListener(this.fare, this.ptScoringHandler));
		
		ControlerConfigGroup controlerConfGroup = controler.getConfig().controler();
		controlerConfGroup.setOutputDirectory(this.directoryExtIt + "/internalIterations");
		if (controlerConfGroup.getLastIteration() == 0) {
			controlerConfGroup.setWriteEventsInterval(1);
			controlerConfGroup.setWritePlansInterval(1);
		} else {
			controlerConfGroup.setWriteEventsInterval(controlerConfGroup.getLastIteration());
			controlerConfGroup.setWritePlansInterval(controlerConfGroup.getLastIteration());
		}
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = controler.getConfig().planCalcScore();	
		planCalcScoreConfigGroup.setMarginalUtilityOfMoney(MARGINAL_UTILITY_OF_MONEY);
		planCalcScoreConfigGroup.setPerforming_utils_hr(PERFORMING);

		planCalcScoreConfigGroup.setConstantCar(CONSTANT_CAR);
		planCalcScoreConfigGroup.setTraveling_utils_hr(TRAVEL_CAR);
		planCalcScoreConfigGroup.setMonetaryDistanceCostRateCar(MONETARY_DISTANCE_COST_RATE_CAR);

		planCalcScoreConfigGroup.setConstantWalk(CONSTANT_WALK);
		planCalcScoreConfigGroup.setTravelingWalk_utils_hr(TRAVEL_WALK);
		planCalcScoreConfigGroup.setMonetaryDistanceCostRatePt(MONETARY_DISTANCE_COST_RATE_PT);
		
		planCalcScoreConfigGroup.setConstantPt(CONSTANT_PT);
		planCalcScoreConfigGroup.setTravelingPt_utils_hr(TRAVEL_PT_IN_VEHICLE);
		planCalcScoreConfigGroup.setMarginalUtlOfWaitingPt_utils_hr(TRAVEL_PT_WAITING);
		planCalcScoreConfigGroup.setUtilityOfLineSwitch(LINE_SWITCH);
		
		planCalcScoreConfigGroup.setLateArrival_utils_hr(LATE_ARRIVAL);
		planCalcScoreConfigGroup.setEarlyDeparture_utils_hr(EARLY_DEPARTURE);
		planCalcScoreConfigGroup.setMarginalUtlOfWaiting_utils_hr(WAITING);
		
		OptimizationScoringFunctionFactory scoringfactory = new OptimizationScoringFunctionFactory(planCalcScoreConfigGroup, scenario.getNetwork(), ptScoringHandler, TRAVEL_PT_IN_VEHICLE, TRAVEL_PT_WAITING, STUCK_SCORE, TRAVEL_PT_ACCESS, TRAVEL_PT_EGRESS);
		controler.setScoringFunctionFactory(scoringfactory);

		// just for information so that schedule and vehicles appear in the output_config:
		TransitConfigGroup transit = controler.getConfig().transit();
		transit.setTransitScheduleFile(this.directoryExtIt+"/scheduleFile.xml");
		transit.setVehiclesFile(this.directoryExtIt+"/vehiclesFile.xml");
		
		controler.setCreateGraphs(false);
		controler.run();		
	}

	public double getMarginalUtlOfMoney() {
		return MARGINAL_UTILITY_OF_MONEY;
	}
	
}
