/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesReaderConvertOTs.java
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

package playground.anhorni.surprice.preprocess;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;
import org.xml.sax.Attributes;

import playground.anhorni.surprice.Surprice;


public class FacilitiesReaderConvertOTs extends MatsimXmlParser {

	private final static String FACILITIES = "facilities";
	private final static String FACILITY = "facility";
	private final static String ACTIVITY = "activity";
	private final static String CAPACITY = "capacity";
	private final static String OPENTIME = "opentime";

	private final Scenario scenario;
	private final ActivityFacilities facilities;
	private final ActivityFacilitiesFactory factory;
	private ActivityFacility currfacility = null;
	private ActivityOption curractivity = null;
	
	public FacilitiesReaderConvertOTs(final Scenario scenario) {
		this.scenario = scenario;
		this.facilities = scenario.getActivityFacilities();
		this.factory = this.facilities.getFactory();
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
			this.currfacility = null;
		} else if (ACTIVITY.equals(name)) {
			this.curractivity = null;
		}
	}

	private void startFacilities(final Attributes atts) {
		this.facilities.setName(atts.getValue("name"));
		if (atts.getValue("aggregation_layer") != null) {
			Logger.getLogger(FacilitiesReaderConvertOTs.class).warn("aggregation_layer is deprecated.");
		}
	}
	
	private void startFacility(final Attributes atts) {
		this.currfacility = this.factory.createActivityFacility(Id.create(atts.getValue("id"), ActivityFacility.class), 
				this.scenario.createCoord(Double.parseDouble(atts.getValue("x")), Double.parseDouble(atts.getValue("y"))));
		this.facilities.addActivityFacility(this.currfacility);
		((ActivityFacilityImpl) this.currfacility).setDesc(atts.getValue("desc"));
	}
	
	private void startActivity(final Attributes atts) {
		this.curractivity = this.factory.createActivityOption(atts.getValue("type"));
		this.currfacility.addActivityOption(this.curractivity);
	}
	
	private void startCapacity(final Attributes atts) {
		double cap = Double.parseDouble(atts.getValue("value"));
		this.curractivity.setCapacity(cap);
	}
	
	private void startOpentime(final Attributes atts) {
		double dayOffset = Surprice.days.indexOf(atts.getValue("day")) * 24.0 * 3600.0;
		this.curractivity.addOpeningTime(new OpeningTimeImpl(dayOffset + Time.parseTime(atts.getValue("start_time")), 
				dayOffset + Time.parseTime(atts.getValue("end_time"))));
	}


	/**
	 * Parses the specified facilities file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

}
