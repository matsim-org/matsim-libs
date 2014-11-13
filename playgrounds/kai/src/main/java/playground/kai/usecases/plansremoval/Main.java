/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.kai.usecases.plansremoval;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler ctrl = new Controler( args ) ;
		
		double logitScaleFactor = ctrl.getConfig().planCalcScore().getBrainExpBeta() ;
		double pathSizeLogitExponent = ctrl.getConfig().planCalcScore().getPathSizeLogitBeta() ;
        PlanSelector planSelector = new PathSizeLogitSelector( pathSizeLogitExponent, - logitScaleFactor, ctrl.getScenario().getNetwork());
//		PlanSelector planSelector = new RandomPlanSelector() ;
		

		ctrl.getStrategyManager().setPlanSelectorForRemoval(planSelector) ;
		// yy should probably be replaced by a factory pattern
		
		ctrl.run();

	}

}
