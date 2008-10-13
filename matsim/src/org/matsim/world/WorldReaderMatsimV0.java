/* *********************************************************************** *
 * project: org.matsim.*
 * WorldReaderMatsimV0.java
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

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for world files of MATSim according to <code>world_v0.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class WorldReaderMatsimV0 extends MatsimXmlParser {

	private final static String MUNICIPALITIES = "municipalities";
	private final static String MUNICIPALITY = "municipality";
	private final static String HEKTAR = "hektar";

	private World world;
	private ZoneLayer hektar_layer = null;
	private ZoneLayer currLayer = null;
	private Zone currZone = null;
	private int hektar_cnt;

	public WorldReaderMatsimV0(final World world) {
		this.world = world;
		this.hektar_cnt = 1;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (MUNICIPALITIES.equals(name)) {
			startWorld(atts);
		} else if (MUNICIPALITY.equals(name)) {
			startZone(atts);
		} else if (HEKTAR.equals(name)) {
			startRef(atts);
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (MUNICIPALITIES.equals(name)) {
			this.currLayer = null;
			this.hektar_layer = null;
			this.world.complete();
			this.world = null;
		} else if (MUNICIPALITY.equals(name)) {
			this.currZone = null;
			this.currLayer = null;
		}
	}

	private void startWorld(final Attributes atts) {
		this.world.setName(atts.getValue("name"));
		this.hektar_layer = (ZoneLayer)this.world.createLayer(new IdImpl("hektar"), "created by " + this.getClass().getName() + " from a world_v0-file");
		this.currLayer = (ZoneLayer)this.world.createLayer(new IdImpl("municipality"), "created by " + this.getClass().getName() + " from a world_v0-file");
		this.world.createMappingRule("hektar[+]-[1]municipality");
	}

	private void startZone(final Attributes atts) {
		this.currZone =
			this.currLayer.createZone(atts.getValue("id"),null,null,null,null,null,null,null,atts.getValue("name"));
	}

	private void startRef(final Attributes meta) {
		int x_min = Integer.parseInt(meta.getValue("x100"));
		int y_min = Integer.parseInt(meta.getValue("y100"));
		Zone hektar_zone =
			this.hektar_layer.createZone(Integer.toString(this.hektar_cnt), Integer.toString(x_min+50), Integer.toString(y_min+50),
																	 Integer.toString(x_min), Integer.toString(y_min), Integer.toString(x_min+100), Integer.toString(y_min+100),
																	 Integer.toString(10000), "hektar of " + this.currZone.getName());
		this.hektar_cnt++;
		this.world.addMapping(hektar_zone,this.currZone);
	}

}
