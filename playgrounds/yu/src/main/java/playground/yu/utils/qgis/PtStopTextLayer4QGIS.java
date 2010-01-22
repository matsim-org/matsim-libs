/* *********************************************************************** *
 * project: org.matsim.*
 * PtStopTextLayer4QGIS.java
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

/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

/**
 * @author yu
 * 
 */
public class PtStopTextLayer4QGIS extends TextLayer4QGIS {

	/**
	 * 
	 */
	public PtStopTextLayer4QGIS() {
	}

	/**
	 * @param textFilename
	 */
	public PtStopTextLayer4QGIS(String textFilename) {
		super(textFilename);
		this.writer.writeln("id\tlinkRef\tisBlocking");
	}

	public void run(Plan plan) {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../berlin-bvg09/pt/nullfall_M44_344/network.xml";
		String scheduleFilename = "../berlin-bvg09/pt/nullfall_M44_344/transitSchedule.xml";
		String txtFilename = "../berlin-bvg09/pt/nullfall_M44_344/QGIS/stops.txt";

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		new MatsimNetworkReader(scenario).readFile(netFilename);

		TransitSchedule schedule = scenario.getTransitSchedule();
		try {
			new TransitScheduleReader(scenario).readFile(scheduleFilename);
			PtStopTextLayer4QGIS pst4q = new PtStopTextLayer4QGIS(txtFilename);
			for (TransitStopFacility stop : schedule.getFacilities().values()) {
				Coord coord = stop.getCoord();
				pst4q.writeln(coord.getX() + "\t" + coord.getY() + "\t"
						+ stop.getId() + "\t" + stop.getLinkId() + "\t"
						+ stop.getIsBlockingLane());
			}
			pst4q.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
