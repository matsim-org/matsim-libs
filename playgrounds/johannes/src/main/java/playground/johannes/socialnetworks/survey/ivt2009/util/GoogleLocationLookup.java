/* *********************************************************************** *
 * project: org.matsim.*
 * GoogleLocationLookup.java
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
package playground.johannes.socialnetworks.survey.ivt2009.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author illenberger
 *
 */
public class GoogleLocationLookup {
	
	private static final Logger logger = Logger.getLogger(GoogleLocationLookup.class);

	private static final String PROTOCOL = "http";
	
	private static final String SERVER = "maps.google.com";
	
	private static final String QUERY = "/maps/geo?output=xml&oe=utf-8&q=";
	
	private static final String CHAR_ENCODING = "utf-8";
	
	private ResponseHandler handler = new ResponseHandler();
	
	public Coord locationToCoord(String location) {
		try {
			URLConnection connection = new URL(PROTOCOL, SERVER, QUERY + URLEncoder.encode(location, CHAR_ENCODING)).openConnection();
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			SAXParser parser;
			
			parser = factory.newSAXParser();

			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(handler);
			
			reader.parse(new InputSource(connection.getInputStream()));
			
			if(handler.responseCode == 200)
				return handler.coord;
			else {
				logger.warn(String.format("Google server returned %1$s.", handler.responseCode));
				return null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int getLastErrorCode() {
		return handler.responseCode;
	}
	
	private class ResponseHandler extends DefaultHandler {
		
		private static final String STATUS_TAG = "status";
		
		private static final String CODE_TAG = "code";
		
		private static final String PLACEMARK_TAG = "placemark";
		
		private static final String COORDINATES_TAG = "coordinates";
		
		int responseCode;
		
		Coord coord;
		
		private StringBuffer buffer;
		
		private boolean isStatus;
		
		private boolean isPlacemark;
		
		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			if(STATUS_TAG.equalsIgnoreCase(name))
				isStatus = true;
			else if(CODE_TAG.equalsIgnoreCase(name) && isStatus)
				buffer = new StringBuffer();
			else if(PLACEMARK_TAG.equalsIgnoreCase(name))
				isPlacemark = true;
			else if(COORDINATES_TAG.equalsIgnoreCase(name) && isPlacemark)
				buffer = new StringBuffer();
		}

		@Override
		public void endElement(String uri, String localName,
				String qName) throws SAXException {
			if(STATUS_TAG.equalsIgnoreCase(qName))
				isStatus = false;
			else if(CODE_TAG.equalsIgnoreCase(qName) && isStatus) {
				responseCode = Integer.parseInt(buffer.toString());
				buffer = null;
			} else if(PLACEMARK_TAG.equalsIgnoreCase(qName))
				isPlacemark = false;
			else if(COORDINATES_TAG.equalsIgnoreCase(qName) && isPlacemark) {
				String[] tokens = buffer.toString().split(",");
				double x = Double.parseDouble(tokens[0]);
				double y = Double.parseDouble(tokens[1]);
				coord = new CoordImpl(x, y);
				buffer = null;
			}
				
		}
		
		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException {
			if(buffer != null)
				buffer.append(ch, start, length);
		}
	}
	
	public static void main(String args[]) throws UnsupportedEncodingException {
		GoogleLocationLookup lookup = new GoogleLocationLookup();
		
		Coord c = lookup.locationToCoord(URLEncoder.encode("wilhelmsh�her str. 9, 12161 berlin", "UTF-8"));
		System.out.println("x="+c.getX()+", y="+c.getY());
	}
}
