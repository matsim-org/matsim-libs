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
package org.matsim.signalsystems.data;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesData;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsWriter20;


/**
 * Flexible writer for all kind of signals data output. Easiest way to write all existing data is
 * new SignalsScenarioWriter("myOutputDirectory").writeSignalsData(mySignalsDataInstance);
 * @author dgrether
 */
public class SignalsScenarioWriter {
	
	private static final Logger log = Logger.getLogger(SignalsScenarioWriter.class);
	
	private String signalSystemsOutputFilename = "output_signal_systems_v2.0.xml";
	private String signalGroupsOutputFilename = "output_signal_groups_v2.0.xml";
	private String signalControlOutputFilename = "output_signal_control_v2.0.xml";
	private String amberTimesOutputFilename = "output_amber_times_v1.0.xml";
	private String intergreenTimesOutputFilename = "output_intergreen_times_v1.0.xml";

	public SignalsScenarioWriter(){
	}
	
	public SignalsScenarioWriter(final String outputDirectory){
		String sep = System.getProperty("file.separator");
		this.signalSystemsOutputFilename = outputDirectory + sep + this.signalSystemsOutputFilename;
		this.signalGroupsOutputFilename = outputDirectory + sep + this.signalGroupsOutputFilename;
		this.signalControlOutputFilename = outputDirectory + sep + this.signalControlOutputFilename;
		this.amberTimesOutputFilename = outputDirectory + sep + this.amberTimesOutputFilename;
		this.intergreenTimesOutputFilename = outputDirectory + sep + this.intergreenTimesOutputFilename;
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
	}
	
	public void writeSignalSystemsData(SignalSystemsData signalSystemsData){
		SignalSystemsWriter20 writer = new SignalSystemsWriter20(signalSystemsData);
		writer.write(this.signalSystemsOutputFilename);
	}
	
	public void writeSignalGroupsData(SignalGroupsData signalGroupsData){
		SignalGroupsWriter20 writer = new SignalGroupsWriter20(signalGroupsData);
		writer.write(this.signalGroupsOutputFilename);
	}
	
	public void writeSignalControlData(SignalControlData controlData){
		SignalControlWriter20 writer = new SignalControlWriter20(controlData);
		writer.write(this.signalControlOutputFilename);
	}
	
	public void writeAmberTimesData(AmberTimesData amberTimesData){
		AmberTimesWriter10 writer = new AmberTimesWriter10(amberTimesData);
		writer.write(this.amberTimesOutputFilename);
	}
	
//	public void writeIntergreenTimesData(IntergreenTimesData intergreenTimesData){
//		throw new UnsupportedOperationException("Not implemented yet");
//	}

	
	public String getSignalSystemsOutputFilename() {
		return signalSystemsOutputFilename;
	}

	
	public void setSignalSystemsOutputFilename(String signalSystemsOutputFilename) {
		this.signalSystemsOutputFilename = signalSystemsOutputFilename;
	}

	
	public String getSignalGroupsOutputFilename() {
		return signalGroupsOutputFilename;
	}

	
	public void setSignalGroupsOutputFilename(String signalGroupsOutputFilename) {
		this.signalGroupsOutputFilename = signalGroupsOutputFilename;
	}

	
	public String getSignalControlOutputFilename() {
		return signalControlOutputFilename;
	}

	
	public void setSignalControlOutputFilename(String signalControlOutputFilename) {
		this.signalControlOutputFilename = signalControlOutputFilename;
	}

	
	public String getAmberTimesOutputFilename() {
		return amberTimesOutputFilename;
	}

	
	public void setAmberTimesOutputFilename(String amberTimesOutputFilename) {
		this.amberTimesOutputFilename = amberTimesOutputFilename;
	}

	
	public String getIntergreenTimesOutputFilename() {
		return intergreenTimesOutputFilename;
	}

	
	public void setIntergreenTimesOutputFilename(String intergreenTimesOutputFilename) {
		this.intergreenTimesOutputFilename = intergreenTimesOutputFilename;
	}
	
	
}
