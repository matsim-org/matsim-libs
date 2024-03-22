/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.analysis;

import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

import org.matsim.contrib.common.timeprofile.ProfileWriter;

import com.google.common.collect.ImmutableMap;

public class FleetSizeProfileView implements ProfileWriter.ProfileView {
	private final FleetSizeProfileCalculator calculator;

	public FleetSizeProfileView(FleetSizeProfileCalculator calculator) {
		this.calculator = calculator;
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		return ImmutableMap.of("Fleet size", calculator.getActiveVehiclesProfile());
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return ImmutableMap.of("Fleet size", Color.RED);
	}

	@Override
	public double[] times() {
		return calculator.getTimeDiscretizer().getTimes();
	}
}
