/* *********************************************************************** *
 * project: org.matsim.*
 * ShoppingLegTripDurationAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

import playground.telaviv.locationchoice.LocationChoicePlanModule;

public class ShoppingLegTripDurationAnalyzer implements  AgentDepartureEventHandler, AgentArrivalEventHandler {

	private static final Logger log = Logger.getLogger(ShoppingLegTripDurationAnalyzer.class);
	
	private static String basePath = "../../matsim/mysimulations/telaviv/";
	
	private static String networkFile = basePath + "input/network.xml";
	private static String populationFile = basePath + "output_without_location_choice/ITERS/it.90/90.plans.xml.gz";
	private String eventsFile = basePath + "output_without_location_choice/ITERS/it.90/90.events.txt.gz";
	
	private String outFileCar = basePath + "output_without_location_choice/ITERS/it.90/90.shoppingLegsCar.txt";

	private String delimiter = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	
	private Map<Id, List<Integer>> shoppingActivities;	// <PersonId, List<Index of Shopping Activity>
	private Map<Id, Integer> legCounter;	// <PersonId, currently performed Leg Index>
	private Map<Id, Double> departures;
	private List<Leg> toLegsPt;
	private List<Leg> toLegsCar;
	private List<Leg> toLegsUndefined;
	private List<Leg> fromLegsPt;
	private List<Leg> fromLegsCar;
	private List<Leg> fromLegsUndefined;
	
	public static void main(String[] args)
	{
		Scenario scenario = new ScenarioImpl();
		
		// load network
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		// load population
		new MatsimPopulationReader(scenario).readFile(populationFile);

		new ShoppingLegTripDurationAnalyzer(scenario);
	}
	
	public ShoppingLegTripDurationAnalyzer(Scenario scenario)
	{
		this.scenario = scenario;
		
		log.info("Identifying shopping activities...");
		LocationChoicePlanModule lcpm = new LocationChoicePlanModule(scenario);
		shoppingActivities = lcpm.getShoppingActivities();
		log.info("done. Found " + shoppingActivities.size());
		
		log.info("reading events...");
		readEvents();
		log.info("done.");
		
		/*
		 * Error checking - did we miss some Trips???
		 */
		for (Entry<Id, List<Integer>> entry : shoppingActivities.entrySet())
		{
			if (entry.getValue().size() > 0)
			{
				Person person = scenario.getPopulation().getPersons().get(entry.getKey());
				log.error("Why are indices left??? " + entry.getKey());
			}
		}
		
		analyzeResults();
		
		log.info("Writing car shopping legs to file...");
		List<Leg> legsCar = new ArrayList<Leg>();
		legsCar.addAll(toLegsCar);
		legsCar.addAll(fromLegsCar);
		writeFile(legsCar, outFileCar);
		log.info("done.");
	}
	
	private void readEvents()
	{	
		reset(0);
		
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(this);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
	}

	private void writeFile(List<Leg> legs, String outFile)
	{
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		
	    try 
	    {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("depaturetime" + delimiter + "arrivaltime" + "\n");
			
			// write Values
			for (Leg leg : legs)
			{
				bw.write(String.valueOf(leg.getDepartureTime()));
				bw.write(delimiter);
				bw.write(String.valueOf(leg.getDepartureTime() + leg.getTravelTime()));
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	    
		
	}
	
	private void analyzeResults()
	{
		double toTravelTimesPt = 0.0;
		double toTravelTimesCar = 0.0;
		double toTravelTimesUndefined = 0.0;
		
		double toDistancesPt = 0.0;
		double toDistancesCar = 0.0;
		double toDistancesUndefined = 0.0;
		
		double fromTravelTimesPt = 0.0;
		double fromTravelTimesCar = 0.0;
		double fromTravelTimesUndefined = 0.0;
		
		double fromDistancesPt = 0.0;
		double fromDistancesCar = 0.0;
		double fromDistancesUndefined = 0.0;
		
//		for (Leg leg : toLegsPt)
//		{
//			toTravelTimesPt = toTravelTimesPt + leg.getTravelTime();
//			toDistancesPt = toDistancesPt + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of to-legsPt = " + toLegsPt.size());
//		log.info("mean to-traveltimesPt = " + toTravelTimesPt / toLegsPt.size());
//		log.info("mean to-distancesPt = " + toDistancesPt / toLegsPt.size());
		log.info("");
		
		for (Leg leg : toLegsCar)
		{
			toTravelTimesCar = toTravelTimesCar + leg.getTravelTime();
			toDistancesCar = toDistancesCar + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
		}
		log.info("number of to-legsCar = " + toLegsCar.size());
		log.info("mean to-traveltimesCar = " + toTravelTimesCar / toLegsCar.size());
		log.info("mean to-distancesCar = " + toDistancesCar / toLegsCar.size());
		log.info("");
		
//		for (Leg leg : toLegsUndefined)
//		{
//			toTravelTimesUndefined = toTravelTimesUndefined + leg.getTravelTime();
//			toDistancesUndefined = toDistancesUndefined + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of to-legsUndefined = " + toLegsUndefined.size());
//		log.info("mean to-traveltimesUndefined = " + toTravelTimesUndefined / toLegsUndefined.size());
//		log.info("mean to-distancesUndefined = " + toDistancesUndefined / toLegsUndefined.size());
		log.info("");
		
//		for (Leg leg : fromLegsPt)
//		{
//			fromTravelTimesPt = fromTravelTimesPt + leg.getTravelTime();
//			fromDistancesPt = fromDistancesPt + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of from-legsPt = " + fromLegsPt.size());
//		log.info("mean from-traveltimesPt = " + fromTravelTimesPt / fromLegsPt.size());
//		log.info("mean from-distancesPt = " + fromDistancesPt / fromLegsPt.size());
		log.info("");
		
		for (Leg leg : fromLegsCar)
		{
			fromTravelTimesCar = fromTravelTimesCar + leg.getTravelTime();
			fromDistancesCar = fromDistancesCar + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
		}
		log.info("number of from-legsCar = " + fromLegsCar.size());
		log.info("mean from-traveltimesCar = " + fromTravelTimesCar / fromLegsCar.size());
		log.info("mean from-distancesCar = " + fromDistancesCar / fromLegsCar.size());
		log.info("");
		
//		for (Leg leg : fromLegsUndefined)
//		{
//			fromTravelTimesUndefined = fromTravelTimesUndefined + leg.getTravelTime();
//			fromDistancesUndefined = fromDistancesUndefined + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of from-legsUndefined = " + fromLegsUndefined.size());
//		log.info("mean from-traveltimesUndefined = " + fromTravelTimesUndefined / fromLegsUndefined.size());
//		log.info("mean from-distancesUndefined = " + fromDistancesUndefined / fromLegsUndefined.size());
		log.info("");
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		
		if (legCounter.containsKey(event.getPersonId()))
		{
			int count = legCounter.get(event.getPersonId());
			
			// increase leg count
			count++;
			legCounter.put(event.getPersonId(), count);
			
			departures.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
		List<Integer> shoppingIndices = null;
		if ((shoppingIndices = shoppingActivities.get(event.getPersonId())) != null)
		{
			int count = legCounter.get(event.getPersonId());
			
			double departureTime = departures.remove(event.getPersonId());
			
			if (shoppingIndices.contains(count + 1))
			{
				Leg toLeg = (Leg) scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements().get(count);
				toLeg.setDepartureTime(departureTime);
				toLeg.setTravelTime(event.getTime() - departureTime);
				
				if (toLeg.getMode().equals(TransportMode.car)) toLegsCar.add(toLeg);
				else if (toLeg.getMode().equals(TransportMode.pt)) toLegsPt.add(toLeg);
				else toLegsUndefined.add(toLeg);
			}
			else if (shoppingIndices.contains(count - 1))
			{
				Leg fromLeg = (Leg) scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements().get(count);
				fromLeg.setDepartureTime(departureTime);
				fromLeg.setTravelTime(event.getTime() - departureTime);

				if (fromLeg.getMode().equals(TransportMode.car)) fromLegsCar.add(fromLeg);
				else if (fromLeg.getMode().equals(TransportMode.pt)) fromLegsPt.add(fromLeg);
				else fromLegsUndefined.add(fromLeg);
				
				shoppingIndices.remove((Object)(count - 1));
			}
			
			// increase leg count
			count++;
			legCounter.put(event.getPersonId(), count);
		}
	}

	@Override
	public void reset(int iteration) {
		legCounter = new HashMap<Id, Integer>();
		for (Id id : shoppingActivities.keySet())
		{
			legCounter.put(id, 0);
		}
	
		departures = new HashMap<Id, Double>();
		
		toLegsPt = new ArrayList<Leg>();
		toLegsCar = new ArrayList<Leg>();
		toLegsUndefined = new ArrayList<Leg>();
		fromLegsPt = new ArrayList<Leg>();
		fromLegsCar = new ArrayList<Leg>();
		fromLegsUndefined = new ArrayList<Leg>();
	}

}
