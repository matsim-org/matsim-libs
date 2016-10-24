/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scenario.ScenarioByConfigModule;

/**
 * @author thibautd
 */
public class SocialNetworkSamplerModule extends AbstractModule {
	@Override
	public void install() {
		// assumes the Injector is created the MATSim way.
		install( new ScenarioByConfigModule() );
		bind( SocialNetworkSampler.class );
	}
}
