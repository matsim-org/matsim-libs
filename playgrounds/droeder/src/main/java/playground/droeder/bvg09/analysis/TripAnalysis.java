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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
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

import com.vividsolutions.jts.geom.Geometry;


/**
 * @author droeder
 *
 */
public class TripAnalysis {
	private Map<String, List<AnaTrip>> tripsByLocation;
	private Geometry zone;
	private BvgAnaTripType typeAnalyzer;
	private Map<Id, ArrayList<PersonEvent>> events;
	private Map<Id, ArrayList<PlanElement>> planElements;
	
	
	
	public static void main(String[] args){
		final String PATH = DaPaths.VSP + "BVG09_Auswertung/"; 
		final String IN = PATH + "input/";
		final String PLANS = PATH + "testPopulation1.xml.gz";
		final String NETWORK = IN + "network.final.xml.gz";
		final String EVENTS = IN + "bvg.run128.25pct.100.events.xml.gz";
		
		Set<Feature> features = null;
		try {
			features = new ShapeFileReader().readFileAndInitialize(DaPaths.VSP + "BVG09_Auswertung/BerlinSHP/Berlin.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Geometry g =  (Geometry) features.iterator().next().getAttribute(0);
		
		TripAnalysis ana = new TripAnalysis(g);
		ana.run(PLANS, NETWORK, EVENTS, null);
	}
	
	
	public TripAnalysis (Geometry g){
		this.tripsByLocation = new HashMap<String, List<AnaTrip>>();
		this.zone = g;
		this.typeAnalyzer = new BvgAnaTripType(g);
	}
	
	public void run(String plans, String network, String events, String out){
		this.readPlans(plans, network);
//		this.readEvents(events);
		this.generateTrips();
		
//		this.test();
	}

//	private void test(){
//		for(Entry<Id, ArrayList<PlanElement>> e: this.planElements.entrySet()){
//			System.out.println(e.getKey().toString());
//			for(PlanElement pe: e.getValue()){
//				if(pe instanceof Leg){
//					System.out.print(((Leg) pe).getMode() + "\t");
//				}else if (pe instanceof Activity){
//					System.out.print(((Activity) pe).getType() + "\t");
//				}
//			}
//			System.out.println();
//			for(PersonEvent pe : this.events.get(e.getKey())){
//				if(pe instanceof ActivityEvent){
//					System.out.print(((ActivityEvent) pe).getActType() + "\t");
//				}else if(pe instanceof AgentEvent){
//					System.out.print(((AgentEvent) pe).getLegMode()+ "\t");
//				}
//			}
//			System.out.println();
//			System.out.println();
//		}
//	}
	
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
	
	
	private void generateTrips(){
		Set<Id> ids = this.planElements.keySet();
		ArrayList<ArrayList<PlanElement>> splittedElements;
		ListIterator<PlanElement> elementsIterator;

		ArrayList<PlanElement> temp;
		PlanElement p;
		int counter;
		
		
		for(Id id : ids){
			elementsIterator = this.planElements.get(id).listIterator();
			splittedElements = new ArrayList<ArrayList<PlanElement>>(); 
			
			
			temp = new ArrayList<PlanElement>();
			while (elementsIterator.hasNext()){
				p = elementsIterator.next();
				
				if(p instanceof Activity){
					if(temp.size() == 0){
						temp.add(p);
					}else if(((Activity) p).getType().equals("pt interaction")){
						temp.add(p);
					}else{
						temp.add(p);
						splittedElements.add(temp);
						temp = new ArrayList<PlanElement>();
						
						if(elementsIterator.nextIndex() == this.planElements.get(id).size()){
							break;
						}else{
							elementsIterator.previous();
						}
					}
				}else if(p instanceof Leg){
					temp.add(p);
				}
			}
			
			
			//TODO Test this
			counter = 0;
			System.out.println(id);
			for(ArrayList<PlanElement> pes : splittedElements){
				AnaTrip trip = new AnaTrip((ArrayList<PersonEvent>) this.events.get(id).subList(counter, pes.size()-1), pes);
				trip.toString();
				this.addTrip(trip);
				counter = pes.size();
			}
			
//			System.out.println(id);
//			for(ArrayList<PlanElement> pes : splittedElements){
//				for(PlanElement pe: pes){
//					if(pe instanceof Leg){
//						System.out.print(((Leg) pe).getMode() + "\t");
//					}else if (pe instanceof Activity){
//						System.out.print(((Activity) pe).getType() + "\t");
//					}
//				}
//				System.out.println();
//				System.out.println();
//			}
		}
	}
	
	private void addTrip(AnaTrip trip){
		String location = this.typeAnalyzer.getTripLocation(trip);
		if(!this.tripsByLocation.containsKey(location)){
			this.tripsByLocation.put(location, new ArrayList<AnaTrip>());
		}
		this.tripsByLocation.get(location).add(trip);
	}
}

