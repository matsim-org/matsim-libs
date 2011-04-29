/* *********************************************************************** *
 * project: org.matsim.*
 * DummyBseParamCalibrationControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.scoring.PlansScoring4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.withLegModeASC.CharyparNagelScoringFunctionFactory4PC;

/**
 * @author yu
 *
 */
public abstract class BseParamCalibrationControler extends Controler {

	protected BseParamCalibrationControlerListener extension;
	protected PlansScoring4PC plansScoring4PC;

	public BseParamCalibrationControler(String[] args) {
		super(args);
	}

	public BseParamCalibrationControler(Config config) {
		super(config);
	}

	public PlansScoring4PC getPlansScoring4PC() {
		return plansScoring4PC;
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactory4PC(config
				.planCalcScore());
	}

	@Override
	protected abstract void loadCoreListeners();

}