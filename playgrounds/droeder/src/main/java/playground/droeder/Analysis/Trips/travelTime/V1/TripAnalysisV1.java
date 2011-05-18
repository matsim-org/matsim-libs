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
package playground.droeder.Analysis.Trips.travelTime.V1;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.Analysis.Trips.travelTime.AnalysisTripSetAllMode;
import playground.droeder.Analysis.Trips.travelTime.AnalysisTripSetOneMode;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */
public class TripAnalysisV1 {
	private Geometry zone;
	private Map<Id, ArrayList<PersonEvent>> events;
	private Map<Id, ArrayList<PlanElement>> planElements;
	private AnalysisTripSetAllMode tripSet;
	
	
	public TripAnalysisV1 (Geometry g){
		this.zone = g;
	}
	
	public void run(String plans, String network, String events, String outDir, boolean storeTrips){
		this.readPlans(plans, network);
		this.readEvents(events);
		this.tripSet = AnalysisTripGeneratorV1.calculateTripSet(this.events, this.planElements, this.zone, storeTrips);
		this.write2csv(outDir);
	}
	
	private void write2csv(String out){
		try {
			BufferedWriter writer;
			for(Entry<String, AnalysisTripSetOneMode> e : this.tripSet.getTripSets().entrySet()){
				writer = IOUtils.getBufferedWriter(out + e.getKey() + "_trip_analysis.csv");
				writer.write(e.getValue().toString());
				writer.flush();
				writer.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readPlans(String plans, String network){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(network);
		
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		PlanElementFilterV1 filter = new PlanElementFilterV1();
		((PopulationImpl) sc.getPopulation()).addAlgorithm(filter);
		new MatsimPopulationReader(sc).parse(IOUtils.getInputstream(plans));
		
		this.planElements = filter.getElements();
	}
	
	private void readEvents(String eventsFile){
		TripEventsHandlerV1 handler = new TripEventsHandlerV1(this.planElements.keySet());
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);
		new EventsReaderXMLv1(manager).parse(IOUtils.getInputstream(eventsFile));
		this.events = handler.getEvents();
	}
}

