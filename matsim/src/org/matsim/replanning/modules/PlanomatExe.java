/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatExe.java
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

package org.matsim.replanning.modules;

import org.matsim.config.Module;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;

public class PlanomatExe extends ExternalModule {

	private static final String SCENARIO = "scenario";
	private static final String SCENARIO_INPUT_PLANS_FILENAME = "inputPlansFilename";
	private static final String SCENARIO_WORKING_PLANS_FILENAME = "workingPlansFilename";
	private static final String SCENARIO_WORKING_EVENTS_TXT_FILENAME = "workingEventsTxtFilename";
	private static final String SCENARIO_NETWORK_FILENAME = "networkFilename";
	private static final String SCENARIO_INITIAL_PLANS_FILENAME = "initialPlansFilename";
	private static final String SCENARIO_WORKING_EVENTS_FILENAME = "workingEventsFilename";
	private static final String SCENARIO_WORKING_SNAPSHOT_FILENAME = "workingSnapshotFilename";
	private static final String SCENARIO_FINAL_STORAGE_LOCATION = "finalStorageLocation";
	private static final String SCENARIO_WORKING_PARTITIONING_FILENAME = "workingPartitioningFilename";
	private static final String SCENARIO_SNAPSHOT_INTERVAL = "snapshotInterval";
	private static final String SCENARIO_USE_EVENTS = "useEvents";
	private static final String SCENARIO_USE_TXT_EVENTS = "useTxtEvents";
	private static final String SCENARIO_USE_XML_EVENTS = "useXmlEvents";
	private static final String SCENARIO_N_PLANS_FILES = "nPlansFiles";
	private static final String SCENARIO_DIRECT_VERBOSITY = "directVerbosity";
	private static final String SCENARIO_CONSERVE_DISK_SPACE = "conserveDiskSpace";
	private static final String SCENARIO_LAST_ITERATION = "lastIteration";
	
	public PlanomatExe(final String exePath) {
		super(exePath, "planomat");
	}

	@Override
	protected PlansWriter getPlansWriterHandler() {
		String filename = this.outFileRoot + "/" + this.moduleId + ExternalInFileName;
		String dtd = "http://www.vsp.tu-berlin.de/projects/Matsim/data/dtd/plans_v0.dtd";
		String version = "v0";
		return new PlansWriter(new Plans(Plans.USE_STREAMING), filename, version);
	}

	@Override
	protected PlansReaderI getPlansReader(final Plans plans) {
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		return plansReader;
	}

	@Override
	public void prepareExternalExeConfig() {
		super.prepareExternalExeConfig();

		// overwrite the file paths for in- and out-plans and set root_dir
		Module rootModule = this.extConfig.createModule("root_dir");
		rootModule.addParam("root_dir", this.outFileRoot);

		
		this.extConfig.setParam(SCENARIO, SCENARIO_INPUT_PLANS_FILENAME, this.outFileRoot + "/" + this.moduleId + ExternalInFileName);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_PLANS_FILENAME, this.outFileRoot + "/" + this.moduleId + ExternalOutFileName);
		int iteration = Controler.getIteration() - 1;
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_EVENTS_TXT_FILENAME, Controler.getIterationPath(iteration) + "/" + iteration + ".events.txt");
		String networkFilename = Gbl.getConfig().findParam("network", "inputNetworkFile");
		this.extConfig.setParam(SCENARIO, SCENARIO_NETWORK_FILENAME, networkFilename);

		this.extConfig.setParam(SCENARIO, SCENARIO_INPUT_PLANS_FILENAME, this.moduleId + ExternalInFileName);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_PLANS_FILENAME, this.moduleId + ExternalOutFileName);

		/* All the following parameters are included for matsimC backwards compatibility.
		 * They are not used in planomat, but defined as required by the matsimC config class
		 * so ignore the values set.  */
		final String notUsed = "not used";
		this.extConfig.setParam(SCENARIO, SCENARIO_INITIAL_PLANS_FILENAME, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_EVENTS_FILENAME, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_SNAPSHOT_FILENAME, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_FINAL_STORAGE_LOCATION, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_FINAL_STORAGE_LOCATION, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_PARTITIONING_FILENAME, notUsed);
		this.extConfig.setParam(SCENARIO, SCENARIO_SNAPSHOT_INTERVAL, "100000");
		this.extConfig.setParam(SCENARIO, SCENARIO_USE_EVENTS, "1");
		this.extConfig.setParam(SCENARIO, SCENARIO_USE_TXT_EVENTS, "1");
		this.extConfig.setParam(SCENARIO, SCENARIO_USE_XML_EVENTS, "0");
		this.extConfig.setParam(SCENARIO, SCENARIO_N_PLANS_FILES, "1");
		this.extConfig.setParam(SCENARIO, SCENARIO_DIRECT_VERBOSITY, "1");
		this.extConfig.setParam(SCENARIO, SCENARIO_CONSERVE_DISK_SPACE, "2");
		this.extConfig.setParam(SCENARIO, SCENARIO_LAST_ITERATION, "999");
	}

}
