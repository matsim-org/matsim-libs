/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.audiAV.electric;

import java.util.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.util.random.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import playground.michalm.ev.data.*;
import playground.michalm.ev.data.file.*;

public class AudiAVSmallChargingInfrastructureCreator {
	public static void main(String[] args) {
		String dir = "../../../shared-svn/projects/audi_av/scenario/";
		String netFile = dir + "networkc.xml.gz";
		String runDir = "../../../runs-svn/avsim_time_variant_network/";
		String chFilePrefix = runDir + "chargers/chargers_";
		String fractChFilePrefix = runDir + "chargers_small/chargers_";

		String[] scenarios = { "FOSSIL_FUEL_MINUS_20", "ZERO", "MINUS_20", "ONLY_DRIVE", "PLUS_20" };

		int[] counts = { 10560, 15086, 21120, 4800, 6600 };// these numbers do not include plugs...

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(netFile);

		double fraction = 0.001;
		UniformRandom uniform = RandomUtils.getGlobalUniform();
		for (int i = 0; i < scenarios.length; i++) {
			String s = scenarios[i];
			int c = counts[i];

			EvData data = new EvDataImpl();
			new ChargerReader(network, data).readFile(chFilePrefix + c + "_" + s + ".xml");

			List<Charger> fractChargers = new ArrayList<>();
			int totalPlugs = 0;
			for (Charger ch : data.getChargers().values()) {
				int plugs = (int)uniform.floorOrCeil(fraction * ch.getPlugs());
				if (plugs > 0) {
					fractChargers.add(new ChargerImpl(ch.getId(), ch.getPower(), plugs, ch.getLink()));
					totalPlugs += plugs;
				}
			}

			new ChargerWriter(fractChargers).write(fractChFilePrefix + totalPlugs + "_" + s + ".xml");
		}
	}
}
