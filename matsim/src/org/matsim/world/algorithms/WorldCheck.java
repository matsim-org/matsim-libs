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

import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Layer;
import org.matsim.world.MappingRule;
import org.matsim.world.World;

public class WorldCheck extends WorldAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldCheck() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean checkStructure(final World world) {
		System.out.println("      running checkStructure(final World world)...");

		TreeMap<IdI,Layer> layers = world.getLayers();
		TreeMap<String,MappingRule> rules = world.getRules();
		System.out.println("        generals:");
		System.out.println("          number of layers = " + layers.size());
		System.out.println("          number of rules = " + rules.size());
		if (layers.isEmpty()) {
			if (rules.isEmpty()) {
				return true;
			}
			System.out.println("            => STRUCTURE NOT VALID!");
			return false;
		}
		else if (layers.size() == 1) {
			if (!rules.isEmpty()) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
		}
		else { // two or more layers
			if (layers.size() != (rules.size()+1))  { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
		}

		int l_cnt = 0;
		int m_cnt = 0;
		Layer l = world.getTopLayer();
		System.out.println("        Traversing Layers and MappingRules:");
		System.out.println("          top layer = " + l);
		if (l == null) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
		System.out.println("          layer = " + l);
		l_cnt++;
		while (l.getDownRule() != null) {
			MappingRule m = l.getDownRule();
			System.out.println("          rule = " + m);
			if (m == null) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
			m_cnt++;
			l = m.getDownLayer();
			System.out.println("          layer = " + l);
			if (l == null) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
			l_cnt++;
		}
		System.out.println("          bottom layer = " + world.getBottomLayer());
		if (l != world.getBottomLayer()) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
		System.out.println("          number of layers traversed = " + l_cnt);
		if (l_cnt != layers.size()) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }
		System.out.println("          number of rules traversed = " + m_cnt);
		if (m_cnt != rules.size()) { System.out.println("            => STRUCTURE NOT VALID!"); return false; }

		System.out.println("      done.");
		return true;
	}

	private final boolean checkMapping(final Layer down_layer, final Layer up_layer) {
		System.out.println("      running checkMapping(final Layer down_layer, final Layer up_layer)...");

		MappingRule m = down_layer.getUpRule();
		System.out.println("        up_layer =" + up_layer);
		System.out.println("        rule =" + m);
		System.out.println("        down_layer =" + down_layer);
		if (m == null) { System.out.println("          => MAPPING NOT VALID!"); return false; }
		if (m.getDownLayer() != down_layer) { System.out.println("          => MAPPING NOT VALID!"); return false; }
		if (m.getUpLayer() != up_layer) { System.out.println("          => MAPPING NOT VALID!"); return false; }
		if (down_layer.getUpRule() != m) { System.out.println("          => MAPPING NOT VALID!"); return false; }
		if (up_layer.getDownRule() != m) { System.out.println("          => MAPPING NOT VALID!"); return false; }

		System.out.println("      done.");
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(World world) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

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

		System.out.println("    done.");
	}
}
