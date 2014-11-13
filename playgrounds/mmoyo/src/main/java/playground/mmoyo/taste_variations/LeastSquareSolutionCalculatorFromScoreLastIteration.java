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

package playground.mmoyo.taste_variations;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
  * Calculates and stores svd values from plans at the end of a iteration
  */
public class LeastSquareSolutionCalculatorFromScoreLastIteration implements IterationEndsListener{
	
	public LeastSquareSolutionCalculatorFromScoreLastIteration(){
	
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
	
		//calculate svd from scores from last iteration and iteration number 10 
		if (   event.getIteration() == event.getControler().getConfig().controler().getLastIteration() ||  event.getIteration() ==10) {
			System.err.println("calculating svd values.........");
			MyLeastSquareSolutionCalculator lssCalculator = new MyLeastSquareSolutionCalculator(event.getControler().getScenario().getNetwork(), event.getControler().getScenario().getTransitSchedule(), MyLeastSquareSolutionCalculator.SVD );
            lssCalculator.run(event.getControler().getScenario().getPopulation());
			String STR_Solutions = "SVDSolutions.xml.gz";
			String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), STR_Solutions);
			lssCalculator.writeSolutionObjAttr(filename);
			STR_Solutions = null;
			filename = null;
			lssCalculator = null;
		}
	}
	
}
