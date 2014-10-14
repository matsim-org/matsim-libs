/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.jjoubert.projects.gfipAnalysis;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

public class FilterVlnByGantryPair {
	private final static Logger LOG = Logger
			.getLogger(FilterVlnByGantryPair.class);

	public static void main(String[] args) {
		Header.printHeader(FilterVlnByGantryPair.class.toString(), args);
		
		String inputFile = args[0];
		String gantryFrom = args[1];
		String gantryTo = args[2];
		String outputFile = args[3];
	
		Counter counter = new Counter("   lines # ");
		EventsManager events = new EventsManagerImpl();
		EventHandler handler = new EventWriterXML(outputFile);
		events.addHandler(handler);
		
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
//				Id vln = new IdImpl(sa[0]);
				String dateTime = sa[1];
				String tollClass = sa[2];
				String gantry = sa[3];
				
				Time.setDefaultTimeFormat("DD.MM.YYYY HH:MM:SS");
				double t = Time.parseTime(dateTime);
				
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + inputFile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputFile);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}
}
