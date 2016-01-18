/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;

import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;

/**
 * @author mrieser
 */
public class DynusTMobsimFactory implements MobsimFactory {

	private final DynusTConfig dc;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final Network dynusTnet;
	private final MatsimServices controler;

	public DynusTMobsimFactory(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final Network dynusTnet, final MatsimServices controler) {
		this.dc = dc;
		this.ttMatrix = ttMatrix;
		this.dynusTnet = dynusTnet;
		this.controler = controler;
	}

	@Override
	public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
		return new DynusTMobsim(this.dc, this.ttMatrix, sc, eventsManager, this.dynusTnet, this.controler, this.controler.getIterationNumber());
	}

}
