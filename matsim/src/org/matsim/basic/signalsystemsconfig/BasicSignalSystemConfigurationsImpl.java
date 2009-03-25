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
package org.matsim.basic.signalsystemsconfig;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicSignalSystemConfigurationsImpl implements
		BasicSignalSystemConfigurations {

	private BasicSignalSystemConfigurationsBuilder builder = new BasicSignalSystemConfigurationsBuilder();
	
	private Map<Id, BasicSignalSystemConfiguration> signalSystemConfigs = new HashMap<Id, BasicSignalSystemConfiguration>();

	public BasicSignalSystemConfigurationsImpl(){
	}
	
	/**
	 * @see org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurations#getSignalSystemConfigurations()
	 */
	public Map<Id, BasicSignalSystemConfiguration> getSignalSystemConfigurations() {
		return this.signalSystemConfigs;
	}

	public BasicSignalSystemConfigurationsBuilder getBuilder() {
		return this.builder;
	}

}
