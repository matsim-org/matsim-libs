/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSnapshotWriterFactory
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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

class KMLSnapshotWriterFactory implements Provider<SnapshotWriter> {

	private Scenario scenario;
	private OutputDirectoryHierarchy controlerIO;
	private final int iteration;

	@Inject
	KMLSnapshotWriterFactory(Scenario scenario, OutputDirectoryHierarchy controlerIO, ControlerI controler) {
		this.scenario = scenario;
		this.controlerIO = controlerIO;
		this.iteration = controler.getIterationNumber();
	}

	public SnapshotWriter get() {
		String baseFileName = "googleearth.kmz";
		String fileName = controlerIO.getIterationFilename(iteration, baseFileName);
		String coordSystem = scenario.getConfig().global().getCoordinateSystem();
		return new KmlSnapshotWriter(fileName, TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84));
	}

}
