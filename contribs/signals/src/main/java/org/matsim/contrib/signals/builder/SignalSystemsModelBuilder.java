/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsModelBuilder
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
package org.matsim.contrib.signals.builder;

import org.matsim.contrib.signals.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public interface SignalSystemsModelBuilder {

	public SignalSystemsManager createAndInitializeSignalSystemsManager();
	
	public SignalModelFactory getSignalModelFactory();
	
	public void setSignalModelFactory(SignalModelFactory factory);
}
