/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesReaderMatsimV1.java
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

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for facilities-files of MATSim according to <code>facilities_v1.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class FacilitiesReaderMatsimV1 extends MatsimXmlParser {

	private final static String FACILITIES = "facilities";
	private final static String FACILITY = "facility";
	private final static String ACTIVITY = "activity";
	private final static String CAPACITY = "capacity";
	private final static String OPENTIME = "opentime";

	private final Facilities facilities;
	private Facility currfacility = null;
	private Activity curractivity = null;
	
	public FacilitiesReaderMatsimV1(final Facilities facilities) {
		this.facilities = facilities;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (FACILITIES.equals(name)) {
			startFacilities(atts);
		} else if (FACILITY.equals(name)) {
			startFacility(atts);
		} else if (ACTIVITY.equals(name)) {
			startActivity(atts);
		} else if (CAPACITY.equals(name)) {
			startCapacity(atts);
		} else if (OPENTIME.equals(name)) {
			startOpentime(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (FACILITY.equals(name)) {
			this.facilities.finishFacility(this.currfacility);
			this.currfacility = null;
		} else if (ACTIVITY.equals(name)) {
			this.curractivity = null;
		}
	}

	private void startFacilities(final Attributes atts) {
		this.facilities.setName(atts.getValue("name"));
		if (atts.getValue("aggregation_layer") != null) {
			Logger.getLogger(FacilitiesReaderMatsimV1.class).warn("aggregation_layer is deprecated.");
		}
	}
	
	private void startFacility(final Attributes atts) {
		this.currfacility = this.facilities.createFacility(new IdImpl(atts.getValue("id")), new CoordImpl(atts.getValue("x"),atts.getValue("y")));
		this.currfacility.setDesc(atts.getValue("desc"));
	}
	
	private void startActivity(final Attributes atts) {
		this.curractivity = this.currfacility.createActivity(atts.getValue("type"));
	}
	
	private void startCapacity(final Attributes atts) {
		int cap = Integer.parseInt(atts.getValue("value"));
		this.curractivity.setCapacity(cap);
	}
	
	private void startOpentime(final Attributes atts) {
		DayType day = getDayType(atts.getValue("day"));
		this.curractivity.addOpeningTime(new OpeningTime(day, Time.parseTime(atts.getValue("start_time")), Time.parseTime(atts.getValue("end_time"))));
	}

	
	private DayType getDayType(String dt){
		for (DayType d : DayType.values()) {
			if (d.toString().equalsIgnoreCase(dt))
				return d;
		}
		throw new IllegalArgumentException("Cannot detect daytype for String: " + dt);
	}

	/**
	 * Parses the specified facilities file. This method calls {@link #parse(String)}, but handles all
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

}
