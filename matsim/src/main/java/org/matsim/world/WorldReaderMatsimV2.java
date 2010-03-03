/* *********************************************************************** *
 * project: org.matsim.*
 * WorldReaderMatsimV2.java
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

import java.util.Stack;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for world files of MATSim according to <code>world_v2.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class WorldReaderMatsimV2 extends MatsimXmlParser {

	private final static String WORLD = "world";
	private final static String LAYER = "layer";
	private final static String MAPPING = "mapping";
	private final static String ZONE = "zone";
	private final static String REF = "ref";

	private Scenario scenario;
	private World world;
	private ZoneLayer currLayer = null;
	private Layer currUpLayer = null;
	private Layer currDownLayer = null;

	public WorldReaderMatsimV2(final ScenarioImpl scenario) {
		this.scenario = scenario;
		this.world = scenario.getWorld();
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (WORLD.equals(name)) {
			startWorld(atts);
		} else if (LAYER.equals(name)) {
			startLayer(atts);
		} else if (MAPPING.equals(name)) {
			startMapping(atts);
		} else if (ZONE.equals(name)) {
			startZone(atts);
		} else if (REF.equals(name)) {
			startRef(atts);
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (WORLD.equals(name)) {
			this.world.complete();
			this.world = null;
		} else if (LAYER.equals(name)) {
			this.currLayer = null;
		}
	}

	private void startWorld(final Attributes atts) {
		this.world.setName(atts.getValue("name"));
	}

	private void startLayer(final Attributes meta) {
		this.currLayer = (ZoneLayer)this.world.createLayer(scenario.createId(meta.getValue("type")),meta.getValue("name"));
	}

	private void startZone(final Attributes atts) {
		this.currLayer.createZone(scenario.createId(atts.getValue("id")), atts.getValue("center_x"), atts.getValue("center_y"), atts.getValue("min_x"), atts.getValue("min_y"),
				 atts.getValue("max_x"), atts.getValue("max_y"), atts.getValue("area"), atts.getValue("name"));
	}

	private void startMapping(final Attributes atts) {
		String [] strings = atts.getValue("mapping_rule").split("\\[|\\]-\\[|\\]");
		this.currDownLayer = this.world.getLayer(new IdImpl(strings[0]));
		this.currUpLayer = this.world.getLayer(new IdImpl(strings[3]));
		this.currDownLayer.setUpLayer(this.currUpLayer);
		this.currUpLayer.setDownLayer(this.currDownLayer);
	}

	private void startRef(final Attributes atts) {
		this.world.addMapping(this.currDownLayer, this.currUpLayer, scenario.createId(atts.getValue("down_zone_id")), scenario.createId(atts.getValue("up_zone_id")));

	}

}
