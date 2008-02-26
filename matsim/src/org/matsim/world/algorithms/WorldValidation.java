/* *********************************************************************** *
 * project: org.matsim.*
 * WorldValidation.java
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
import org.matsim.gbl.Gbl;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappingRule;
import org.matsim.world.World;

public class WorldValidation extends WorldAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	static final Logger log = Logger.getLogger(WorldValidation.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldValidation() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean isValid(final MappingRule m) {
		Layer down_layer = m.getDownLayer();
		Layer up_layer = m.getUpLayer();

		// check for empty layers
		if (down_layer.getLocations().isEmpty() || up_layer.getLocations().isEmpty()) {
			log.info("      mapping rule='" + m + "'");
			if ((m.getDownCardinality() != 'm') || (m.getUpCardinality() != 'm')) {
				log.info("      ==> NOT VALID: one or both layers are empty!");
				return false;
			}
		}

		String range = down_layer.getType().toString();
		// calculate current down cardinality
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
		else { Gbl.errorMsg("Haeh?"); }

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
		else { Gbl.errorMsg("Haeh?"); }
		log.info("      range='" + range + "'");

		// compare the current rule with the given mapping rule
		String curr_rule = down_layer.getType().toString() +
				"[" + curr_down_cardinality + "]-[" + curr_up_cardinality + "]" +
				up_layer.getType().toString();
		log.info("      mapping rule='" + m + "'");
		log.info("      current rule='" + curr_rule + "'");
		if (m.getDownCardinality() == 'm') { return true; }
		if (!curr_rule.equals(m.toString())) {
			log.info("      ==> NOT VALID: mapping does not respect the rule!");
			return false;
		}
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final World world) {
		log.info("    running " + this.getClass().getName() + " algorithm...");

		int nof_layers = world.getLayers().size();
		if (nof_layers == 0) { log.info("      nof_layers = 0: no layer to validate."); }
		else if (nof_layers == 1) { log.info("      nof_layers = 1: no mapping to validate."); }
		else {
			Layer layer = world.getBottomLayer();
			while (layer.getUpRule() != null) {
				MappingRule m = layer.getUpRule();
				if (!this.isValid(m)) {
					throw new RuntimeException("[rule=" + m.toString() + "]" + "Validation failed!");
				}
				layer = layer.getUpRule().getUpLayer();
			}
		}
		log.info("    done.");
	}
}
