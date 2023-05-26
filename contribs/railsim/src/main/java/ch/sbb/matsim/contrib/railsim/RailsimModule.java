/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim;

import ch.sbb.matsim.contrib.railsim.analysis.linkstates.RailsimLinkStateControlerListener;
import ch.sbb.matsim.contrib.railsim.analysis.trainstates.RailsimTrainStateControlerListener;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import com.google.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
 * Railsim module installing all needed component.
 */
public class RailsimModule extends AbstractModule {

	@Override
	public void install() {
		installQSimModule(new RailsimQSimModule());
		ConfigUtils.addOrGetModule(getConfig(), RailsimConfigGroup.class);

		bind(RailsimLinkStateControlerListener.class).in(Singleton.class);
		addControlerListenerBinding().to(RailsimLinkStateControlerListener.class);

		bind(RailsimTrainStateControlerListener.class).in(Singleton.class);
		addControlerListenerBinding().to(RailsimTrainStateControlerListener.class);
	}
}
