/* *********************************************************************** *
 * project: org.matsim.*
 * WorldSummary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas;

import org.matsim.network.NetworkLayer;
import org.matsim.world.Layer;
import org.matsim.world.World;
import org.matsim.world.ZoneLayer;
import org.matsim.world.algorithms.WorldAlgorithm;

/**
 * @author lnicolas
 * Prints out some information about the given World instance.
 * @see WorldAlgorithm
 */
public class WorldSummary extends WorldAlgorithm {

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.world.algorithms.WorldAlgorithm#run(org.matsim.demandmodeling.world.World)
	 */
	@Override
	public void run(final World world) {

		int zone_layer_count = 0;
		Layer layer = world.getBottomLayer();
		while (layer != null) {
			if (layer instanceof ZoneLayer) {
				zone_layer_count++;
			}
			if (layer.getUpRule() == null) {
				layer = null;
			} else {
				layer = layer.getUpRule().getUpLayer();
			}
		}

		int network_layer_count = 0;
		layer = world.getBottomLayer();
		while (layer != null) {
			if (layer instanceof NetworkLayer) {
				network_layer_count++;
			}
			if (layer.getUpRule() == null) {
				layer = null;
			} else {
				layer = layer.getUpRule().getUpLayer();
			}
		}

		int location_count = 0;
		layer = world.getBottomLayer();
		while (layer != null) {
			location_count += layer.getLocations().size();
			if (layer.getUpRule() == null) {
				layer = null;
			} else {
				layer = layer.getUpRule().getUpLayer();
			}
		}

		System.out.println("      world summary:");
		System.out.println("        name             = " + world.getName());
		System.out.println("      layers summary:");
		System.out.println("        number of layers = " + world.getLayers().size());
		System.out.println("        number of zone layers = " + zone_layer_count);
		System.out.println("        number of network layers = " + network_layer_count);
		System.out.print("      number of persons in population = ");
		if (world.getPopulation() == null
				|| world.getPopulation().getPersons() == null) {
			System.out.println("0 (no population defined)");
		} else {
			System.out.println(world.getPopulation().getPersons().size());
		}
		System.out.println("      number of locations = " + location_count);
		System.out.println("    done.");
	}

}
