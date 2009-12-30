/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule.api;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.xml.sax.SAXException;

/**
 * Reads {@link TransitSchedule}s from file as long as the files are in one of the
 * supported file formats.
 *
 * @author mrieser
 */
public class TransitScheduleReader {

	private final ScenarioImpl scenario;

	public TransitScheduleReader(final Scenario scenario) {
		this.scenario = (ScenarioImpl) scenario;
	}

	public void readFile(final String filename) throws IOException, SAXException, ParserConfigurationException {
		MatsimFileTypeGuesser guesser = new MatsimFileTypeGuesser(filename);
		String systemId = guesser.getSystemId();
		if (systemId.endsWith("transitSchedule_v1.dtd")) {
			new TransitScheduleReaderV1(this.scenario.getTransitSchedule(), this.scenario.getNetwork()).readFile(filename);
		} else {
			throw new IOException("Unsupported file format: " + systemId);
		}

	}

}
