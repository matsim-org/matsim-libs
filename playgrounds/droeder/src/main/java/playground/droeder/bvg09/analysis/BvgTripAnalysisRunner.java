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
package playground.droeder.bvg09.analysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.Feature;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.Trips.AnalysisTripSet;
import playground.droeder.Analysis.Trips.AnalysisTripGenerator;
import playground.droeder.Analysis.Trips.PlanElementFilter;
import playground.droeder.Analysis.Trips.TripEventsHandler;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */
public class BvgTripAnalysisRunner {
	private Geometry zone;
	private Map<Id, ArrayList<PersonEvent>> events;
	private Map<Id, ArrayList<PlanElement>> planElements;
	private AnalysisTripSet tripSet;
	
	
	
	public static void main(String[] args){
		final String PATH = DaPaths.VSP + "BVG09_Auswertung/"; 
		final String IN = PATH + "input/";
		final String PLANS = PATH + "testPopulation1.xml.gz";
		final String EVENTS = PATH + "testEvents.xml.gz";
		final String NETWORK = IN + "network.final.xml.gz";
		
		Set<Feature> features = null;
		try {
			features = new ShapeFileReader().readFileAndInitialize(DaPaths.VSP + "BVG09_Auswertung/BerlinSHP/Berlin.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Geometry g =  (Geometry) features.iterator().next().getAttribute(0);
		
		BvgTripAnalysisRunner ana = new BvgTripAnalysisRunner(g);
		ana.run(PLANS, NETWORK, EVENTS, null);
	}
	
	
	public BvgTripAnalysisRunner (Geometry g){
		this.zone = g;
	}
	
	public void run(String plans, String network, String events, String out){
		this.readPlans(plans, network);
		this.readEvents(events);
		this.tripSet = AnalysisTripGenerator.calculateTripSet(this.events, this.planElements, this.zone);
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
		
		try {
			new EventsReaderXMLv1(manager).parse(new GZIPInputStream(new FileInputStream(eventsFile)));
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

