/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.fleet;

import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

class FleetControlerListener implements ShutdownListener {

	private static final String OUTPUT_FILE_NAME = "vehicles.xml.gz";
	private final OutputDirectoryHierarchy controlerIO;
	private FleetSpecification fleetSpecification;
	private final String mode;
	private final DvrpLoadType dvrpLoadType;

	FleetControlerListener(String mode, OutputDirectoryHierarchy controlerIO, FleetSpecification fleetSpecification, DvrpLoadType dvrpLoadType) {
		this.mode = mode;
		this.controlerIO = controlerIO;
		this.fleetSpecification = fleetSpecification;
		this.dvrpLoadType = dvrpLoadType;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		FleetWriter writer = new FleetWriter(fleetSpecification.getVehicleSpecifications().values().stream(), dvrpLoadType);
		writer.write(controlerIO.getOutputFilename(mode + "_" +  OUTPUT_FILE_NAME));
	}
}
