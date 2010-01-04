/* *********************************************************************** *
 * project: org.matsim.*
 * BasicSignalSystemConfigurationsImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.config;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicSignalSystemConfigurationsImpl implements
		BasicSignalSystemConfigurations {

	private final BasicSignalSystemConfigurationsFactory factory = new BasicSignalSystemConfigurationsFactory();
	
	private final SortedMap<Id, BasicSignalSystemConfiguration> signalSystemConfigs = new TreeMap<Id, BasicSignalSystemConfiguration>();

	public BasicSignalSystemConfigurationsImpl(){
	}
	
	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemConfigurations#getSignalSystemConfigurations()
	 */
	public SortedMap<Id, BasicSignalSystemConfiguration> getSignalSystemConfigurations() {
		return this.signalSystemConfigs;
	}

	public BasicSignalSystemConfigurationsFactory getFactory() {
		return this.factory;
	}

	public void addSignalSystemConfiguration(BasicSignalSystemConfiguration systemConfig) {
		this.signalSystemConfigs.put(systemConfig.getSignalSystemId(), systemConfig);
	}

}
