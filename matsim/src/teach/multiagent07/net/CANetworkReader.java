/* *********************************************************************** *
 * project: org.matsim.*
 * CANetworkReader.java
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

package teach.multiagent07.net;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.geometry.shared.Coord;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CANetworkReader extends DefaultHandler {


	private CANetwork net;
	private String fileName;

	public CANetworkReader(CANetwork net, String filename) {
		this.net = net;
		this.fileName = filename;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals("node")) {

			CANode node = net.newNode(attributes.getValue("id"));
			node.setCoord(new Coord(Double.parseDouble(attributes.getValue("x")),
					Double.parseDouble(attributes.getValue("y"))));
			net.add(node);
			//System.out.println(node);
		} else if (qName.equals("link")) {

			CALink link = net.newLink(attributes.getValue("id"));
			BasicNodeI from = net.getNodes().get(new Id(attributes.getValue("from")));
			BasicNodeI to = net.getNodes().get(new Id(attributes.getValue("to")));
			link.setFromNode(from);
			link.setToNode(to);
			link.setLength(Double.parseDouble(attributes.getValue("length")));
			link.setCapacity(Double.parseDouble(attributes.getValue("capacity")));
			link.setFreespeed(Double.parseDouble(attributes.getValue("freespeed")));
			link.setLanes(Integer.parseInt(attributes.getValue("permlanes")));
			net.add(link);
			//System.out.println(link);
		}
	}

	public void readNetwork() {
		// Prepare SAX parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(fileName, this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
