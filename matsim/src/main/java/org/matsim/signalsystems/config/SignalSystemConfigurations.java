/* *********************************************************************** *
 * project: org.matsim.*
 * BasicSignalSystemConfigurations
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * @author dgrether
 * 
 */
public interface SignalSystemConfigurations extends MatsimToplevelContainer {

	public SignalSystemConfigurationsFactory getFactory();

	/**
	 * 
	 * @return a map containing all signal system configurations organized by the
	 *         Id of the SignalSystem
	 */
	public SortedMap<Id, SignalSystemConfiguration> getSignalSystemConfigurations();

	/**
	 * adds the given SignalSystemConfiguration to the map of this container.
	 * @param systemConfig
	 */
	public void addSignalSystemConfiguration(SignalSystemConfiguration systemConfig);
}
