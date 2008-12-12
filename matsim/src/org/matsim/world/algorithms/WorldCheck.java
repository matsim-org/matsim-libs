/* *********************************************************************** *
 * project: org.matsim.*
 * WorldCheck.java
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

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.world.Layer;
import org.matsim.world.MappingRule;
import org.matsim.world.World;

/**
 * Analyzes and prints the general structure of a {@link World world}. It also produces
 * warnings for invalid structures or mapping rules but it does not repair/adapt/change
 * the structures in any way.
 * 
 * @author balmermi
 */
public class WorldCheck {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(WorldCheck.class);

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean checkStructure(final World world) {
		log.info("  analyse world structure...");

		TreeMap<Id,Layer> layers = world.getLayers();
		TreeMap<String,MappingRule> rules = world.getRules();
		log.info("    generals:");
		log.info("      number of layers = " + layers.size());
		log.info("      number of rules = " + rules.size());
		if (layers.isEmpty()) {
			if (rules.isEmpty()) { log.info("  done."); return true; }
			log.info("  done."); return false;
		}
		else if (layers.size() == 1) {
			if (!rules.isEmpty()) { log.info("  done."); return false; }
		}
		else { // two or more layers
			if (layers.size() != (rules.size()+1))  { log.info("  done."); return false; }
		}

		int l_cnt = 0;
		int m_cnt = 0;
		Layer l = world.getTopLayer();
		log.info("    traversing layers and mapping rules:");
		log.info("      top layer = " + l);
		if (l == null) { log.info("  done."); return false; }
		log.info("      layer = " + l);
		l_cnt++;
		while (l.getDownRule() != null) {
			MappingRule m = l.getDownRule();
			log.info("      rule = " + m);
			if (m == null) { log.info("  done."); return false; }
			m_cnt++;
			l = m.getDownLayer();
			log.info("      layer = " + l);
			if (l == null) { log.info("  done."); return false; }
			l_cnt++;
		}
		log.info("      bottom layer = " + world.getBottomLayer());
		log.info("      number of layers traversed = " + l_cnt);
		log.info("      number of rules traversed = " + m_cnt);
		if ((l != world.getBottomLayer()) || (l_cnt != layers.size()) || (m_cnt != rules.size())) { log.info("  done."); return false; }
		log.info("  done.");  return true;
	}

	//////////////////////////////////////////////////////////////////////

	private final boolean checkMapping(final Layer down_layer, final Layer up_layer) {
		log.info("  analyse mapping...");

		MappingRule m = down_layer.getUpRule();
		log.info("    up_layer =" + up_layer);
		log.info("    rule =" + m);
		log.info("    down_layer =" + down_layer);
		if ((m == null) || (m.getDownLayer() != down_layer) || (m.getUpLayer() != up_layer) ||
		    (down_layer.getUpRule() != m) || (up_layer.getDownRule() != m)) {
			log.info("  done."); return false;
		}

		log.info("  done."); return true;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("running "+this.getClass().getName()+" module (MATSim-ANALYSIS)...");

		if (!checkStructure(world)) { log.warn("  => INVALID STRUCTURE"); }

		if (!world.getLayers().isEmpty()) {
			Layer up_layer = world.getTopLayer();
			while (up_layer.getDownRule() != null) {
				Layer down_layer = up_layer.getDownRule().getDownLayer();
				if (!checkMapping(down_layer,up_layer)) { log.warn("  => INVALID MAPPING"); }
				up_layer = down_layer;
			}
		}

		log.info("done.");
	}
}
