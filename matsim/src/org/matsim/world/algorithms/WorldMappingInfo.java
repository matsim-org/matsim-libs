/* *********************************************************************** *
 * project: org.matsim.*
 * WorldMappingInfo.java
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

package org.matsim.world.algorithms;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappingRule;
import org.matsim.world.World;

/**
 * Calculates and prints the cardinalities of the given {@link MappingRule mappings} in
 * the {@link World world}.
 *
 * <p><b>Note:</b> It does not assign the calculated cardinalities to the mapping rule, since cardinalities
 * are no longer supported in this context.</p>
 *
 * @see MappingRule
 * @author balmermi
 */
public class WorldMappingInfo {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(WorldMappingInfo.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean calcCardinality(final MappingRule m) {
		log.info("  validate MappingRule: "+m+"...");
		Layer down_layer = m.getDownLayer();
		Layer up_layer = m.getUpLayer();

		// calculate current down cardinality
		String range = down_layer.getType().toString();
		int minmap = Integer.MAX_VALUE;
		int maxmap = Integer.MIN_VALUE;
		Iterator<? extends Location> ul_it = up_layer.getLocations().values().iterator();
		while (ul_it.hasNext()) {
			Location ul = ul_it.next();
			int map = ul.getDownMapping().size();
			if (minmap > map) { minmap = map; }
			if (maxmap < map) { maxmap = map; }
		}
		range = range + "(" + minmap + "..." + maxmap +")-";
		char curr_down_cardinality = Character.UNASSIGNED;
		if ((minmap == 0) && (maxmap == 0)) { curr_down_cardinality = 'm'; }
		else if ((minmap == 0) && (maxmap == 1)) { curr_down_cardinality = '?'; }
		else if ((minmap == 0) && (maxmap > 1)) { curr_down_cardinality = '*'; }
		else if ((minmap == 1) && (maxmap == 1)) { curr_down_cardinality = '1'; }
		else if ((minmap == 1) && (maxmap > 1)) { curr_down_cardinality = '+'; }
		else if ((minmap > 1) && (maxmap > 1)) { curr_down_cardinality = '+'; }
		else {
			throw new RuntimeException("invalid.");
		}

		// calculate current up cardinality
		minmap = Integer.MAX_VALUE;
		maxmap = Integer.MIN_VALUE;
		Iterator<? extends Location> dl_it = down_layer.getLocations().values().iterator();
		while (dl_it.hasNext()) {
			Location dl = dl_it.next();
			int map = dl.getUpMapping().size();
			if (minmap > map) { minmap = map; }
			if (maxmap < map) { maxmap = map; }
		}
		range = range + "(" + minmap + "..." + maxmap +")" + up_layer.getType().toString();
		char curr_up_cardinality = Character.UNASSIGNED;
		if ((minmap == 0) && (maxmap == 0)) { curr_up_cardinality = 'm'; }
		else if ((minmap == 0) && (maxmap == 1)) { curr_up_cardinality = '?'; }
		else if ((minmap == 0) && (maxmap > 1)) { curr_up_cardinality = '*'; }
		else if ((minmap == 1) && (maxmap == 1)) { curr_up_cardinality = '1'; }
		else if ((minmap == 1) && (maxmap > 1)) { curr_up_cardinality = '+'; }
		else if ((minmap > 1) && (maxmap > 1)) { curr_up_cardinality = '+'; }
		else { throw new RuntimeException("invalid."); }

		String curr_rule = down_layer.getType().toString()+"["+curr_down_cardinality+"]-["+
		                   curr_up_cardinality+"]"+up_layer.getType().toString();
		log.info("    range:         " + range);
		log.info("    cardinalities: " + curr_rule);
		log.info("  done.");
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final World world) {
		log.info("running "+this.getClass().getName()+" module (MATSim-ANALYSIS)...");

		int nof_layers = world.getLayers().size();
		if (nof_layers == 0) { log.info("  nof_layers = 0"); }
		else if (nof_layers == 1) { log.info("  nof_layers = 1"); }
		else {
			Layer layer = world.getBottomLayer();
			while (layer.getUpRule() != null) {
				MappingRule m = layer.getUpRule();
				this.calcCardinality(m);
				layer = layer.getUpRule().getUpLayer();
			}
		}
		log.info("done.");
	}
}
