/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatX12Initialiser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.mfeil;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.scoring.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatX12Initialiser extends MultithreadedModuleA{
	
	private final LegTravelTimeEstimator 	estimator;
	private final PreProcessLandmarks 		preProcessRoutingData;
	private final NetworkLayer 				network;
	private final TravelCost 				travelCostCalc;
	private final TravelTime 				travelTimeCalc;
	private final ScoringFunctionFactory 	factory;
	public static ArrayList<String>			actTypes; 
	private final Controler					controler;
	private List<PlanAlgorithm>  			planAlgoInstances;
	private boolean 						constrained;
	private static final Logger 			log = Logger.getLogger(LocationChoice.class);

	
	public PlanomatX12Initialiser (final ControlerTest controlerTest, final LegTravelTimeEstimator estimator) {
		
		this.estimator = estimator;
		this.preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.network = controlerTest.getNetwork();
		this.preProcessRoutingData.run(network);
		this.travelCostCalc = controlerTest.getTravelCostCalculator();
		this.travelTimeCalc = controlerTest.getTravelTimeCalculator();
		//factory = Gbl.getConfig().planomat().getScoringFunctionFactory();//TODO @MF: Check whether this is correct (Same scoring function as for Planomat)!
		this.factory = new CharyparNagelScoringFunctionFactory();
		
		int gblCounter = 0;
		
		actTypes = new ArrayList<String>();
		while (Gbl.getConfig().findParam("planCalcScore", "activityType_"+gblCounter)!=null){
			actTypes.add(Gbl.getConfig().findParam("planCalcScore", "activityType_"+gblCounter));
			gblCounter++;
		}
		
		this.planAlgoInstances = new Vector<PlanAlgorithm>();
		this.controler = controlerTest;
		this.init(network, controler);
		
	}
	
	private void init(
			final NetworkLayer network,
			final Controler controler) {
		
		//if (Gbl.getConfig().locationchoice().getMode().toString().equals("true")) {
			//this.constrained = true;
			//log.info("Doing constrained location choice");
		//}
		//else {
			//this.constrained = false;
			//log.info("Doing random location choice on univ. choice set");
		//}
		this.constrained = false; //replaces above code 
		
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		

		PlanAlgorithm planomatXAlgorithm = null;
		planomatXAlgorithm =  new PlanomatX12 (this.estimator, this.network, this.travelCostCalc, 
				this.travelTimeCalc, this.preProcessRoutingData, this.factory, this.constrained, this.controler);

		return planomatXAlgorithm;
	}
}
