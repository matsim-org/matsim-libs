/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsFileReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.events;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;


public class XYZEventsFileReader extends MatsimXmlParser {
	
	
	
	private XYZEvent currentEvent;
	private XYZEventsManager ev;

	public XYZEventsFileReader(XYZEventsManager ev) {
		this.ev = ev;
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(this.currentEvent != null) {
			this.ev.processXYZEvent(currentEvent);
			this.currentEvent = null;
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(XYZEventsFileWriter.XYZ_EVENT_TAG)) {
			String id = atts.getValue(XYZEventsFileWriter.ID_TAG);
			String x = atts.getValue(XYZEventsFileWriter.X_COORD_TAG);
			String y = atts.getValue(XYZEventsFileWriter.Y_COORD_TAG);
			String z = atts.getValue(XYZEventsFileWriter.Z_COORD_TAG);
			String azimuth = atts.getValue(XYZEventsFileWriter.AZIMUTH_TAG);
			String time =  atts.getValue(XYZEventsFileWriter.TIME_TAG);
			
			createEvent(id,x,y,z,azimuth,time);
		}
		
	}

	private void createEvent(String id, String x, String y, String z, String azimuth,
			String time) {
		double xc = Double.parseDouble(x);
		double yc = Double.parseDouble(y);
		double zc = Double.parseDouble(z);
		double az = Double.parseDouble(azimuth);
		double t = Double.parseDouble(time);
		this.currentEvent = new XYZEvent(new IdImpl(id), new Coordinate(xc,yc,zc),az,t);
		
	}

	public void readFile(String file) {
		try {
			super.parse(file);
		} catch (SAXException e) {
			throw new RuntimeException("Error during parsing.", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error during parsing.", e);
		} catch (IOException e) {
			throw new RuntimeException("Error during parsing.", e);
		}
		
	}
}
