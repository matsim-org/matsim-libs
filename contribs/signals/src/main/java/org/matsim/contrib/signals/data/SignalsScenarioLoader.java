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
package org.matsim.contrib.signals.data;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesReader10;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesWriter10;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesReader10;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsReader20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.signals.data.SignalsData;


/**
 * Loads all data files related to the traffic signal systems model.
 * 
 * @author dgrether
 *
 */
public class SignalsScenarioLoader {

	private static final Logger log = Logger.getLogger(SignalsScenarioLoader.class);

	private SignalSystemsConfigGroup signalConfig;

	public SignalsScenarioLoader(SignalSystemsConfigGroup config){
		this.signalConfig = config;
	}

	public SignalsData loadSignalsData() {
		SignalsData data = new SignalsDataImpl(this.signalConfig);
		this.loadSystems(data);
		this.loadGroups(data);
		this.loadControl(data);
		if (this.signalConfig.isUseAmbertimes()){
			this.loadAmberTimes(data);
		}
		if (this.signalConfig.isUseIntergreenTimes()){
			this.loadIntergreenTimes(data);
		}
		return data;
	}


	private void loadIntergreenTimes(SignalsData data){
		if (this.signalConfig.getIntergreenTimesFile() != null) {
			IntergreenTimesReader10 reader = new IntergreenTimesReader10(data.getIntergreenTimesData());
			reader.readFile(this.signalConfig.getIntergreenTimesFile());
		}
	}


	private void loadAmberTimes(SignalsData data) {
		if (this.signalConfig.getAmberTimesFile() != null){
			AmberTimesReader10 reader = new AmberTimesReader10(data.getAmberTimesData(), AmberTimesWriter10.AMBERTIMES10);
			reader.readFile(this.signalConfig.getAmberTimesFile());
		}
		else {
			log.info("Signals: No amber times file set, can't load amber times!");
		}
	}

	private void loadControl(SignalsData data){
		if (this.signalConfig.getSignalControlFile() != null){
			SignalControlReader20 controlReader = new SignalControlReader20(data.getSignalControlData(), MatsimSignalSystemsReader.SIGNALCONTROL20);
			controlReader.readFile(this.signalConfig.getSignalControlFile());
		}
		else {
			log.info("Signals: No signal control file set, can't load signal control data!");
		}
	}

	private void loadGroups(SignalsData data) {
		if (this.signalConfig.getSignalGroupsFile() != null){
			SignalGroupsReader20 groupsReader = new SignalGroupsReader20(data.getSignalGroupsData(), MatsimSignalSystemsReader.SIGNALGROUPS20);
			groupsReader.readFile(this.signalConfig.getSignalGroupsFile());
		}
		else {
			log.info("Signals: No signal groups file set, can't load signal groups!");
		}
	}

	private void loadSystems(SignalsData data){
		if (this.signalConfig.getSignalSystemFile() != null){
			SignalSystemsReader20 systemsReader = new SignalSystemsReader20(data.getSignalSystemsData(), MatsimSignalSystemsReader.SIGNALSYSTEMS20);
			systemsReader.readFile(this.signalConfig.getSignalSystemFile());
		}
		else {
			log.info("Signals: No signal systems file set, can't load signal systems information!");
		}
	}


}
