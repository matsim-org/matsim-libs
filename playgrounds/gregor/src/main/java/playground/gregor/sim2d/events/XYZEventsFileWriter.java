/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsFileWriter.java
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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class XYZEventsFileWriter  extends MatsimXmlWriter implements XYZEventsHandler {

	private static final Logger log = Logger.getLogger(XYZEventsFileWriter.class);

	public static final String TAB = "\t";

	public static final String WHITESPACE = " ";

	public static final String OPEN_TAG_1 = "<";

	public static final String OPEN_TAG_2 = "</";

	public static final String CLOSE_TAG_1 = ">";

	public static final String CLOSE_TAG_2 = "/>";

	public static final String QUOTE = "\"";

	public static final String EQUALS = "=";

	public static final String DTD_LOCATION = "http://www.matsim.org/files/dtd";

	public static final String W3_URL = "http://www.w3.org/2001/XMLSchema-instance";

	public static final String XSD_LOCATION = "http://www.matsim.org/files/dtd/XYZEvents.xsd";

	public static final String XYZ_EVENTS_TAG = "XYZEvents";

	public static final String  XYZ_EVENT_TAG = "XYZEvent";
	
	public static final String ID_TAG = "id";

	public static final String X_COORD_TAG = "x";

	public static final String Y_COORD_TAG = "y";

	public static final String Z_COORD_TAG = "z";
	
	public static final String AZIMUTH_TAG = "azimuth";

	public static final String TIME_TAG = "time";



	public XYZEventsFileWriter(String file) {
		try {
			openFile(file);
			super.writeXmlHead();

			this.writer.write(OPEN_TAG_1);
			this.writer.write(XYZ_EVENTS_TAG);

			this.writer.write(WHITESPACE);
			this.writer.write("xmlns");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(DTD_LOCATION);
			this.writer.write(QUOTE);

			this.writer.write(WHITESPACE);
			this.writer.write("xmlns:xsi");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(W3_URL);
			this.writer.write(QUOTE);

			this.writer.write(WHITESPACE);
			this.writer.write("xsi:schemaLocation");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(DTD_LOCATION);
			this.writer.write(WHITESPACE);
			this.writer.write(XSD_LOCATION);
			this.writer.write(QUOTE);
			this.writer.write(CLOSE_TAG_1);
			this.writer.write(NL);

			this.writer.write(NL);
		} catch (IOException e) {
			throw new RuntimeException("Error during writing xyz events!", e);
		}
	}

	@Override
	public void handleXYZEvent(XYZEvent e) {
		try {
			this.writer.write(TAB);
			this.writer.write(OPEN_TAG_1);
			this.writer.write(XYZ_EVENT_TAG);
			this.writer.write(WHITESPACE);

			this.writer.write(ID_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(e.getId().toString());
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);
			
			this.writer.write(X_COORD_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(Double.toString(e.getC().x));
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);

			this.writer.write(Y_COORD_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(Double.toString(e.getC().y));
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);

			this.writer.write(Z_COORD_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(Double.toString(e.getC().z));
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);

			this.writer.write(AZIMUTH_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(Double.toString(e.getAzimuth()));
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);
			
			this.writer.write(TIME_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(Double.toString(e.time));
			this.writer.write(QUOTE);
			this.writer.write(WHITESPACE);

			this.writer.write(CLOSE_TAG_2);

			this.writer.write(NL);
		} catch (IOException ex) {
			throw new RuntimeException("Error during writing xyz events!", ex);
		}

	}




	@Override
	public void reset() {
		try {
			this.writer.write(OPEN_TAG_2);
			this.writer.write(XYZ_EVENTS_TAG);
			this.writer.write(CLOSE_TAG_1);
			this.writer.write(NL);
			close();
		} catch (IOException e) {
			throw new RuntimeException("Error during writing xyz events!", e);
		}

	}




}
