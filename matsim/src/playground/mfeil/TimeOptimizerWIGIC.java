/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizer.java
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

import java.util.ArrayList;
import java.util.List;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.routes.LinkCarRoute;
import org.matsim.scoring.PlanScorer;
import org.matsim.controler.Controler;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * @author Matthias Feil
 * TimeOptimizerWIGIC: "What I Get Is Correct"
 * This is an extension of the standard TimeOptimizer that saves runtime by
 * assuming that the plans to be optimized are "correct" (i.e. times of the legs
 * and acts are meaningful and correct, no overlapping or similar). It basically saves
 * the initial clean-up loop compared to the standard TimeOptimizer.
 * The TimeOptimizerWIGIC is particularly designed to serve as FinalTimer in the 
 * PlanomatX.
 */

public class TimeOptimizerWIGIC extends TimeOptimizer implements PlanAlgorithm { 
		
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizerWIGIC (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		super(estimator, scorer);
	
		this.OFFSET					= 900;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 10;
		this.NEIGHBOURHOOD_SIZE		= 10;
		
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	public void run (Plan basePlan){
		
		if (basePlan.getActsLegs().size()==1) return;		
		this.processPlan(basePlan);
	}
}
	

	
