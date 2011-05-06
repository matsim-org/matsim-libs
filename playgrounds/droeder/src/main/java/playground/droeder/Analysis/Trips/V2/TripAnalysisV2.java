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
package playground.droeder.Analysis.Trips.V2;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import playground.droeder.Analysis.Trips.AnalysisTripSetAllMode;
import playground.droeder.Analysis.Trips.AnalysisTripSetOneMode;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */

public class TripAnalysisV2 {
	private static final Logger log = Logger.getLogger(TripAnalysisV2.class);
	private TripEventsHandlerV2 eventsHandler;
	
	public TripAnalysisV2 (){
		 this.eventsHandler = new TripEventsHandlerV2();
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.eventsHandler.addZones(zones);
	}
	
	public void run(String plans, String network, String events, String outDir){
		this.readPlans(plans, network);
		log.info("streaming plans finished!");
		this.readEvents(events);
		log.info("streaming events finished!");
		this.write2csv(outDir);
		log.info("output written to " + outDir);
	}
	
	private void write2csv(String out){
		try {
			BufferedWriter writer;
			for(Entry<String, AnalysisTripSetAllMode> e : this.eventsHandler.getZone2Tripset().entrySet()){
				for(Entry<String, AnalysisTripSetOneMode> o : e.getValue().getTripSets().entrySet()){
					writer = IOUtils.getBufferedWriter(out + e.getKey() + "_" + o.getKey() + "_trip_analysis.csv");
					writer.write(o.getValue().toString());
					writer.flush();
					writer.close();
				}
			}
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
		Plan2TripsFilterV2 planFilter = new Plan2TripsFilterV2(); 
		((PopulationImpl) sc.getPopulation()).addAlgorithm(planFilter);
		
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
		
		
		this.eventsHandler.addTrips(planFilter.getTrips());
	}
	
	private void readEvents(String eventsFile){
		
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this.eventsHandler);
		
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
	}
}