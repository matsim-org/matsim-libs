
/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.analysis;

import static java.util.Map.Entry;

import java.awt.Paint;
import java.util.Collections;
import java.util.Map;

import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.collect.ImmutableMap;

public class DrtServiceProfileView implements ProfileWriter.ProfileView {

	private final DrtServiceProfileCalculator calculator;

	public DrtServiceProfileView(DrtServiceProfileCalculator calculator) {
		this.calculator = calculator;
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		return calculator.getProfile()
				.entrySet()
				.stream()
				.collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return Collections.emptyMap();
	}

	@Override
	public double[] times() {
		return calculator.getTimeDiscretizer().getTimes();
	}
}
