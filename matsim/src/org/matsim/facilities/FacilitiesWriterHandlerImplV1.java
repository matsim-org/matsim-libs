/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriterHandlerImplV1.java
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

package org.matsim.facilities;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.utils.misc.Time;

public class FacilitiesWriterHandlerImplV1 implements FacilitiesWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <facilities ... > ... </facilities>
	//////////////////////////////////////////////////////////////////////

	public void startFacilities(final Facilities facilities, final BufferedWriter out) throws IOException {
		out.write("<facilities");
		if (facilities.getName() != null) {
			out.write(" name=\"" + facilities.getName() + "\"");
		}
		out.write(">\n\n");
	}


	public void endFacilities(final BufferedWriter out) throws IOException {
		out.write("</facilities>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <facility ... > ... </facility>
	//////////////////////////////////////////////////////////////////////

	public void startFacility(final Facility facility, final BufferedWriter out) throws IOException {
		out.write("\t<facility");
		out.write(" id=\"" + facility.getId() + "\"");
		out.write(" x=\"" + facility.getCenter().getX() + "\"");
		out.write(" y=\"" + facility.getCenter().getY() + "\"");
		if (facility.getDesc() != null) { out.write(" desc=\"" + facility.getDesc() + "\""); }
		out.write(">\n");
	}

	public void endFacility(final BufferedWriter out) throws IOException {
		out.write("\t</facility>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	public void startActivity(final ActivityOption activity, final BufferedWriter out) throws IOException {
		out.write("\t\t<activity");
		out.write(" type=\"" + activity.getType() + "\"");
		out.write(">\n");
	}

	public void endActivity(final BufferedWriter out) throws IOException {
		out.write("\t\t</activity>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <capacity ... />
	//////////////////////////////////////////////////////////////////////

	public void startCapacity(final ActivityOption activity, final BufferedWriter out) throws IOException {
		if (activity.getCapacity() != Integer.MAX_VALUE) {
			out.write("\t\t\t<capacity");
			out.write(" value=\"" + activity.getCapacity() + "\"");
			out.write(" />\n");
		}
	}

	public void endCapacity(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <opentime ... />
	//////////////////////////////////////////////////////////////////////

	public void startOpentime(final BasicOpeningTime opentime, final BufferedWriter out) throws IOException {
		out.write("\t\t\t<opentime");
		out.write(" day=\"" + opentime.getDay() + "\"");
		out.write(" start_time=\"" + Time.writeTime(opentime.getStartTime()) + "\"");
		out.write(" end_time=\"" + Time.writeTime(opentime.getEndTime()) + "\"");
		out.write(" />\n");
	}

	public void endOpentime(final BufferedWriter out) throws IOException {
	}
	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
