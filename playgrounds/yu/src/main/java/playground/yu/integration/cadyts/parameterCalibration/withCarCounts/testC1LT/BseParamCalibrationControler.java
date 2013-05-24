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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.PlansScoring4PC_I;

/**
 * @author yu
 * 
 */
public abstract class BseParamCalibrationControler extends Controler {

	protected BseParamCalibrationControlerListener extension;
	protected PlansScoring4PC_I plansScoring4PC;

	public BseParamCalibrationControler(Config config) {
		super(config);
	}

	public BseParamCalibrationControler(String[] args) {
		super(args);
	}

	public PlansScoring4PC_I getPlansScoring4PC() {
		return plansScoring4PC;
	}

}