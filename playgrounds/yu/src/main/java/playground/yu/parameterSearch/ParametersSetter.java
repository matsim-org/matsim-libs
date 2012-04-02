/* *********************************************************************** *
 * project: org.matsim.*
 * ParametersSetter.java
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

/**
 * 
 */
package playground.yu.parameterSearch;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.CalibrationConfig;
import playground.yu.scoring.withAttrRecorder.leftTurn.CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty;

/**
 * sets parameters of scoringfunction into {@code Config} and also into
 * {@code Controler}
 * 
 * @author yu
 * 
 */
public class ParametersSetter {
	public static void setParameters(Controler ctl,
			Tuple<String, Double>... parameters) {
		// set new parameters in config
		Config cfg = ctl.getConfig();
		PlanCalcScoreConfigGroup scoringCfg = cfg.planCalcScore();
		for (Tuple<String, Double> parameter : parameters) {
			String name = parameter.getFirst();
			String value = Double.toString(parameter.getSecond());

			if (scoringCfg.getParams().containsKey(name)) {
				scoringCfg.addParam(name, value);
			} else {
				cfg.setParam(CalibrationConfig.BSE_CONFIG_MODULE_NAME, name,
						value);
			}
		}
		// set new parameters in Controler
		ctl.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
				cfg, ctl.getNetwork()));
	}
}
