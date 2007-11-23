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

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;

public class MappingRule {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Layer downLayer;
	private final char downCardinality;
	private final Layer upLayer;
	private final char upCardinality;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected MappingRule(final String mapping_rule, final TreeMap<IdI,Layer> layers) {
		if (!mapping_rule.matches("[a-zA-Z]+\\[[\\?\\*1\\+m]\\]-\\[[\\?\\*1\\+m]\\][a-zA-Z]+")) {
			Gbl.errorMsg("[mapping_rule=" + mapping_rule + " does not match]");
		}
		String [] strings = mapping_rule.split("\\[|\\]-\\[|\\]");
		this.downLayer = layers.get(new Id(strings[0]));
		this.downCardinality = strings[1].charAt(0);
		this.upCardinality = strings[2].charAt(0);
		this.upLayer = layers.get(new Id(strings[3]));

		if (this.downLayer == null) { Gbl.errorMsg("[down_layer_type=" + strings[0] + " does not exist]"); }
		if (this.upLayer == null) { Gbl.errorMsg("[up_layer_type=" + strings[3] + " does not exist]"); }
		if (this.downLayer.equals(this.upLayer)) { Gbl.errorMsg("[down_layer=" + this.downLayer + "and up_layer=" + this.upLayer + " are equal]"); }
		if (this.downLayer.getUpRule() != null) { Gbl.errorMsg("[down_layer=" + this.downLayer + " already holds a up_rule]"); }
		if (this.upLayer.getDownRule() != null) { Gbl.errorMsg("[up_layer_type=" + this.upLayer.getType() + " already holds a down_rule"); }

		this.downLayer.setUpRule(this);
		this.upLayer.setDownRule(this);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Layer getDownLayer() {
		return this.downLayer;
	}

	public final Layer getUpLayer() {
		return this.upLayer;
	}

	public final char getDownCardinality() {
		return this.downCardinality;
	}

	public final char getUpCardinality() {
		return this.upCardinality;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return this.downLayer.getType() + "[" + this.downCardinality + "]-[" +
				this.upCardinality + "]" + this.upLayer.getType();
	}
}
