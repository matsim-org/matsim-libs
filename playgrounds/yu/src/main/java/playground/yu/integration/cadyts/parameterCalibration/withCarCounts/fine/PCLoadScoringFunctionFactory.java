/* *********************************************************************** *
 * project: org.matsim.*
 * PCLoadScoringFunctionFactory.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.fine;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.withLegModeASC.CharyparNagelScoringFunctionFactory4PC;

public class PCLoadScoringFunctionFactory implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		controler
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactory4PC(
						controler.getConfig().planCalcScore(), controler
								.getNetwork()));
	}
}
