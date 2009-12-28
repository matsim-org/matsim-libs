/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizerWIGIC.java
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
package playground.mfeil;


import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;




/**
 * @author Matthias Feil
 * TimeOptimizerWIGIC: "What I Get Is Correct"
 * This is an extension of the standard TimeOptimizer that saves runtime by
 * assuming that the plans to be optimized are "correct" (i.e. times of the legs
 * and acts are meaningful and correct, no overlaps or similar). It basically saves
 * the initial clean-up loop compared to the standard TimeOptimizer.
 * The TimeOptimizerWIGIC is particularly designed to serve as FinalTimer in the 
 * PlanomatX.
 */

public class TimeOptimizerWIGIC extends TimeOptimizer implements PlanAlgorithm { 
		
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizerWIGIC (Controler controler, LegTravelTimeEstimatorFactory estimatorFactory, PlanScorer scorer){
		
		super(controler, estimatorFactory, scorer);
	
		this.OFFSET					= 900;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 10;
		this.NEIGHBOURHOOD_SIZE		= 10;
		
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	@Override
	public void run (PlanImpl basePlan){
		
		if (basePlan.getPlanElements().size()==1) return;		
		this.processPlan(basePlan);
	}
}
	

	
