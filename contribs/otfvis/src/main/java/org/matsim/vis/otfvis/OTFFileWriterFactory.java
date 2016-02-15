/* *********************************************************************** *
 * project: org.matsim.*
 * OTFFileWriterFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import javax.inject.Inject;
import javax.inject.Provider;

public class OTFFileWriterFactory implements Provider<SnapshotWriter> {

	private Scenario scenario;
	private OutputDirectoryHierarchy controlerIO;
	private final ReplanningContext iterationContext;

	@Inject
	OTFFileWriterFactory(Scenario scenario, ReplanningContext replanningContext, OutputDirectoryHierarchy controlerIO) {
		this.scenario = scenario;
		this.iterationContext = replanningContext;
		this.controlerIO = controlerIO;
	}

	@Override
	public SnapshotWriter get() {
		String fileName = controlerIO.getIterationFilename(iterationContext.getIteration(), "otfvis.mvi");
		OTFFileWriter writer = new OTFFileWriter(scenario, fileName);
		return writer;
	}

}
