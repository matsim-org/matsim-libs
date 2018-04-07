/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimFactory
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
package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimFactory;


/**
 * @author dgrether
 *
 * @deprecated -- please use inject framework with Provider<Mobsim>. kai, aug'15
 *
 */
@Deprecated // please use inject framework with Provider<Mobsim>. kai, aug'15
public interface MobsimFactory extends MatsimFactory {
	
	Mobsim createMobsim(Scenario sc, EventsManager eventsManager);

}
