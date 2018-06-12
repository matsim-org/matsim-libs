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

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesReader10;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsReader;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesReader10;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsReader20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.UncheckedIOException;


/**
 * Loads all data files related to the traffic signal systems model.
 * 
 * @author dgrether
 *
 */
public class SignalsDataLoader {

	private static final Logger log = Logger.getLogger(SignalsDataLoader.class);

	private Config config;
	private SignalSystemsConfigGroup signalConfig;

	public SignalsDataLoader(Config config){
		this.config = config;
		this.signalConfig = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
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
		if (this.signalConfig.isUseConflictingDirections()) {
			this.loadConflicts(data);
		}
		return data;
	}


	private void loadIntergreenTimes(SignalsData data){
		if (this.signalConfig.getIntergreenTimesFile() != null) {
			IntergreenTimesReader10 reader = new IntergreenTimesReader10(data.getIntergreenTimesData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getIntergreenTimesFile());
			try {
				reader.readStream(filename.openStream());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}


	private void loadAmberTimes(SignalsData data) {
		if (this.signalConfig.getAmberTimesFile() != null){
			AmberTimesReader10 reader = new AmberTimesReader10(data.getAmberTimesData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getAmberTimesFile());
			reader.readFile(filename.getFile());
		}
		else {
			log.info("Signals: No amber times file set, can't load amber times!");
		}
	}

	private void loadControl(SignalsData data){
		if (this.signalConfig.getSignalControlFile() != null){
			SignalControlReader20 reader = new SignalControlReader20(data.getSignalControlData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getSignalControlFile());
			try {
				reader.readStream(filename.openStream());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			log.info("Signals: No signal control file set, can't load signal control data!");
		}
	}

	private void loadGroups(SignalsData data) {
		if (this.signalConfig.getSignalGroupsFile() != null){
			SignalGroupsReader20 reader = new SignalGroupsReader20(data.getSignalGroupsData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getSignalGroupsFile());
			try {
				reader.readStream(filename.openStream());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			log.info("Signals: No signal groups file set, can't load signal groups!");
		}
	}

	private void loadSystems(SignalsData data){
		if (this.signalConfig.getSignalSystemFile() != null){
			SignalSystemsReader20 reader = new SignalSystemsReader20(data.getSignalSystemsData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getSignalSystemFile());
			try {
				reader.readStream(filename.openStream());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			log.info("Signals: No signal systems file set, can't load signal systems information!");
		}
	}
	
	private void loadConflicts(SignalsData data) {
		if (this.signalConfig.getConflictingDirectionsFile() != null) {
			ConflictingDirectionsReader reader = new ConflictingDirectionsReader(data.getConflictingDirectionsData());
			URL filename = ConfigGroup.getInputFileURL(config.getContext(), this.signalConfig.getConflictingDirectionsFile());
			try {
				reader.parse(filename.openStream());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			log.info("Signals: No conflicting directions file set, can't load conflicting directions information!");
		}
	}


}
