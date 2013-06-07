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

package playground.mmoyo.utils.calibration;

import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;

public class NullifyingBasicScoringFunction implements BasicScoring {
	protected double score;
	protected double startTime;
	
	public NullifyingBasicScoringFunction() {}

	@Override
	public void finish() {
		score=0.0;
	}

	@Override
	public double getScore() {
		return 0.0;
	}

	@Override
	public void reset() {
		score= 0.0;
	}
	
	
}
