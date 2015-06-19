/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsScenarioWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesWriter10;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;


/**
 * Flexible writer for all kind of signals data output. Easiest way to write all existing data is
 * new SignalsScenarioWriter("myOutputDirectory").writeSignalsData(mySignalsDataInstance);
 * @author dgrether
 */
public class SignalsScenarioWriter {
	
	private static final Logger log = Logger.getLogger(SignalsScenarioWriter.class);
	
	public static final String FILENAME_SIGNAL_SYSTEMS = "output_signal_systems_v2.0.xml.gz";
	public static final String FILENAME_SIGNAL_GROUPS = "output_signal_groups_v2.0.xml.gz";
	public static final String FILENAME_SIGNAL_CONTROL = "output_signal_control_v2.0.xml.gz";
	public static final String FILENAME_AMBER_TIMES = "output_amber_times_v1.0.xml.gz";
	private static final String FILENAME_INTERGREEN_TIMES = "output_intergreen_times_v1.0.xml.gz";

	private String pathToSignalSystemsOutputFilename = null;

	private String pathToSignalGroupsOutputFilename = null;

	private String pathToSignalControlOutputFilename = null;

	private String pathToAmberTimesOutputFilename = null;

	private String pathToIntergreenTimesOutputFilename = null;

	public SignalsScenarioWriter(){
	}
	
	public SignalsScenarioWriter(String outputDirectoryPath){
		this.pathToSignalSystemsOutputFilename = outputDirectoryPath + FILENAME_SIGNAL_SYSTEMS;
		this.pathToSignalGroupsOutputFilename = outputDirectoryPath + FILENAME_SIGNAL_GROUPS;
		this.pathToSignalControlOutputFilename = outputDirectoryPath + FILENAME_SIGNAL_CONTROL;
		this.pathToAmberTimesOutputFilename = outputDirectoryPath + FILENAME_AMBER_TIMES;
		this.pathToIntergreenTimesOutputFilename =  outputDirectoryPath + FILENAME_INTERGREEN_TIMES;
	}
	
	
	public SignalsScenarioWriter(final OutputDirectoryHierarchy controlerIo){
		this.pathToSignalSystemsOutputFilename = controlerIo.getOutputFilename(FILENAME_SIGNAL_SYSTEMS);
		this.pathToSignalGroupsOutputFilename = controlerIo.getOutputFilename(FILENAME_SIGNAL_GROUPS);
		this.pathToSignalControlOutputFilename = controlerIo.getOutputFilename(FILENAME_SIGNAL_CONTROL);
		this.pathToAmberTimesOutputFilename = controlerIo.getOutputFilename(FILENAME_AMBER_TIMES);
		this.pathToIntergreenTimesOutputFilename = controlerIo.getOutputFilename(FILENAME_INTERGREEN_TIMES);
	}
	
	public void writeSignalsData(SignalsData signalsData){
		if (signalsData.getSignalSystemsData() != null){
			this.writeSignalSystemsData(signalsData.getSignalSystemsData());
		}
		else {
			log.info("No SignalSystemsData object set to write!");
		}
		if (signalsData.getSignalGroupsData() != null){
			writeSignalGroupsData(signalsData.getSignalGroupsData());
		}
		else {
			log.info("No SignalGroupsData object set to write!");
		}
		if (signalsData.getSignalControlData() != null){
			writeSignalControlData(signalsData.getSignalControlData());
		}
		else {
			log.info("No SignalControlData object set to write!");
		}
		if (signalsData.getAmberTimesData() != null){
			writeAmberTimesData(signalsData.getAmberTimesData());
		}
		else {
			log.info("No AmberTimesData object set to write!");
		}
		if (signalsData.getIntergreenTimesData() != null){
			writeIntergreenTimesData(signalsData.getIntergreenTimesData());
		}
		else{
			log.info("No IntergreenTimesData object set to write!");
		}
	}
	
	public void writeSignalSystemsData(SignalSystemsData signalSystemsData){
		if (this.pathToSignalSystemsOutputFilename != null) {
			SignalSystemsWriter20 writer = new SignalSystemsWriter20(signalSystemsData);
			writer.write(this.pathToSignalSystemsOutputFilename);
		}
		else {
			log.warn("No path to signal systems output file set!");
		}
	}
	
	public void writeSignalGroupsData(SignalGroupsData signalGroupsData){
		if (this.pathToSignalGroupsOutputFilename != null){
			SignalGroupsWriter20 writer = new SignalGroupsWriter20(signalGroupsData);
			writer.write(this.pathToSignalGroupsOutputFilename);
		}
		else {
			log.warn("No path to signal groups output file set!");
		}
	}
	
	public void writeSignalControlData(SignalControlData controlData){
		if (this.pathToSignalControlOutputFilename != null) {
			SignalControlWriter20 writer = new SignalControlWriter20(controlData);
			writer.write(this.pathToSignalControlOutputFilename);
		}
		else {
			log.warn("No path to signal control output file set!");
		}
	}
	
	public void writeAmberTimesData(AmberTimesData amberTimesData){
		if (this.pathToAmberTimesOutputFilename != null) {
			AmberTimesWriter10 writer = new AmberTimesWriter10(amberTimesData);
			writer.write(this.pathToAmberTimesOutputFilename);
		}
		else {
			log.warn("No path to amber times output file set!");
		}
	}
	
	public void writeIntergreenTimesData(IntergreenTimesData intergreenTimesData){
		if (this.pathToIntergreenTimesOutputFilename != null){
			IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(intergreenTimesData);
			writer.write(this.pathToIntergreenTimesOutputFilename);
		}
		else {
			log.warn("No path to intergreen times output file set!");
		}
	}

	
	public String getSignalSystemsOutputFilename() {
		return pathToSignalSystemsOutputFilename;
	}

	
	public void setSignalSystemsOutputFilename(String signalSystemsOutputFilename) {
		this.pathToSignalSystemsOutputFilename = signalSystemsOutputFilename;
	}

	
	public String getSignalGroupsOutputFilename() {
		return pathToSignalGroupsOutputFilename;
	}

	
	public void setSignalGroupsOutputFilename(String signalGroupsOutputFilename) {
		this.pathToSignalGroupsOutputFilename = signalGroupsOutputFilename;
	}

	
	public String getSignalControlOutputFilename() {
		return pathToSignalControlOutputFilename;
	}

	
	public void setSignalControlOutputFilename(String signalControlOutputFilename) {
		this.pathToSignalControlOutputFilename = signalControlOutputFilename;
	}

	
	public String getAmberTimesOutputFilename() {
		return pathToAmberTimesOutputFilename;
	}

	
	public void setAmberTimesOutputFilename(String amberTimesOutputFilename) {
		this.pathToAmberTimesOutputFilename = amberTimesOutputFilename;
	}

	
	public String getIntergreenTimesOutputFilename() {
		return pathToIntergreenTimesOutputFilename;
	}

	
	public void setIntergreenTimesOutputFilename(String intergreenTimesOutputFilename) {
		this.pathToIntergreenTimesOutputFilename = intergreenTimesOutputFilename;
	}
	
	
}
