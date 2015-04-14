/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QSimFactory.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.thibautd.mobsim.pseudoqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSimUtils;

/**
 * Constructs an instance of the modular QSim based on the required features as per the Config file.
 * Used by the Controler.
 * You can get an instance of this factory and use it to create a QSim if you are running a "complete"
 * simulation based on a config file. For test cases or specific experiments, it may be better to
 * plug together your QSim instance from your own code.
 * It is not recommended to mix, i.e. to construct a QSim with this code and then add further modules to
 * your own code.
 *
 */
class QSimFactory implements MobsimFactory {

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		return QSimUtils.createDefaultQSim(sc, eventsManager);
	}

}
