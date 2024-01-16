/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.infrastructure;/*
 *
 * created by jbischoff, 23.08.2018
 */

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.LTHDriveEnergyConsumption;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.vehicles.VehicleType;

import com.google.common.primitives.Doubles;

/**
 * This class reads Energy consumption files from CSV as used in the IDEAS project between TU Berlin and LTH Lund.
 * CSVs contain a slope (in percent) in rows and columns with speeds in m/s.
 * Values in the table are in kWh
 */
public class LTHConsumptionModelReader {

	public LTHConsumptionModelReader() {
	}

	public DriveEnergyConsumption.Factory readURL( URL fileUrl ) {
		List<Double> speeds = new ArrayList<>();
		List<Double> slopes = new ArrayList<>();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setDelimiterTags(new String[] { "," });
		tabularFileParserConfig.setUrl(fileUrl);

		new TabularFileParser().parse(tabularFileParserConfig, row -> {
			if (speeds.isEmpty()) {
				for (int i = 1; i < row.length; i++) {
					speeds.add(Double.parseDouble(row[i]));
				}
			} else {
				slopes.add(Double.parseDouble(row[0]) / 100);
			}
		});

		double[][] consumptionPerSpeedAndSlope = new double[speeds.size()][slopes.size()];

		new TabularFileParser().parse(tabularFileParserConfig, new TabularFileHandler() {
			int line = 0;

			@Override
			public void startRow(String[] row) {
				if (line > 0) {
					double lastValidValue = Double.MIN_VALUE;
					for (int i = 1; i < row.length; i++) {
						double value = Double.parseDouble(row[i]);
						if (Double.isNaN(value)) {
							value = lastValidValue;
						}
						lastValidValue = value;
						consumptionPerSpeedAndSlope[i - 1][line - 1] = value;
					}
				}
				line++;
			}
		});

		return new LTHDriveEnergyConsumption.Factory(Doubles.toArray(speeds), Doubles.toArray(slopes),
				consumptionPerSpeedAndSlope, false);
	}
}
