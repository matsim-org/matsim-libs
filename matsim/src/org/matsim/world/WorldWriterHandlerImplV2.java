/* *********************************************************************** *
 * project: org.matsim.*
 * WorldWriterHandlerImplV2.java
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

import java.io.BufferedWriter;
import java.io.IOException;

public class WorldWriterHandlerImplV2 implements WorldWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <world ... > ... </world>
	//////////////////////////////////////////////////////////////////////

	public void startWorld(final World world, final BufferedWriter out)
			throws IOException {
		out.write("<world");
		if (world.getName() != null) {
			out.write(" name=\"" + world.getName() + "\"");
		}
		out.write(">\n\n");
	}

	public void endWorld(final BufferedWriter out) throws IOException {
		out.write("</world>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <layer ... > ... </layer>
	//////////////////////////////////////////////////////////////////////

	public void startLayer(final ZoneLayer layer, final BufferedWriter out) throws IOException {
		out.write("\t<layer");
		out.write(" type=\"" + layer.getType() + "\"");
		if (layer.getName() != null) {
			out.write(" name=\"" + layer.getName() + "\"");
		}
		out.write(">\n");
	}

	public void endLayer(final BufferedWriter out) throws IOException {
		out.write("\t</layer>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <zone ... />
	//////////////////////////////////////////////////////////////////////

	public void startZone(final Zone zone, final BufferedWriter out) throws IOException {
		out.write("\t\t<zone");
		out.write(" id=\"" + zone.getId() + "\"");
		if (zone.getCenter() != null) {
			out.write(" center_x=\"" + zone.getCenter().getX() + "\"");
			out.write(" center_y=\"" + zone.getCenter().getY() + "\"");
		}
		if (zone.getMin() != null) {
			out.write(" min_x=\"" + zone.getMin().getX() + "\"");
			out.write(" min_y=\"" + zone.getMin().getY() + "\"");
		}
		if (zone.getMax() != null) {
			out.write(" max_x=\"" + zone.getMax().getX() + "\"");
			out.write(" max_y=\"" + zone.getMax().getY() + "\"");
		}
		if (!Double.isNaN(zone.getArea())) {
			out.write(" area=\"" + zone.getArea() + "\"");
		}
		if (zone.getName() != null) {
			out.write(" name=\"" + zone.getName() + "\"");
		}
		out.write(" />\n");
	}

	public void endZone(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <mapping ... > ... </mapping>
	//////////////////////////////////////////////////////////////////////

	public void startMapping(final MappingRule mappingRule, final BufferedWriter out) throws IOException {
		out.write("\t<mapping");
		out.write(" mapping_rule=\"" + mappingRule.toString() + "\"");
		out.write(">\n");
	}

	public void endMapping(final BufferedWriter out) throws IOException {
		out.write("\t</mapping>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <ref ... />
	//////////////////////////////////////////////////////////////////////

	public void startRef(final Zone down_zone, final Zone up_zone, final BufferedWriter out) throws IOException {
		out.write("\t\t<ref");
		out.write(" down_zone_id=\"" + down_zone.getId() + "\"");
		out.write(" up_zone_id=\"" + up_zone.getId() + "\"");
		out.write(" />\n");
	}

	public void endRef(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
