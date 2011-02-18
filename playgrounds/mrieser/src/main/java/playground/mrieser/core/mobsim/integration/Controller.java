/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.integration;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.ptproject.qsim.QSimFactory;

import playground.mrieser.core.mobsim.usecases.OptimizedCarSimFactory;
import playground.mrieser.core.mobsim.usecases.RefMobsimFactory;
import playground.mrieser.core.mobsim.usecases.TeleportOnlyMobsimFactory;

public class Controller {

	public static void main(String[] args) {
		MobsimFactory mobsimFactory = null;
		int shiftBy = 0;
		if ("-teleportOnly".equals(args[0])) {
			mobsimFactory = new TeleportOnlyMobsimFactory();
			shiftBy = 1;
		} else if ("-refSim".equals(args[0])) {
			mobsimFactory = new RefMobsimFactory();
			shiftBy = 1;
		} else if ("-fastCarSim".equals(args[0])) {
			int numberOfThreads = Integer.parseInt(args[1]);
			mobsimFactory = new OptimizedCarSimFactory(numberOfThreads);
			((OptimizedCarSimFactory) mobsimFactory).setTeleportedModes(new String[] {TransportMode.bike, TransportMode.pt, TransportMode.ride, TransportMode.walk, "undefined"});
			shiftBy = 2;
		} else if ("-qsim".equals(args[0])) {
			mobsimFactory = new QSimFactory();
			shiftBy = 1;
		} else if ("-queuesim".equals(args[0])) {
			mobsimFactory = new QueueSimulationFactory();
			shiftBy = 1;
		}
		if (mobsimFactory == null) {
			throw new RuntimeException("no mobsim specified!");
		}

		Controler c = new Controler(shiftArray(shiftBy, args));
		c.setMobsimFactory(mobsimFactory);
		c.run();
	}

	private static String[] shiftArray(int shiftBy, final String[] array) {
		String[] newArray = new String[array.length - shiftBy];
		System.arraycopy(array, shiftBy, newArray, 0, newArray.length);
		return newArray;
	}

}
