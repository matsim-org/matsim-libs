/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.jtrrouter.transims;

import java.io.PrintWriter;

/**
 * @author michalm
 */
public class TransimsVehicle {
	public static final String HEADER = "VEHICLE\tHHOLD\tLOCATION\tTYPE\tSUBTYPE";

	private final int hhold;
	private final int location;// parking_id
	private final int type;
	private final int subType;

	public TransimsVehicle(int hhold, int location, int type, int subtype) {
		this.hhold = hhold;
		this.location = location;
		this.type = type;
		this.subType = subtype;
	}

	public void write(PrintWriter writer) {
		writer.println(hhold + "\t" + hhold + "\t" + location + "\t" + type + "\t" + subType);
	}
}
