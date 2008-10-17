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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.controler.Controler;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;

import playground.christoph.events.algorithms.ActEndReplanner;
import playground.christoph.knowledge.nodeselection.CreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.DijkstraWrapper;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelCostCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.util.DeadEndRemover;


/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author mrieser
 */
public class EventControler extends Controler{

	protected ReplanningQueueSimulation sim;
//	protected TravelTimeDistanceCostCalculator travelCostCalculator;
//	protected KnowledgeTravelTimeCalculator travelTimeCalculator;
	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;
	
	private static final Logger log = Logger.getLogger(EventControler.class);
	
	
//	private static final String FILENAME_EVENTS = "events.txt.gz";

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

		/*
		 * Implement EndActReplanning
		 * If the current Activity of a Persons ends, the route to the next Activity.
		 * At the moment replanning when a link is left can't be handled via events -
		 * there is no event that could be used for that purpose (NO, a LinkLeaveEvent
		 * can NOT be used). Due to this fact replanning when leaving a links is
		 * implemented in the MyQueueNode Class.
		 */
		events.addHandler(new ActEndReplanner());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's. 
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter()
	{
		replanners = new ArrayList<PlanAlgorithm>();
		
		KnowledgeTravelTimeCalculator travelTimeCalculator = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		TravelTimeDistanceCostCalculator travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator);
	
		// Dijkstra
		//replanners.add(new PlansCalcRouteDijkstra(network, travelCostCalculator, travelTimeCalculator));

		//AStarLandmarks
		PreProcessLandmarks landmarks = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		landmarks.run(network);
		replanners.add(new PlansCalcRouteLandmarks(network, landmarks, travelCostCalculator, travelTimeCalculator));
		
		// BasicReplanners (Random, Tabu, Compass, ...)
		// each replanner can handle an arbitrary number of persons
		replanners.add(new KnowledgePlansCalcRoute(new RandomRoute(), new RandomRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new TabuRoute(), new TabuRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new CompassRoute(), new CompassRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new RandomCompassRoute(), new RandomCompassRoute()));
		
		// Dijkstra for Replanning
		KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator();
		KnowledgeTravelCostCalculator travelCost = new KnowledgeTravelCostCalculator(travelTime);
		Dijkstra dijkstra = new Dijkstra(network, travelCost, travelTimeCalculator);
		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCost, travelTime);
		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(dijkstraWrapper, dijkstraWrapper);
		dijkstraRouter.setQueueNetwork(sim.getQueueNetwork());
		replanners.add(dijkstraRouter);
		
	}
	
	public ArrayList<PlanAlgorithm> getReplanningRouters()
	{
		return replanners;
	}
	/*
	 * Initializes the NodeSeletors that are used to create the Activity Spaces of the
	 * Persons of a Population.
	 */
	protected void initNodeSelectors()
	{
		nodeSelectors = new ArrayList<SelectNodes>();
		
		nodeSelectors.add(new SelectNodesCircular(this.network));
		
		SelectNodesDijkstra selectNodesDijkstra = new SelectNodesDijkstra(this.network);
		selectNodesDijkstra.setCostFactor(1.1);
		nodeSelectors.add(selectNodesDijkstra);
	}
	
	
	@Override
	protected void runMobSim() 
	{
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);
		
		sim.setControler(this);
		
		// CostCalculator entsprechend setzen!
