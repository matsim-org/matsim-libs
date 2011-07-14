/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data.semiAutomaticScheduleMatching;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class TransitLines2csv {
	
	public static void main (String[] args){
		
		final String DIR = DaPaths.OUTPUT + "osm/";
		
		final String OSMSCHED = DIR + "osm_berlin_subway_sched.xml";
		final String HAFASSCHED = DaPaths.OUTPUT + "bvg09/transitSchedule-HAFAS-Coord.xml";
		
		final String OSMOUT = DIR + "osmLines.csv";
		final String HAFASOUT = DIR + "hafasLines.csv";
		
		final String SEPARATOR = "\n";
		
		StringBuffer b = new StringBuffer();
		
		ScenarioImpl osm = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		osm.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(osm).readFile(OSMSCHED);
		
		b.append("osm" + SEPARATOR);
		for(TransitLine l : osm.getTransitSchedule().getTransitLines().values()){
			b.append(l.getId().toString() + SEPARATOR);
		}
		BufferedWriter w = IOUtils.getBufferedWriter(OSMOUT);
		try {
			w.write(b.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ScenarioImpl hafas = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafas.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(hafas).readFile(HAFASSCHED);
		b = new StringBuffer();
		b.append("hafas" + SEPARATOR);
		for(TransitLine l : hafas.getTransitSchedule().getTransitLines().values()){
			b.append(l.getId().toString() + SEPARATOR);
		}
		w = IOUtils.getBufferedWriter(HAFASOUT);
		try {
			w.write(b.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
