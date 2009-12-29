/* *********************************************************************** *
 * project: org.matsim.*
 * SNRePlanner.java
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
package playground.jhackney.deprecated;

import org.matsim.population.Population;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;

import playground.jhackney.algorithms.SNSecLocShortest;
import playground.jhackney.algorithms.SNSecLocRandom;

public class SNRePlanner  extends MultithreadedModuleA{
	
    public SNRePlanner(Population plans) {
    	super();
//    	this.estimator = estimator;
    //
//    	PlanomatConfig.init();
    	SNConfig.snsetup(plans);
    }

    @Override
    public PlanAlgorithmI getPlanAlgoInstance() {
//	return new SNSecLocShortest();
	return new SNSecLocRandom();
	

//	PlanAlgorithmI planomatAlgorithm = null;
//	planomatAlgorithm =  new PlanOptimizeTimes( this.estimator);
//
//	return planomatAlgorithm;
    }
}
