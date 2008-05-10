/* *********************************************************************** *
 * project: org.matsim.*
 * WorldReaderMatsimV1.java
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

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for world files of MATSim according to <code>world_v1.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class WorldReaderMatsimV1 extends MatsimXmlParser {

	private final static String WORLD = "world";
	private final static String ZONE = "zone";
	private final static String REF = "ref";

	private World world;
	private ZoneLayer currLayer = null;
	private Zone currZone = null;
	private MappingRule currMappingrule = null;

	public WorldReaderMatsimV1(final World world) {
		this.world = world;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (WORLD.equals(name)) {
			startWorld(atts);
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
		} else if (ZONE.equals(name)) {
			this.currZone = null;
			this.currLayer = null;
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startWorld(final Attributes atts) {
		this.world.setName(atts.getValue("name"));
	}

	private void startZone(final Attributes atts) {
		this.currLayer = (ZoneLayer)this.world.getLayer(atts.getValue("type"));
		if (this.currLayer==null) {
			this.currLayer =
				(ZoneLayer)this.world.createLayer(new IdImpl(atts.getValue("type")),"created by "+this.getClass().getName()+" by zone (id="+atts.getValue("id")+")");
		}
		this.currZone =
			this.currLayer.createZone(atts.getValue("id"), atts.getValue("center_x"), atts.getValue("center_y"), atts.getValue("min_x"), atts.getValue("min_y"),
																 atts.getValue("max_x"), atts.getValue("max_y"), atts.getValue("area"), atts.getValue("name"));
	}

	public void startRef(final Attributes meta) {
		ZoneLayer other_layer = (ZoneLayer)this.world.getLayer(meta.getValue("type"));
		if (other_layer != null) {
			// that layer was already read in. so the zone of that layer must exist
			Zone other_zone = (Zone)other_layer.getLocation(meta.getValue("id"));
			if (other_zone != null) {
				// we found the zone in the other layer
				// check for the four different mapping situations:
				// cell-block; block-cell; block-taz and taz-block
				if (this.currLayer.getType().toString().equals("cell") &&
						other_layer.getType().toString().equals("block")) {
					this.currMappingrule = this.world.getMappingRule(this.currLayer, other_layer);
					if (this.currMappingrule == null) {
						this.currMappingrule = this.world.createMappingRule("cell[+]-[*]block");
					}
					this.world.addMapping(this.currZone,other_zone);
				}
				else if (this.currLayer.getType().toString().equals("block") && other_layer.getType().toString().equals("cell")) {
					this.currMappingrule = this.world.getMappingRule(other_layer,this.currLayer);
					if (this.currMappingrule == null) {
						this.currMappingrule = this.world.createMappingRule("cell[+]-[*]block");
					}
					this.world.addMapping(this.currZone,other_zone);
				}
				else if (this.currLayer.getType().toString().equals("block") && other_layer.getType().toString().equals("taz")) {
					this.currMappingrule = this.world.getMappingRule(this.currLayer,other_layer);
					if (this.currMappingrule == null) {
						this.currMappingrule = this.world.createMappingRule("block[+]-[1]taz");
					}
					this.world.addMapping(this.currZone, other_zone);
				}
				else if (this.currLayer.getType().toString().equals("taz") && other_layer.getType().toString().equals("block")) {
					this.currMappingrule = this.world.getMappingRule(other_layer,this.currLayer);
					if (this.currMappingrule == null) {
						this.currMappingrule = this.world.createMappingRule("block[+]-[1]taz");
					}
					this.world.addMapping(this.currZone,other_zone);
				} else {
					// a mapping between cell and taz. therefore nothing to do...
				}
			} else {
				// the other layer was found. therefore the other zone must exist.
				// but we could not find it. So there's something wrong with the input xml file
				Gbl.errorMsg("[curr_layer_type=" + this.currLayer.getType() + "]" +
				 "[curr_zone_id=" + this.currZone.getId() + "]" +
				 "[other_layer_type=" + other_layer.getType() + "]" +
				 " other_zone not found in existing other_layer.");
			}
		} else {
			// there is no other layer, therefore zone with the required layer
			// will be parsed later, and at that time the mapping will be done.
		}
	}

}
