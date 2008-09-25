/* *********************************************************************** *
 * project: org.matsim.*
 * EventControler.java
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

package playground.christoph.events;


import org.matsim.controler.Controler;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;

import playground.christoph.events.algorithms.KnowledgeReplanner;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.RandomRoute;


/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author mrieser
 */
public class EventControler extends Controler{

	protected ReplanningQueueSimulation sim;
	protected TravelTimeDistanceCostCalculator travelCostCalculator;
	protected KnowledgeTravelTimeCalculator travelTimeCalculator;
	protected PlansCalcRoute replanner;
	
	private static final String FILENAME_EVENTS = "events.txt.gz";

	/**
	 * Initializes a new instance of Controler with the given arguments.
	 *
	 * @param args The arguments to initialize the controler with. <code>args[0]</code> is expected to
	 * 		contain the path to a configuration file, <code>args[1]</code>, if set, is expected to contain
	 * 		the path to a local copy of the DTD file used in the configuration file.
	 */
	public EventControler(String[] args)
	{
		super(args);


		// Replanning klappt so nicht, da der Event erst ausgelöst wird, wenn das
		// Fahrzeug bereits auf den nächsten Link verschoben wurde!
//		events.addHandler(new KnowledgeReplanner(this));
	}
	
	// Eigenen ReplanningRouter anlegen und nicht denjenigen des Controlers überschreiben.
	// Eventuell sollen später mit dem Router auch Iterationen durchgeführt werden.
	// -> jeweils eigene Router verwenden.
	// Könnte man auch in die Mobsim packen - würde der Replanner da mehr Sinn machen?
	protected void initReplanningRouter()
	{
		travelTimeCalculator = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator);
	
		// Dijkstra
		//replanner = new PlansCalcRouteDijkstra(network, travelCostCalculator, travelTimeCalculator);

		//AStarLandmarks
		PreProcessLandmarks landmarks = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		landmarks.run(network);
		replanner = new PlansCalcRouteLandmarks(network, landmarks, travelCostCalculator, travelTimeCalculator);
		
		// BasicReplanners (Random, Tabu, Compass, ...)
		// arbeiten statisch - müssen also nur 1x je Agent ausgeführt werden
		replanner = new PlansCalcRoute(new RandomRoute(), new RandomRoute());
		
	}
	
	public PlanAlgorithm getReplanningRouter()
	{
		return replanner;
	}
	
	// Workaround!
	protected void setup() {
			
		/*
		try 
		{
			// ask currently executing Thread to sleep for 1000ms
			Thread.sleep(5000); 
		}
		catch(InterruptedException e)   
		{      
			System.out.println("Sleep interrupted:"+e);      
		}
		 */
		
//		this.network.setEffectiveCellSize(100);
/*				
		Map<Id, Link> linkMap = this.network.getLinks();
		Iterator linkIterator = linkMap.values().iterator();
		while(linkIterator.hasNext())
		{
			Link link = (Link)linkIterator.next();
			//link.setCapacity(link.getCapacity(0.0)/10);
//			link.setFreespeed(0.50);
			
			//link.setLength(link.getLength()/10);			
		}
	*/	
		super.setup();
	}
	
	protected void runMobSim() 
	{
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);
		
		sim.setControler(this);
		
		// CostCalculator entsprechend setzen!
//		setCostCalculator();
		initReplanningRouter();
		sim.run();
	}
	
/*
	protected void setCostCalculator()
	{
		travelTimeCalculator = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator);
		
		// CostCalculator überschreiben!
		super.travelCostCalculator = travelCostCalculator;
	}
*/
	
	/* ===================================================================
	 * main
	 * =================================================================== */

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final EventControler controler = new EventControler(args);			
			controler.setOverwriteFiles(true);
			controler.run();
//			System.out.println(controler.eventWriter.getClass());
//			System.out.println(controler.events.getClass());
//			System.out.println(controler.getEvents().getClass());
		}
		System.exit(0);
	}

	
}