//		setCostCalculator();
		
		log.info("Initialize Replanning Routers");
		initReplanningRouter();
		
		log.info("Set Replanning flags");
		setReplanningFlags();
		
		log.info("Set Replanners for each Person");
		setReplanners();
		
		log.info("Initialize Node Selectors");
		initNodeSelectors();
		
		log.info("Set Node Selectors");
		setNodeSelectors();
		
		log.info("Create known Nodes Maps");
		createKnownNodes();

		/* 
		 * Could be done before or after the creation of the activity rooms -
		 * depending on the intention of the simulation.
		 * 
		 * If done before, the new created Route is the base for the activity rooms.
		 * 
		 * If done afterwards, existing routes are the base for the activity rooms and
		 * the replanners have to act within the borders of the already defined rooms.
		 * The existing plans could for example be the results of a relaxed solution of
		 * a standard MATSim simulation.
		 */
		log.info("do initial Replanning");
		doInitialReplanning();
		
		sim.run();
	}
	

	/* Add three boolean variables to each Person.
	 * They are used to indicate, if the plans of this person should be
	 * replanned each time if an activity ends, each time a link is left,
	 * before the simulation starts or never during an iteration.
	 * 
	 * I don't like this way but, it is the best way I know at the moment...
	 * In my opinion these variables should be part of the PersonAgents within
	 * the QueueSimulation - but they can't be accessed from an EventHandler.
	 */
	protected void setReplanningFlags()
	{
		int counter = 0;
		
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{		
			Person p = PersonIterator.next();
			
			counter++;
			if(counter < 5000)
			{
				Map<String,Object> customAttributes = p.getCustomAttributes();
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(true));
			}
			else
			{
				Map<String,Object> customAttributes = p.getCustomAttributes();
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(false));
			}
		}
	}

	/*
	 * Assigns a replanner to every Person of the population.
	 * Same problem as above: should be part of the PersonAgents, but only
	 * Persons are available in the replanning modules.
	 * 
	 * At the moment: Replanning Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files. 
	 */
	protected void setReplanners()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
		
			Map<String,Object> customAttributes = p.getCustomAttributes();
			customAttributes.put("Replanner", replanners.get(5));
		}
	}
	
	/*
	 * Assigns nodeSelectors to every Person of the population, which are
	 * used to create an activity rooms for every Person. It is possible to
	 * assign more than one Selector to each Person.
	 * If non is selected the Person knows every Node of the network.
	 *
	 * At the moment: Selection Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files. 
	 */
	protected void setNodeSelectors()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
		
			Map<String,Object> customAttributes = p.getCustomAttributes();
			
			ArrayList<SelectNodes> personNodeSelectors = new ArrayList<SelectNodes>();
			personNodeSelectors.add(nodeSelectors.get(1));
			
			customAttributes.put("NodeSelectors", personNodeSelectors);
		}
	}
	
	protected void createKnownNodes()
	{
		// use only one of these two - they should produce the same results... 
		ParallelCreateKnownNodesMap.run(this.population, nodeSelectors, 2);

		// non multi-core calculation
		//CreateKnownNodesMap.collectAllSelectedNodes(this.population);
		
		
/*		// remove this part, if the methods from above work as expected

		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		
		int counter = 1;
		
		while (PersonIterator.hasNext())
		{
			if (counter == 5000) break;
			
			if (counter % 500 == 0) log.info("created Acivityrooms for " + counter + " persons.");
			counter++;
			
			Person p = PersonIterator.next();
		
			if(p.getKnowledge() == null) p.createKnowledge("activityroom");
			
			Plan plan = p.getSelectedPlan();
					
			//ArrayList<Node> nodes = new ArrayList<Node>();
			Map<Id, Node> nodesMap = new TreeMap<Id, Node>();
			
			ArrayList<SelectNodes> personNodeSelectors = (ArrayList<SelectNodes>)p.getCustomAttributes().get("NodeSelectors");
			
			for(int i = 0; i < personNodeSelectors.size(); i++)
			{
				SelectNodes nodeSelector = personNodeSelectors.get(i);
				
				if(nodeSelector instanceof SelectNodesDijkstra)
				{
					ActIterator actIterator = plan.getIteratorAct();
					
					// get all acts of the selected plan
					ArrayList<Act> acts = new ArrayList<Act>();					
					while(actIterator.hasNext()) acts.add((Act)actIterator.next());
					
					for(int j = 1; j < acts.size(); j++)
					{						
						Node startNode = acts.get(j-1).getLink().getToNode();
						Node endNode = acts.get(j).getLink().getFromNode();
						
						((SelectNodesDijkstra)nodeSelector).setStartNode(startNode);
						((SelectNodesDijkstra)nodeSelector).setEndNode(endNode);
						
						//nodeSelector.getNodes(nodes);
						nodeSelector.addNodesToMap(nodesMap);
					}
				}	//if instanceof SelectNodesDijkstra
				
				else if(nodeSelector instanceof SelectNodesCircular)
				{
					// do something here...
				}
				
				else
				{
					log.error("Unkown NodeSelector!");
				}
			}

			// add the selected Nodes to the knowledge of the person
			Map<String,Object> customKnowledgeAttributes = p.getKnowledge().getCustomAttributes();
			customKnowledgeAttributes.put("Nodes", nodesMap);

			// remove Dead Ends from the Person's Activity Room
			DeadEndRemover.removeDeadEnds(p);
			
//			log.info("created Activityroom for person..." + p.getId());
		}
		//ArrayList<Id> includedLinkIds = (ArrayList<Id>)person.getKnowledge().getCustomAttributes().get("IncludedLinkIDs");
*/
	}	// setNodes()

	
	protected void doInitialReplanning()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
	
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
			
			boolean replanning = (Boolean)p.getCustomAttributes().get("initialReplanning");
			
			if (replanning)
			{
				KnowledgePlansCalcRoute replanner = (KnowledgePlansCalcRoute)replanners.get(1);
				replanner.setPerson(p);
				replanner.run(p.getSelectedPlan());
			}
		}
	
	} //doInitialReplanning
	
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
		}
		System.exit(0);
	}

	
}
