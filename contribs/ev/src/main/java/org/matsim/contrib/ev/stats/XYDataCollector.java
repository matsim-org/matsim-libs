/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.stats;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.IOUtils;

public class XYDataCollector<T extends Identifiable<T>>
		implements MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {
	public interface XYDataCalculator<T> {
		String[] getHeader();

		Coord getCoord(T object);

		double[] calculate(T object);
	}

	private final Iterable<T> monitoredObjects;
	private final XYDataCalculator<T> calculator;
	private final int interval;
	private final String outputFile;
	private final MatsimServices matsimServices;

	private CompactCSVWriter writer;

	public XYDataCollector(Iterable<T> monitoredObjects, XYDataCalculator<T> calculator, int interval,
			String outputFile, MatsimServices matsimServices) {
		this.monitoredObjects = monitoredObjects;
		this.calculator = calculator;
		this.interval = interval;
		this.outputFile = outputFile;
		this.matsimServices = matsimServices;
	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		String file = matsimServices.getControlerIO()
				.getIterationFilename(matsimServices.getIterationNumber(), outputFile);
		writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".xy.gz"));
		writer.writeNext(new CSVLineBuilder().addAll("time", "id", "x", "y").addAll(calculator.getHeader()));
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (e.getSimulationTime() % interval == 0) {
			String time = (int)e.getSimulationTime() + "";
			for (T o : monitoredObjects) {
				Coord coord = calculator.getCoord(o);
				CSVLineBuilder builder = new CSVLineBuilder().addAll(time, o.getId() + "", coord.getX() + "",
						coord.getY() + "");
				for (Double value : calculator.calculate(o)) {
					builder.add(value + "");
				}
				writer.writeNext(builder);
			}
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		writer.close();
	}
}
