
/* *********************************************************************** *
 * project: org.matsim.*
 * StandardQSimComponentConfigurator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.components;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.pt.TransitQSimComponentsConfigurator;

public class StandardQSimComponentConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public StandardQSimComponentConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponentsConfig components) {
		
		new DefaultQSimComponentsConfigurator().configure(components);
		// (clears all the components, and puts in those components defined as defaults in the
		// QSimComponentsConfigGroup)
		
		new QSimComponentsFromConfigConfigurator(config).configure(components);
		// (IF a qsimComponentsConfigGroup was added by the user, the components are cleared
		// again, and instead the components from the qsimComponentsConfigGroup are installed)
		
		new TransitQSimComponentsConfigurator(config).configure(components);
		
		new NetworkChangeEventsQSimComponentsConfigurator(config).configure(components);
		
	}

}
