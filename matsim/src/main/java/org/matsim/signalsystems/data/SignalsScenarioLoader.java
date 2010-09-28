/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsScenarioLoader
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

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesReader10;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsReader20;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsReader20;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalsScenarioLoader {

	private SignalSystemsConfigGroup signalConfig;

	public SignalsScenarioLoader(SignalSystemsConfigGroup config){
		this.signalConfig = config;
	}

	public SignalsData loadSignalsData() {
		SignalsData data = new SignalsDataImpl();
		this.loadSystems(data);
		this.loadGroups(data);
		this.loadControl(data);
		this.loadAmberTimes(data);
		this.loadIntergreenTimes(data);
		return data;
	}
	
	
	private void loadIntergreenTimes(SignalsData data){
		if (this.signalConfig.getIntergreenTimesFile() != null) {
			//TODO write when reader is available
		}
	}
	

	private void loadAmberTimes(SignalsData data) {
		if (this.signalConfig.getAmberTimesFile() != null){
			AmberTimesReader10 reader = new AmberTimesReader10(data.getAmberTimesData(), AmberTimesWriter10.AMBERTIMES10);
			try {
				reader.readFile(this.signalConfig.getAmberTimesFile());
			} catch (JAXBException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadControl(SignalsData data){
		SignalControlReader20 controlReader = new SignalControlReader20(data.getSignalSystemControlData(), MatsimSignalSystemsReader.SIGNALCONTROL20);
		try {
			controlReader.readFile(this.signalConfig.getSignalControlFile());
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void loadGroups(SignalsData data) {
		SignalGroupsReader20 groupsReader = new SignalGroupsReader20(data.getSignalGroupsData(), MatsimSignalSystemsReader.SIGNALGROUPS20);
		try {
			groupsReader.readFile(this.signalConfig.getSignalGroupsFile());
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadSystems(SignalsData data){
		SignalSystemsReader20 systemsReader = new SignalSystemsReader20(data.getSignalSystemsData(), MatsimSignalSystemsReader.SIGNALSYSTEMS20);
		try {
			systemsReader.readFile(this.signalConfig.getSignalSystemFile());
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
