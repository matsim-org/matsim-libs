/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;

import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import playground.yu.scoring.PlansScoringI;

/**
 * Interface for all {@code PlanScoring4PC}
 * 
 * @author yu
 * 
 */
public interface PlansScoring4PC_I extends StartupListener, ScoringListener,
		IterationStartsListener, PlansScoringI {
	@Override
	public CadytsChoice getPlanScorer();
}
