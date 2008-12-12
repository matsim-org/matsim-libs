/* *********************************************************************** *
 * project: org.matsim.*
 * MappingRule.java
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

package org.matsim.world;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;

/**
 * Connects two {@link Layer layers} such that for all layers in the {@link World world}
 * a hierarchy is defined. The bottom layer is usually the {@link NetworkLayer network}, the next higher is
 * the {@link Facilities facility layer}, followed by an open number of {@link ZoneLayer zone layers}.
 * 
 * <p><b>Note:</b> The concept of the mapping cardinality is deprecated. It will be always treated as an 
 * <code>[m]-[m]</code> cardinality (undefined/unchecked cardinality).</p>
 * 
 * <p><b>Note:</b> The concept of a hierarchical layer structure will be changed in the future.</p>
 * 
 * @author balmermi
 */
public class MappingRule {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(MappingRule.class);

	private final Layer downLayer;
	private final Layer upLayer;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected MappingRule(final String mapping_rule, final TreeMap<Id,Layer> layers) {
		if (!mapping_rule.matches("[a-zA-Z]+\\[[\\?\\*1\\+m]\\]-\\[[\\?\\*1\\+m]\\][a-zA-Z]+")) {
			throw new RuntimeException("mapping_rule="+mapping_rule+" does not match.");
		}
		String [] strings = mapping_rule.split("\\[|\\]-\\[|\\]");
		downLayer = layers.get(new IdImpl(strings[0]));
		char downC = strings[1].charAt(0);
		char upC = strings[2].charAt(0);
		upLayer = layers.get(new IdImpl(strings[3]));

		if ((downC != 'm') || (upC != 'm')) { log.warn("Cardinalities of a mapping between two layers do not have any purpose anymore. It will be treated as [m]-[m]"); }
		
		if (downLayer == null) { throw new RuntimeException("down_layer_type="+strings[0]+" does not exist."); }
		if (upLayer == null) { throw new RuntimeException("up_layer_type="+strings[3]+" does not exist."); }
		if (downLayer.equals(this.upLayer)) { throw new RuntimeException("down_layer="+downLayer+" and up_layer="+upLayer+" are equal."); }
		if (downLayer.getUpRule() != null) { throw new RuntimeException("down_layer="+downLayer.getType()+" already holds a up_rule."); }
		if (upLayer.getDownRule() != null) { throw new RuntimeException("up_layer_type="+upLayer.getType()+" already holds a down_rule."); }

		this.downLayer.setUpRule(this);
		this.upLayer.setDownRule(this);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Layer getDownLayer() {
		return downLayer;
	}

	public final Layer getUpLayer() {
		return upLayer;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return downLayer.getType()+"[m]-[m]"+upLayer.getType();
	}
}
