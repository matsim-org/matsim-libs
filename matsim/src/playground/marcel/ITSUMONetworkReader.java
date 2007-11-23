/* *********************************************************************** *
 * project: org.matsim.*
 * ITSUMONetworkReader.java
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

package playground.marcel;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.network.NetworkLayer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


public class ITSUMONetworkReader {

	private NetworkLayer network;

	public ITSUMONetworkReader(NetworkLayer network) {
		this.network = network;
		network.setCapacityPeriod("01:00:00");
	}

	public void read(String filename) {
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			//factory.setValidating(recognizer.getValidating()); //the code was generated according DTD
			//factory.setNamespaceAware(false); //the code was generated according DTD
			XMLReader parser;
			parser = factory.newSAXParser().getXMLReader();
			parser.setContentHandler(new BrazilParser());
			//parser.setErrorHandler(recognizer.getDefaultErrorHandler());
			//if (recognizer.resolver != null)
			//	parser.setEntityResolver(recognizer.resolver);
			parser.parse(filename);

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private class BrazilParser extends org.xml.sax.helpers.DefaultHandler {

		private String nodeId = null;
		private String xCoord = null;
		private String yCoord = null;

		private String lanesetId = null;
		private String lanesetFrom = null;
		private String lanesetTo = null;
		private int lanesCount = 0;
		private double laneSpeed = 0.0;

		private Stack<StringBuffer> textbuffer = new Stack<StringBuffer>();

		@Override
		public void startElement(String ns, String lname, String qname, Attributes atts) throws SAXException {
			this.textbuffer.push(new StringBuffer());
			if (ns.length() == 0) lname = qname;

			if (lname.equals("simulation")) {

			} else if (lname.equals("network_id")) {

			} else if (lname.equals("network_name")) {

			} else if (lname.equals("node")) {
				// make sure we start with clean data, not that if data is missing
				// in one node the data from a previous node will be used
				this.nodeId = null;
				this.xCoord = null;
				this.yCoord = null;
			} else if (lname.equals("streets")) {

			} else if (lname.equals("street")) {

			} else if (lname.equals("laneset")) {
				this.lanesetId = null;
				this.lanesetFrom = null;
				this.lanesetTo = null;
				this.lanesCount = 0;
				this.laneSpeed = 0.0;
			} else if (lname.equals("laneset_id")) {

			} else if (lname.equals("laneset_position")) {

			} else if (lname.equals("start_node")) {

			} else if (lname.equals("end_node")) {

			}
		}

		@Override
		public void endElement(String ns, String lname, String qname) throws SAXException {
			if (ns.length() == 0) lname = qname;

			String content = new String(this.textbuffer.peek());

			if (lname.equals("simulation")) {

			} else if (lname.equals("network_id")) {

			} else if (lname.equals("network_name")) {

			} else if (lname.equals("node")) {
				ITSUMONetworkReader.this.network.createNode(this.nodeId, this.xCoord, this.yCoord, null);
			} else if (lname.equals("node_id")) {
				this.nodeId = content.trim();
			} else if (lname.equals("x_coord")) {
				this.xCoord = content.trim();
			} else if (lname.equals("y_coord")) {
				this.yCoord = content.trim();
			} else if (lname.equals("streets")) {

			} else if (lname.equals("laneset")) {
				double length = ITSUMONetworkReader.this.network.getNode(this.lanesetFrom).getCoord().calcDistance(ITSUMONetworkReader.this.network.getNode(this.lanesetTo).getCoord());
				double capacity = 3600.0; // MR TODO calculate capacity from speed
				ITSUMONetworkReader.this.network.createLink(this.lanesetId, this.lanesetFrom, this.lanesetTo, Double.toString(length),
						Double.toString(this.laneSpeed / this.lanesCount), Double.toString(capacity), Integer.toString(this.lanesCount),
						(String)null, (String)null);
			} else if (lname.equals("laneset_id")) {
				this.lanesetId = content.trim();
			} else if (lname.equals("start_node")) {
				this.lanesetFrom = content.trim();
			} else if (lname.equals("end_node")) {
				this.lanesetTo = content.trim();
			} else if (lname.equals("lane")) {
				this.lanesCount++;
			} else if (lname.equals("maximum_speed")) {
				this.laneSpeed += Double.parseDouble(content.trim());
			}

			this.textbuffer.pop();
		}

		@Override
		public void characters(char buf[], int offset, int len) {
			StringBuffer str = this.textbuffer.peek();
			str.append(buf, offset, len);
		}

	}

}
