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
package playground.droeder.Analysis.Trips;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */
public class TripAnalysis {
	private Geometry zone;
	private Map<Id, ArrayList<PersonEvent>> events;
	private Map<Id, ArrayList<PlanElement>> planElements;
	private AnalysisTripSetAllMode tripSet;
	
	
	public TripAnalysis (Geometry g){
		this.zone = g;
	}
	
	public void run(String plans, String network, String events, String outFile, boolean storeTrips){
		this.readPlans(plans, network);
		this.readEvents(events);
		this.tripSet = AnalysisTripGenerator.calculateTripSet(this.events, this.planElements, this.zone, storeTrips);
		this.write2csv(outFile);
	}
	
	private void write2csv(String out){
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(out);
			writer.write(this.tripSet.toString());
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readPlans(String plans, String network){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		try {
			new NetworkReaderMatsimV1(sc).parse(network);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		PlanElementFilter filter = new PlanElementFilter();
		((PopulationImpl) sc.getPopulation()).addAlgorithm(filter);
		
		InputStream in = null;
		try{
			if(plans.endsWith("xml.gz")){
				in = new GZIPInputStream(new FileInputStream(plans));
			}else{
				in = new FileInputStream(plans);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			new PopulationReaderMatsimV4(sc).parse(in);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.planElements = filter.getElements();
	}
	
	private void readEvents(String eventsFile){
		TripEventsHandler handler = new TripEventsHandler(this.planElements.keySet());
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);
		
		InputStream in = null;
		try{
			if(eventsFile.endsWith("xml.gz")){
				in = new GZIPInputStream(new FileInputStream(eventsFile));
			}else{
				in = new FileInputStream(eventsFile);
			}
			new EventsReaderXMLv1(manager).parse(in);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.events = handler.getEvents();
	}
}

