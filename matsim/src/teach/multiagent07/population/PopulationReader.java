/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationReader.java
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

package teach.multiagent07.population;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.basic.v01.BasicNode;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import teach.multiagent07.net.CALink;
import teach.multiagent07.net.CANode;
import teach.multiagent07.util.DateConversion;

public class PopulationReader extends DefaultHandler {

	private Population population;
	private BasicNetI net;
	private String fileName;
	private String buffer; // for Route parsing
	private Person actPerson;
	private Plan actPlan;
	private Leg actLeg;


	public PopulationReader(Population population, BasicNetI net, String filename) {
		this.population = population;
		this.fileName = filename;
		this.net = net;
	}
	
	public void readPopulation() {
		// Prepare SAX parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//factory.setValidating(true);
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

	@Override
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException {
		//System.out.print("Started..." + qName + "...");
		
		if (qName.equals("person")) {
			actPerson = new Person(attributes.getValue("id"));
			try {
				population.addPerson(actPerson);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else if( qName.equals("plan")) {
			actPlan = new Plan();
			actPerson.addPlan(actPlan);
			
		} else if( qName.equals("act")) {
			CALink link = (CALink) net.getLinks().get(attributes.getValue("link"));
			Activity act = new Activity(link, attributes.getValue("type"));
			
			int sec = DateConversion.secondsFromString(attributes.getValue("end_time"));
			act.setEndTime(sec);
			actPlan.addAct(act);
			
		} else if( qName.equals("leg")) {
			actLeg = new Leg(attributes.getValue("mode")); 
			int sec = DateConversion.secondsFromString(attributes.getValue("dur"));
			actLeg.setDuration(sec); 
			actPlan.addLeg(actLeg);
		} else if( qName.equals("route")) {
			buffer = "";
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//System.out.println("finished..." + qName);
		if (qName.equals("route")) {
			BasicRoute<BasicNode> route = new BasicRoute<BasicNode>();
			String[] parts = buffer.split("[ \t\n]+");
			// node[0] == toNode of last activity, Read from node 1 on!
			for (int i = 1; i < parts.length; i++) {
				if (parts[i].length() > 0 ) {
					CANode node = (CANode) net.getNodes().get(parts[i]);
					route.getRoute().add(node);
				}
			}
			actLeg.setRoute(route);
		} else if (qName.equals("person")) {
			
			// Create a new SimpleVehicle
			//BasicAct act = actPerson.getSelectedPlan().getIteratorAct().next();
			//CALink link = (CALink)act.getLink();
			//double time = act.getEndTime();
			
			//link.addParking(new SimpleVehicle(link,time));
		}
	}
	
	@Override
	public void characters(char[] chars, int start, int len) throws SAXException {
			for (int i = start; i< start+len; i++) buffer += chars[i];
			//System.out.print(".." + buffer + "..");
		}

	public static void main(String[] args) {
		//PopulationReader popread = new PopulationReader();
	}
}
