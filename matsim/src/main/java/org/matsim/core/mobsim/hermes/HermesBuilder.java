/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

class HermesBuilder implements AllowsConfiguration {

	@Override
	public AllowsConfiguration addOverridingModule(AbstractModule abstractModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration addOverridingQSimModule(AbstractQSimModule qsimModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration addQSimModule(AbstractQSimModule qsimModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration configureQSimComponents(QSimComponentsConfigurator configurator) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Hermes build(Scenario scenario, EventsManager eventsmanager) {
		return new Hermes(scenario, eventsmanager);
	}

}
