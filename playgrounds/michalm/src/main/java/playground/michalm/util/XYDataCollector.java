/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.util;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.utils.io.IOUtils;

public class XYDataCollector<T extends Identifiable<T>>
		implements MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {
	public interface XYDataCalculator<T> {
		String[] getHeader();

		Coord getCoord(T object);

		String[] calculate(T object);
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
		String file = matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(),
				outputFile);
		writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".xy.gz"));
		writer.writeNext("time", "id", "x", "y", calculator.getHeader());
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (e.getSimulationTime() % interval == 0) {
			String time = (int)e.getSimulationTime() + "";
			for (T o : monitoredObjects) {
				Coord coord = calculator.getCoord(o);
				writer.writeNext(time, o.getId() + "", coord.getX() + "", coord.getY() + "", calculator.calculate(o));
			}
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		writer.close();
	}
}
