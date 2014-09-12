/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.altplansreader;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;



/**
 * @author jbischoff
 *
 */

public class AltPopulationReaderMatsimV4 implements PopulationReader{

	private PersonImpl currperson = null;
	private PlanImpl currplan = null;
	private ActivityImpl curract = null;
	private LegImpl currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;

	private ActivityImpl prevAct;
	private Scenario scenario;
	private Population plans;
	private Network network;

	private final static Logger log = Logger.getLogger(AltPopulationReaderMatsimV4.class);

	public AltPopulationReaderMatsimV4(final Scenario scenario) {
		this.scenario = scenario;
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
		
		
		
	}
	
	private XMLInputFactory createXmlInputFactory(){
		XMLInputFactory xmlif = null;
		try {
			xmlif = XMLInputFactory.newInstance();
			xmlif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
			xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
			// set the IS_COALESCING property to true , if application desires to
			// get whole text data as one event.
			xmlif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return xmlif;
	}
	@Override
	public void readFile(String filename) {
	File file = null;
	file = new File(filename);
	if (!file.exists()) {
		log.error("File: " + filename + " does not exist.");
		return;
	}

	XMLInputFactory xmlif = this.createXmlInputFactory();

//	long starttime = System.currentTimeMillis();
	try {
		XMLStreamReader xmlr = xmlif.createXMLStreamReader(filename, new FileInputStream(file));
		int eventType = xmlr.getEventType();
		while (xmlr.hasNext()) {
			eventType = xmlr.next();
//			log.debug("EventType: " + eventType);
//			 printEventType(eventType);

			 if (XMLEvent.START_ELEMENT == eventType){
				 String name = xmlr.getLocalName();
//				 log.debug("start element: " + name);
				 
				 
				 if ("plans".compareTo(name) == 0){
					 String id = xmlr.getAttributeValue(""	, "name");
					 this.plans.setName(id);
					 
				 }
				 
				 
				 else if ("person".compareTo(name ) == 0){
					 String id = xmlr.getAttributeValue(""	, "id");
//					 log.info("person: "+id);
					 this.prevAct=null;
					 this.currperson = new PersonImpl(new IdImpl(id));
					 
					String sex = xmlr.getAttributeValue(""	, "sex");
					if (sex!=null) this.currperson.setSex(sex);
					
					String age = xmlr.getAttributeValue(""	, "age");
					if (age!=null) this.currperson.setAge(Integer.parseInt(age)); 
					String license = xmlr.getAttributeValue(""	, "license");
					if (license!=null) this.currperson.setLicence(license);
					
					 String car = xmlr.getAttributeValue(""	, "car_avail");
						if (car!=null) this.currperson.setCarAvail(car);
				 
						 String employed = xmlr.getAttributeValue(""	, "employed");
						 
						if (employed!=null){
							if (employed.equals("yes")) this.currperson.setEmployed(true);
							else if (employed.equals("no")) this.currperson.setEmployed(false);
						}
							
				 
				 }
				 else if ("travelcard".compareTo(name ) == 0){
					
					 String tc = xmlr.getAttributeValue("", "type");
					 if (tc!=null) this.currperson.addTravelcard(tc);
				
				 }
				 else if ("plan".compareTo(name ) == 0){
					 this.currplan = new PlanImpl();
					 String score = xmlr.getAttributeValue("", "score");
					 if (score!=null) this.currplan.setScore(Double.parseDouble(score));
					
					 String type = xmlr.getAttributeValue("", "type");
					 if (type!=null) this.currplan.setType(type);
					 
					 
				 }
				 else if ("act".compareTo(name ) == 0){
					 this.prevAct=this.curract;
					 this.curract=null;
//					 log.info("encountered activity start el");
					 Coord coord = null;
					 if (xmlr.getAttributeValue(""	, "link") != null) {
							Id<Link> linkId = Id.create(xmlr.getAttributeValue(""	, "link"), Link.class);
							this.curract = this.currplan.createAndAddActivity(xmlr.getAttributeValue(""	, "type"), linkId);
//							 log.info("created link act "+this.curract.getLinkId());

							
							if ((xmlr.getAttributeValue(""	, "x") != null) && (xmlr.getAttributeValue(""	, "y") != null)) {
								coord = new CoordImpl(xmlr.getAttributeValue("", "x"), xmlr.getAttributeValue("", "y"));
								this.curract.setCoord(coord);
							}
						} else if ((xmlr.getAttributeValue(""	, "x") != null) && (xmlr.getAttributeValue(""	, "y") != null)) {
							coord = new CoordImpl(xmlr.getAttributeValue(""	, "x"), xmlr.getAttributeValue(""	, "y"));
							this.curract = this.currplan.createAndAddActivity(
									xmlr.getAttributeValue(""	, "type"), coord);
						} else {
							throw new IllegalArgumentException(
									"In this version of MATSim either the coords or the link must be specified for an Act.");
						}
					 
					 this.curract.setStartTime(Time.parseTime(xmlr.getAttributeValue(""	, "start_time")));
					 this.curract.setMaximumDuration(Time.parseTime(xmlr.getAttributeValue(""	, "dur")));
					 this.curract.setEndTime(Time.parseTime(xmlr.getAttributeValue(""	, "end_time")));
					String fId = xmlr.getAttributeValue(""	, "facility");
						if (fId != null) {
							this.curract.setFacilityId(Id.create(fId, ActivityFacility.class));
						}
			 
				}
				 
				else if ("leg".compareTo(name ) == 0){ 
				String mode = xmlr.getAttributeValue(""	, "mode").toLowerCase(Locale.ROOT);
					if (mode.equals("undef")) {
						mode = "undefined";
					}
					this.currleg = this.currplan.createAndAddLeg(mode.intern());
					this.currleg.setDepartureTime(Time.parseTime(xmlr.getAttributeValue(""	, "dep_time")));
					this.currleg.setTravelTime(Time.parseTime(xmlr.getAttributeValue(""	, "trav_time")));
					this.currleg.setArrivalTime(Time.parseTime(xmlr.getAttributeValue(""	, "arr_time")));
				} 
				 
				else if ("route".compareTo(name ) == 0){ 
//					 log.info("encountered route start");

					
					
					this.currRoute = ((PopulationFactoryImpl) this.plans.getFactory()).createRoute(this.currleg.getMode(), null, null);
					this.currleg.setRoute(this.currRoute);
					if (xmlr.getAttributeValue(""	, "dist") != null) {
						this.currRoute.setDistance(Double.parseDouble(xmlr.getAttributeValue(""	, "dist")));
					}
					if (xmlr.getAttributeValue(""	, "trav_time") != null) {
						this.currRoute.setTravelTime(Time.parseTime(xmlr.getAttributeValue(""	, "trav_time")));
					}
					this.routeDescription = xmlr.getElementText();
					if (this.routeDescription != null) {
						Id startLinkId = null;
						
						try{
						if (this.prevAct.getLinkId() != null) {
							startLinkId = this.prevAct.getLinkId();
						}}catch (NullPointerException e){
//							log.info("npe");
							}
						Id endLinkId = null;
//						 log.info("ecountered curract.getlink = " + this.curract.getLinkId());

						if (this.curract.getLinkId() != null) {
							endLinkId = this.curract.getLinkId();
						}
						
				
						if (this.currRoute instanceof GenericRoute) {
							((GenericRoute) this.currRoute).setRouteDescription(
									startLinkId, this.routeDescription.trim(), endLinkId);
						} else if (this.currRoute instanceof NetworkRoute) {
							((NetworkRoute) this.currRoute).setLinkIds(startLinkId,
									NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, this.routeDescription))),endLinkId);
						} else {
							throw new RuntimeException("unknown route type: "
									+ this.currRoute.getClass().getName());
						}

				
				} 
			 }
			 }	 
			 else if (XMLEvent.END_ELEMENT == eventType){
				 String name = xmlr.getLocalName();
//				 log.debug("end element: " + name);
				 if ("person".compareTo(name) == 0){
					this.plans.addPerson(currperson);
					this.currperson=null;
				 }
				 else if ("plan".compareTo(name) == 0){
					 this.currperson.addPlan(currplan);
					 this.currplan=null;
				
				 }
				 else if ("act".compareTo(name) == 0){
//					 log.info("act end");
//					 this.prevAct=this.curract;
//					 this.curract=null;
				
				 }
				 
				 
			 }
			 
		}

	} catch (XMLStreamException ex) {
		System.out.println(ex.getMessage());
		if (ex.getNestedException() != null)
			ex.getNestedException().printStackTrace();
	} catch (Exception ex) {
		ex.printStackTrace();
	}
//	long endtime = System.currentTimeMillis();
//	log.info(" Parsing Time = " + (endtime - starttime));

}
}
