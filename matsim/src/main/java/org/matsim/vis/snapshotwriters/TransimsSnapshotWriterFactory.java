/* *********************************************************************** *
 * project: org.matsim.*
 * TransimsSnapshotWriterFactory.java
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

import org.matsim.core.controler.OutputDirectoryHierarchy;

import javax.inject.Named;
import javax.inject.Provider;

class TransimsSnapshotWriterFactory implements Provider<SnapshotWriter> {

	private OutputDirectoryHierarchy controlerIO;
	private final int iteration;

	TransimsSnapshotWriterFactory(OutputDirectoryHierarchy controlerIO, @Named("iteration") int iteration) {
		this.iteration = iteration;
		this.controlerIO = controlerIO;
	}

	@Override
	public SnapshotWriter get() {
		String fileName = controlerIO.getIterationFilename(iteration, "T.veh.gz");
		return new TransimsSnapshotWriter(fileName);
	}

}
