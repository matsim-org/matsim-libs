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
import org.matsim.gbl.Gbl;
import org.matsim.world.Layer;
import org.matsim.world.MappingRule;
import org.matsim.world.World;

public class WorldCheck {
	
	private final static Logger log = Logger.getLogger(WorldCheck.class);

	private final boolean checkStructure(final World world) {
		log.info("      running checkStructure(final World world)...");

		TreeMap<Id,Layer> layers = world.getLayers();
		TreeMap<String,MappingRule> rules = world.getRules();
		log.info("        generals:");
		log.info("          number of layers = " + layers.size());
		log.info("          number of rules = " + rules.size());
		if (layers.isEmpty()) {
			if (rules.isEmpty()) {
				return true;
			}
			log.warn("            => STRUCTURE NOT VALID!");
			return false;
		}
		else if (layers.size() == 1) {
			if (!rules.isEmpty()) { log.warn("            => STRUCTURE NOT VALID!"); return false; }
		}
		else { // two or more layers
			if (layers.size() != (rules.size()+1))  { log.warn("            => STRUCTURE NOT VALID!"); return false; }
		}

		int l_cnt = 0;
		int m_cnt = 0;
		Layer l = world.getTopLayer();
		log.info("        Traversing Layers and MappingRules:");
		log.info("          top layer = " + l);
		if (l == null) { log.warn("            => STRUCTURE NOT VALID!"); return false; }
		log.info("          layer = " + l);
		l_cnt++;
		while (l.getDownRule() != null) {
			MappingRule m = l.getDownRule();
			log.info("          rule = " + m);
			if (m == null) { log.warn("            => STRUCTURE NOT VALID!"); return false; }
			m_cnt++;
			l = m.getDownLayer();
			log.info("          layer = " + l);
			if (l == null) { log.warn("            => STRUCTURE NOT VALID!"); return false; }
			l_cnt++;
		}
		log.info("          bottom layer = " + world.getBottomLayer());
		log.info("          number of layers traversed = " + l_cnt);
		log.info("          number of rules traversed = " + m_cnt);
		if ((l != world.getBottomLayer()) ||
		    (l_cnt != layers.size()) ||
		    (m_cnt != rules.size())) {
			log.warn("            => STRUCTURE NOT VALID!");
			return false;
		}
		log.info("      done.");
		return true;
	}

	private final boolean checkMapping(final Layer down_layer, final Layer up_layer) {
		log.info("      running checkMapping(final Layer down_layer, final Layer up_layer)...");

		MappingRule m = down_layer.getUpRule();
		log.info("        up_layer =" + up_layer);
		log.info("        rule =" + m);
		log.info("        down_layer =" + down_layer);
		if ((m == null) ||
				(m.getDownLayer() != down_layer) ||
				(m.getUpLayer() != up_layer) ||
				(down_layer.getUpRule() != m) ||
				(up_layer.getDownRule() != m)) {
			log.warn("          => MAPPING NOT VALID!"); 
			return false;
		}

		log.info("      done.");
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("    running " + this.getClass().getName() + " algorithm...");

		boolean ok = this.checkStructure(world);
		if (!ok) { Gbl.errorMsg("Layer/MappingRule structure not valid!"); }

		if (!world.getLayers().isEmpty()) {
			Layer up_layer = world.getTopLayer();
			while (up_layer.getDownRule() != null) {
				Layer down_layer = up_layer.getDownRule().getDownLayer();
				ok = this.checkMapping(down_layer,up_layer);
				if (!ok) { Gbl.errorMsg("Mapping not valid!"); }
				up_layer = down_layer;
			}
		}

		log.info("    done.");
	}
}
